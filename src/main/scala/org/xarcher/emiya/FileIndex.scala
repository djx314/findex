package org.xarcher.xPhoto

import java.io.{ File, FileInputStream, IOException }
import java.nio.file.{ Files, Path, Paths }
import java.util.{ Date, Timer, TimerTask }

import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.store.FSDirectory
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.xarcher.cpoi.{ CPoi, PoiOperations }

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }

object FileIndex {

  val txtGen: Path => Future[Either[Throwable, String]] = { path =>
    Future {
      Try {
        IOUtils.toString(path.toUri.toURL, "utf-8")
      }.toEither
    }
  }

  val poiGen: Path => Future[Either[Throwable, String]] = { path =>
    Future {
      val workbook = Try {
        Try {
          new HSSFWorkbook(new FileInputStream(path.toFile))
        }.getOrElse(new XSSFWorkbook(new FileInputStream(path.toFile)))
      }.toEither
      workbook.right.flatMap { wk =>
        object PoiOperations extends PoiOperations
        import PoiOperations._
        Try {
          CPoi.load(wk).sheets.flatMap(_.rows.flatMap(_.cells.map(_.tryValue[String]))).mkString(" ")
        }.toEither
      }
    }
  }

  val docPoiGen: Path => Future[Either[Throwable, String]] = path =>
    Future {
      Try {
        Try {
          import org.apache.poi.hwpf.extractor.WordExtractor
          import java.io.InputStream
          val is: InputStream = new FileInputStream(path.toFile)
          val ex: WordExtractor = new WordExtractor(is)
          val text2003 = ex.getText
          text2003
        }.getOrElse {
          import org.apache.poi.POIXMLDocument
          import org.apache.poi.xwpf.extractor.XWPFWordExtractor
          val opcPackage = POIXMLDocument.openPackage(path.toFile.getCanonicalPath)
          val extractor = new XWPFWordExtractor(opcPackage)
          val text2007 = extractor.getText
          text2007
        }
      }.toEither
    }

  val indexer: Map[String, Path => Future[Either[Throwable, String]]] = Map(
    "txt" -> txtGen,
    "js" -> txtGen,
    "scala" -> txtGen,
    "java" -> txtGen,
    "php" -> txtGen,
    "css" -> txtGen,
    "conf" -> txtGen,
    "bat" -> txtGen,
    "htm" -> txtGen,
    "html" -> txtGen,
    "properties" -> txtGen,
    "xls" -> poiGen,
    "xlsx" -> poiGen,
    "et" -> poiGen,
    "doc" -> docPoiGen,
    "docx" -> docPoiGen,
    "wps" -> docPoiGen)

  import FileTables._
  import FileTables.profile.api._

  def index(file: Path): Future[Int] = {
    val writer = writerGen

    //val fileQueue = mutable.Queue.empty[File]
    //val dirQueue = mutable.Queue.empty[File]

    var isIndexing = true

    def startFetchFiles(rootDir: File): Future[Boolean] = {
      val f = if (!rootDir.isDirectory) {
        Future.successful(true)
      } else {
        db.run {
          DirectoryPrepare.delete
        }.flatMap((_: Int) =>
          db.run {
            FilePrepare.delete
          }).flatMap((_: Int) =>
          db.run {
            DirectoryPrepare
              .returning(DirectoryPrepare.map(_.id))
              .into((dir, id) => dir.copy(id = id)) += DirectoryPrepareRow(
                id = -1,
                dirPath = rootDir.toPath.toRealPath().toString,
                isFinish = false)
          }.flatMap(dir => fetchFiles(List(dir))))
      }
      f.map { _ =>
        isIndexing = false
        true
      }
    }

    def fetchFiles(dirs: List[DirectoryPrepareRow]): Future[Boolean] = {
      //println("1111" + eachDir.getCanonicalFile.toString)
      val subFiles = dirs.flatMap(eachDir => new File(eachDir.dirPath).listFiles().toList.map(s => s -> eachDir.id))
      //val subFiles = eachDir.listFiles().toList
      val subDirs = subFiles.filter(_._1.isDirectory)
      val simpleFiles = subFiles.filterNot(_._1.isDirectory)

      val addSubDirsAction = DirectoryPrepare ++= subDirs.map { dir =>
        DirectoryPrepareRow(
          id = -1,
          dirPath = dir._1.toPath.toRealPath().toString,
          isFinish = false)
      }

      val updateDirStateAction = DirectoryPrepare.filter(_.id inSet dirs.map(_.id)).map(_.isFinish).update(true)

      val addSubFilesAction = FilePrepare ++= simpleFiles.map { file =>
        FilePrepareRow(
          id = -1,
          parentDirId = file._2,
          filePath = file._1.toPath.toRealPath().toString,
          isFinish = false)
      }

      val addFileNotesAction = db.run(DBIO.seq(addSubDirsAction, updateDirStateAction, addSubFilesAction).transactionally).map(_ => 2)

      addFileNotesAction.flatMap((_: Int) => db.run(DirectoryPrepare.filter(_.isFinish === false).take(100).result)).flatMap {
        case newDirs if newDirs.isEmpty =>
          Future.successful(true)
        case newDirs =>
          fetchFiles(newDirs.toList)
      }
    }

    def tranFiles(sum: Int): Future[Int] = {
      //val dirOptF = db.run(DirectoryPrepare.filter(_.isFinish === false).result.headOption)
      val fileListF = db.run(FilePrepare.filter(_.isFinish === false).take(100).result)

      (for {
        fileList <- fileListF
      } yield {
        //println(fileList)
        if (fileList.isEmpty && (!isIndexing)) {
          //println("11111111111111111111111111111111111")
          Future.successful(sum)
        } else if (!fileList.isEmpty) {
          //println("22222222222222222222222222222222222222222")
          val listF = Future.sequence(fileList.map { f =>
            val file = new File(f.filePath)
            indexFile(file.toPath, f.id)
          })
          listF.flatMap { list =>
            val ids = list.collect {
              case Right(info) =>
                val document = new Document()
                document.add(new TextField("fileName", info.fileName, Field.Store.YES))
                document.add(new TextField("content", info.content, Field.Store.YES))
                document.add(new TextField("filePath", info.filePath, Field.Store.YES))
                writer.addDocument(document)
                info.dbId -> 1
              case Left(id) =>
                id -> 0
            }: Seq[(Int, Int)]
            db.run(
              FilePrepare.filter(_.id inSetBind ids.map(_._1)).map(_.isFinish).update(true).transactionally)
              .map(_ => sum + ids.map(_._2).sum).flatMap(newSum => tranFiles(newSum))
          }: Future[Int]
        } else {
          //println("33333333333333333333333333333333333")
          val promise = Promise[Future[Int]]
          val timer = new Timer()
          val task = new TimerTask {
            override def run(): Unit = {
              promise.success(tranFiles(sum))
            }
          }
          timer.schedule(task, 500)
          promise.future.flatten
        }
      }).flatten
    }

    def showInfo: Future[Int] = {
      val promise = Promise[Future[Int]]

      lazy val fetchSumAction1 = db.run(FilePrepare.filter(_.isFinish === false).size.result)
        .map { size =>
          println(s"还有${size}个文件正在索引列表中")
          size
        }
      lazy val fetchSumAction2 = db.run(DirectoryPrepare.filter(_.isFinish === false).size.result)
        .map { size =>
          println(s"还有${size}个文件夹正在正在查找子文件操作队列中")
          size
        }

      val timer = new Timer()
      val task = new TimerTask {
        override def run(): Unit = {
          promise.success(fetchSumAction1.flatMap((_: Int) => fetchSumAction2: Future[Int]).flatMap((_: Int) => showInfo))
        }
      }
      timer.schedule(task, 3000)
      promise.future.flatten
    }

    if (Files.isDirectory(file)) {
      startFetchFiles(file.toFile).recover {
        case e => e.printStackTrace
      }
      showInfo
      tranFiles(0)
        .map { count =>
          println(s"索引:${file.toRealPath()}完成，一共索引了:${count}个文件")
          1
        }.recover {
          case e =>
            e.printStackTrace
            2
        }.andThen {
          case _ =>
            if (null != writer) {
              Try {
                writer.close()
              }.fold(
                e => e.printStackTrace(),
                _ => ())
            }
        }
    } else {
      Future.successful(3)
    }
  }

  def writerGen: IndexWriter = {
    val f = {
      //1、创建Derictory
      //        Directory directory = new RAMDirectory();//这个方法是建立在内存中的索引
      val directory = FSDirectory.open(Paths.get(path))
      //这个方法是建立在磁盘上面的索引
      //        2、创建IndexWriter，用完后要关闭
      val analyzer = new CJKAnalyzer()
      val indexWriterConfig = new IndexWriterConfig(analyzer)
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      new IndexWriter(directory, indexWriterConfig)
    }
    f
  }

  val path = "./lucenceTemp"

  def indexFile(file: Path, id: Int): Future[Either[Int, IndexInfo]] = {
    val fileName = file.getFileName.toString
    indexer.find { case (extName, _) => fileName.endsWith(s".${extName}") }.map(_._2).map(_.apply(file).map { strEither =>
      strEither.right.map { str =>
        IndexInfo(dbId = id, filePath = file.toRealPath().toString, fileName = file.getFileName().toString, content = str)
      }.left.map(_ => id)
    }).getOrElse(Future.successful(Left(id)))
    /*(for {
      strOpt <- strOptF
    } yield (for {
      str <- strOpt
    } yield {*/
    /*val document = new Document()
      document.add(new TextField("fileName", file.toRealPath().toString, Field.Store.YES))
      document.add(new TextField("content", str, Field.Store.YES))
      document.add(new TextField("filePath", file.toRealPath().toString, Field.Store.YES))
      writer.addDocument(document)*/
    /*}).getOrElse {
      th
    }).recover {
      case e: IOException =>
        e.printStackTrace()
        throw e
    }*/
  }

}

case class IndexInfo(dbId: Int, filePath: String, fileName: String, content: String)