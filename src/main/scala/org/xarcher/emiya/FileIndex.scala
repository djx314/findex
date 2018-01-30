package org.xarcher.xPhoto

import java.io.{ File, FileInputStream, IOException }
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.Executors
import java.util.{ Date, Timer, TimerTask }

import akka.actor.ActorRef
import com.softwaremill.tagging.@@
import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.store.FSDirectory
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jsoup.Jsoup
import org.slf4j.{ Logger, LoggerFactory }
import org.xarcher.cpoi.{ CPoi, PoiOperations }
import org.xarcher.emiya.service.FileIgnoreService
import org.xarcher.emiya.utils._

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

class FileIndex(
  FileTables: FileTables,
  futureLimitedGen: FutureLimitedGen,
  futureTimeLimitedGen: FutureTimeLimitedGen,
  fileExtraction: FileExtraction,
  fileIgnoreService: FileIgnoreService,
  shutdownHook: ShutdownHook,
  indexExecutionContext: IndexExecutionContext) {

  val logger = LoggerFactory.getLogger(getClass)

  val indexLimited = futureLimitedGen.create(3 * 1024 * 1024, "fileIndexPool")
  val timeLimited = futureTimeLimitedGen.create(6, "fileSearchPool", 1000)
  val indexEc = indexExecutionContext.indexEc

  import FileTables._
  import FileTables.profile.api._

  def fetchFiles: Future[Boolean] = {
    implicit val ec = indexEc
    timeLimited.limit(() => fetchFilesGen, "索引文件").flatMap { s =>
      //println("索引文件")
      if (s) {
        Future.successful(s)
      } else {
        fetchFiles
      }
    }
  }

  def fetchFilesGen: Future[Boolean] = {
    implicit val ec = indexEc

    val filesF = db.run(DirectoryPrepare.filter(_.isFinish === false).take(4).result).map(_.toList)

    filesF.flatMap {
      case dirs if dirs.isEmpty =>
        Future.successful(true)
      case dirs =>
        Future {
          val subFiles = dirs.flatMap(eachDir => new File(eachDir.dirPath).listFiles().toList.map(s => s.toPath -> eachDir.id))
          val subDirs = subFiles.filter(s => Files.isDirectory(s._1) && !fileIgnoreService.ignoreDir(s._1))
          val simpleFiles = subFiles.filterNot(s => Files.isDirectory(s._1))
          val filesToIndex = simpleFiles.filter {
            s =>
              (Files.size(s._1) < (1024 * 1024 * 2)) && !fileIgnoreService.ignoreFile(s._1)
          }

          val addSubDirsAction = DirectoryPrepare ++= subDirs.map { dir =>
            DirectoryPrepareRow(
              id = -1,
              dirPath = dir._1.toRealPath().toString,
              isFinish = false)
          }

          val updateDirStateAction = DirectoryPrepare.filter(_.id inSet dirs.map(_.id)).map(_.isFinish).update(true)

          val addSubFilesAction = FilePrepare ++= filesToIndex.map { file =>
            FilePrepareRow(
              id = -1,
              parentDirId = file._2,
              filePath = file._1.toRealPath().toString,
              isFinish = false)
          }

          val addFileNotesAction = writeDB.run((addSubDirsAction >> updateDirStateAction >> addSubFilesAction).transactionally)
          addFileNotesAction
        }.flatten.map((_: Option[Int]) => false)
    }
  }

  def index(file: Path)(implicit ec: ExecutionContext): Future[Int] = {
    val writer = writerGen
    shutdownHook.addHook(() => Future.successful(Try { writer.close() }))

    def startFetchFiles(rootDir: File): Future[Boolean] = {
      val f = if (!rootDir.isDirectory) {
        Future.successful(true)
      } else {
        writeDB.run {
          //schema.create >>
          DirectoryPrepare.delete
        }.flatMap((_: Int) =>
          writeDB.run {
            FilePrepare.delete
          }).flatMap((_: Int) =>
          writeDB.run {
            DirectoryPrepare
              .returning(DirectoryPrepare.map(_.id))
              .into((dir, id) => dir.copy(id = id)) += DirectoryPrepareRow(
                id = -1,
                dirPath = rootDir.toPath.toRealPath().toString,
                isFinish = false)
          }.flatMap(dir => fetchFiles))
      }
      f
      /*.andThen {
        case _ =>
          println("查找文件完毕" * 100)
      }*/
      //Future.successful(true)
    }

    def tranFiles(sum: Int, isFetchFileFinished: () => Boolean): Future[Int] = {
      val isIndexing = !isFetchFileFinished()
      val fileListF = db.run(FilePrepare.filter(_.isFinish === false).take(2).result)
      //(s"是否已查找文件完毕：${!isIndexing}")
      (for {
        fileList <- fileListF
      } yield {
        if (fileList.isEmpty && (!isIndexing)) {
          Future.successful(sum)
        } else if (!fileList.isEmpty) {
          val listF = Future.sequence(fileList.map { f =>
            val file = new File(f.filePath)
            indexLimited.limit(() => {
              logger.debug(s"${new Date().toString}，正在索引：${f.filePath}")
              indexFile(file.toPath, f.id).flatMap {
                case Right(info) =>
                  Future {
                    val document = new Document()
                    document.add(new TextField("fileName", info.fileName, Field.Store.YES))
                    document.add(new TextField("content", info.content, Field.Store.YES))
                    document.add(new TextField("filePath", info.filePath, Field.Store.YES))
                    writer.addDocument(document)
                    logger.debug(s"${new Date().toString}，已完成文件：${info.filePath}的索引工作")
                    logger.trace(s"${new Date().toString}，已完成文件：${info.filePath}的索引工作\n索引内容：${info.content}")
                    info.dbId -> 1
                  }(indexEc).andThen {
                    case Failure(e) =>
                      e.printStackTrace
                  }
                case Left(id) =>
                  //println(s"${new Date().toString}，索引：${f.filePath}失败")
                  Future.successful(id -> 0)
              }(indexEc).andThen {
                case Failure(e) =>
                  e.printStackTrace
              }: Future[(Int, Int)]
            }, file.length, s"索引文件（${f.filePath}）")
          })
          listF.flatMap { ids =>
            writeDB.run(
              FilePrepare.filter(_.id inSetBind ids.map(_._1)).map(_.isFinish).update(true).transactionally)
              .map(_ => sum + ids.map(_._2).sum).flatMap(newSum => tranFiles(newSum, isFetchFileFinished))
          }: Future[Int]
        } else {
          val promise = Promise[Future[Int]]
          val timer = new Timer()
          shutdownHook.addHook(() => Future.successful(Try { timer.cancel() }))
          val task = new TimerTask {
            override def run(): Unit = {
              promise.success(tranFiles(sum, isFetchFileFinished))
            }
          }
          timer.schedule(task, 500)
          promise.future.flatten
        }
      }).flatten.andThen {
        case Failure(e) =>
          e.printStackTrace
      }
    }

    def showInfo(isIndexFinished: () => Boolean): Future[Int] = {
      val timer = new Timer()
      shutdownHook.addHook(() => Future.successful(Try { timer.cancel() }))
      val task = new TimerTask {
        override def run(): Unit = {
          lazy val indexingSizeF = db.run(FilePrepare.filter(_.isFinish === false).size.result)
          lazy val fetchingSizeF = db.run(DirectoryPrepare.filter(_.isFinish === false).size.result)
          indexingSizeF.zip(fetchingSizeF).map {
            case (indexingSize, fetchingSize) =>
              val nowisIndexFinished = isIndexFinished()
              if (!nowisIndexFinished) {
                logger.info(
                  s"""还有${indexingSize}个文件正在索引列表中
                     |还有${fetchingSize}个文件夹正在正在查找子文件操作队列中
                     |是否已完成索引：${isIndexFinished()}
                   """.stripMargin)
              }
              2
          }
        }
      }
      timer.schedule(task, 8000, 8000)
      Future.successful(1)
    }

    if (Files.isDirectory(file)) {
      val fetchFilesF = startFetchFiles(file.toFile).recover {
        case e => e.printStackTrace
      }
      val indexFilesF = tranFiles(0, () => fetchFilesF.isCompleted)
      indexFilesF.map { count =>
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

      showInfo(() => indexFilesF.isCompleted)
    } else {
      Future.successful(3)
    }
  }

  def writerGen: IndexWriter = {
    val f = {
      //1、创建Derictory
      //Directory directory = new RAMDirectory();//这个方法是建立在内存中的索引
      val directory = FSDirectory.open(Paths.get(path))
      //这个方法是建立在磁盘上面的索引
      //2、创建IndexWriter，用完后要关闭
      val analyzer = new CJKAnalyzer()
      val indexWriterConfig = new IndexWriterConfig(analyzer)
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      new IndexWriter(directory, indexWriterConfig)
    }
    f
  }

  val path = "./ext_persistence_不索引/lucenceTemp"

  def indexFile(file: Path, id: Int): Future[Either[Int, IndexInfo]] = {
    Future {
      val fileName = file.getFileName.toString
      if (Files.size(file) < (2 * 1024 * 1024)) {
        fileExtraction.indexer.find { case (extName, _) => fileName.endsWith(s".${extName}") }.map(_._2).map(_.apply(file).map { strEither =>
          /*strEither.right.map { str =>
            IndexInfo(dbId = id, filePath = file.toRealPath().toString, fileName = file.getFileName().toString, content = str)
          }.left.map(_ => id)*/
          strEither match {
            case Right(str) =>
              Right(IndexInfo(dbId = id, filePath = file.toRealPath().toString, fileName = file.getFileName().toString, content = str))
            case Left(e) =>
              logger.error(s"索引文件发生错误，路径：${file.toRealPath().toString}")
              Left(id)
          }
        }(indexEc).recover {
          case e: Exception =>
            e.printStackTrace
            Left(id)
        }(indexEc)).getOrElse {
          Future.successful(Left(id))
        }
      } else {
        Future.successful(Left(id))
      }
    }(indexEc).flatten.andThen {
      case Failure(e) =>
        e.printStackTrace
    }(indexEc)
  }

}

case class IndexInfo(dbId: Int, filePath: String, fileName: String, content: String)