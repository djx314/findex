package org.xarcher.xPhoto

import java.io.File
import java.net.URI
import java.nio.file.{ Files, Path, Paths }
import java.util.{ Date, Timer, TimerTask }

import akka.actor.ActorRef
import com.softwaremill.tagging.@@
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{ IndexOptions, IndexWriter, IndexWriterConfig }
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import org.xarcher.emiya.service.FileIgnoreService
import org.xarcher.emiya.utils._

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Try }

class FileIndex(
  fileDB: FileDB,
  futureLimitedGen: FutureLimitedGen,
  futureTimeLimitedGen: FutureTimeLimitedGen,
  fileExtraction: FileExtraction,
  fileIgnoreService: FileIgnoreService,
  fileUpdate: FileUpdate,
  shutdownHook: ShutdownHook,
  indexExecutionContext: IndexExecutionContext) {

  val logger = LoggerFactory.getLogger(getClass)

  val indexLimited = futureLimitedGen.create(3 * 1024 * 1024, "fileIndexPool")
  val timeLimited = futureTimeLimitedGen.create(6, "fileSearchPool", 1000)
  val indexEc = indexExecutionContext.indexEc

  import FileTables._
  import FileTables.profile.api._

  /*fileDB.db.run(schema.create).andThen {
    case Failure(e) =>
      e.printStackTrace
  }(indexEc)*/

  def fetchFiles(content: IndexContentRow): Future[Boolean] = {
    implicit val ec = indexEc
    timeLimited.limit(() => fetchFilesGen(content), "索引文件").flatMap { s =>
      //println("索引文件")
      if (s) {
        Future.successful(s)
      } else {
        fetchFiles(content)
      }
    }
  }

  def fetchFilesGen(content: IndexContentRow): Future[Boolean] = {
    implicit val ec = indexEc

    //还没有处理如果文件夹变成了文件或者文件变成了文件夹的情况
    val filesF = fileDB.db.run(IndexPath
      .filter(s => /*(s.isFinish === false) &&*/ (s.contentId === content.id) && (s.isDirectory === true) && (s.isFetched === false))
      .take(4).result).map(_.toList)

    filesF.flatMap { rows =>
      Future.sequence(
        rows.map(row =>
          fileUpdate.updateIndexRow(row, content))).map((_: Seq[Int]) => rows.isEmpty)
    }

    /*filesF.flatMap {
      case dirs if dirs.isEmpty =>
        Future.successful(true)
      case dirs =>
        Future {
          val subFiles = dirs.flatMap(eachDir => new File(URI.create(eachDir.uri)).listFiles().toList.map(s => s.toPath -> eachDir.id))
          val subDirs = subFiles.filter(s => Files.isDirectory(s._1) && !fileIgnoreService.ignoreDir(s._1))
          val simpleFiles = subFiles.filterNot(s => Files.isDirectory(s._1))
          val filesToIndex = simpleFiles.filter {
            s =>
              (Files.size(s._1) < (1024 * 1024 * 2)) && !fileIgnoreService.ignoreFile(s._1)
          }

          val addSubDirsAction = IndexPath ++= subDirs.map {
            case (dir, pathId) =>
              IndexPathRow(
                id = -1,
                uri = dir.toUri.toASCIIString,
                parentDirId = pathId,
                isDirectory = Files.isDirectory(dir),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(dir).toMillis),
                isFinish = false,
                contentId = content.id)
          }

          val updateDirStateAction = IndexPath
            .filter(s => (s.id inSet dirs.map(_.id)) && (s.contentId === content.id))
            .map(_.isFinish).update(true)

          val addSubFilesAction = IndexPath ++= filesToIndex.map {
            case (file, pathId) =>
              /*FilePrepareRow(
              id = -1,
              parentDirId = file._2,
              filePath = file._1.toRealPath().toString,
              isFinish = false)*/
              IndexPathRow(
                id = -1,
                uri = file.toUri.toASCIIString,
                parentDirId = pathId,
                isDirectory = Files.isDirectory(file),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(file).toMillis),
                isFinish = false,
                contentId = content.id)
          }

          val addFileNotesAction = fileDB.writeDB.run((addSubDirsAction >> updateDirStateAction >> addSubFilesAction).transactionally)
          addFileNotesAction
        }.flatten.map((_: Option[Int]) => false)
    }*/
  }

  def index(content: IndexContentRow)(implicit ec: ExecutionContext): Future[Int] = {
    val rootPath = Paths.get(URI.create(content.rootUri))

    val lucencePath = Paths.get(path).resolve(content.id.toString)
    val writer = writerGen(lucencePath)
    shutdownHook.addHook(() => Future.successful(Try { writer.close() }))

    def startFetchFiles(rootDir: File): Future[Boolean] = {
      val f = if (!rootDir.isDirectory) {
        Future.successful(true)
      } else {
        fileDB.writeDB.run {
          //schema.create >>
          IndexPath.delete
        }.flatMap((_: Int) =>
          fileDB.writeDB.run {
            IndexPath
              .returning(IndexPath.map(_.id))
              .into((dir, id) => dir.copy(id = id)) += IndexPathRow(
                id = -1,
                uri = rootDir.toPath.toUri.toASCIIString,
                parentDirId = -1,
                isDirectory = Files.isDirectory(rootDir.toPath),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(rootDir.toPath).toMillis),
                isFetched = false,
                isFinish = false,
                contentId = content.id)
            /*id = -1,
                dirPath = rootDir.toPath.toRealPath().toString,
                isFinish = false)*/
          }.flatMap(dir => fetchFiles(content)))
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
      val fileListF = fileDB.db.run(
        IndexPath.filter(s => (s.isFinish === false) && (s.isDirectory === false) && (s.contentId === content.id)).take(2).result)
      //(s"是否已查找文件完毕：${!isIndexing}")
      (for {
        fileList <- fileListF
      } yield {
        if (fileList.isEmpty && (!isIndexing)) {
          Future.successful(sum)
        } else if (!fileList.isEmpty) {
          val listF = Future.sequence(fileList.map { f =>
            val file = new File(URI.create(f.uri))
            indexLimited.limit(() => {
              logger.debug(s"${new Date().toString}，正在索引：${f.uri}")
              indexFile(file.toPath, f.id).flatMap {
                case Right(info) =>
                  Future {
                    val aa = new FieldType()
                    aa.setIndexOptions(IndexOptions.DOCS)
                    aa.setTokenized(false)
                    aa.setStored(false)
                    aa.omitNorms()
                    aa.freeze()

                    val document = new Document()
                    document.add(new TextField("fileName", info.fileName, Field.Store.YES))
                    document.add(new TextField("fileContent", info.content, Field.Store.YES))
                    document.add(new TextField("filePath", info.filePath, Field.Store.YES))
                    //document.add(new StringField("law_fileName", info.fileName, Field.Store.YES))
                    //document.add(new StringField("law_filePath", info.filePath, Field.Store.YES))
                    document.add(new Field("law_fileContent", info.content, aa))

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
            }, file.length, s"索引文件（${f.uri}）")
          })
          listF.flatMap { ids =>
            fileDB.writeDB.run(
              IndexPath.filter(_.id inSetBind ids.map(_._1)).map(_.isFinish).update(true).transactionally)
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
          lazy val indexingSizeF = fileDB.db.run(
            IndexPath.filter(s => (s.isFetched === false) && (s.contentId === content.id)).size.result)
          lazy val fetchingSizeF = fileDB.db.run(
            IndexPath.filter(s => (s.isFinish === false) && (s.contentId === content.id)).size.result)
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

    val indexAction = if (Files.isDirectory(rootPath)) {
      val fetchFilesF = startFetchFiles(rootPath.toFile).recover {
        case e => e.printStackTrace
      }
      val indexFilesF = tranFiles(0, () => fetchFilesF.isCompleted)
      indexFilesF.map { count =>
        println(s"索引:${rootPath.toRealPath()}完成，一共索引了:${count}个文件")
        1
      }.recover {
        case e =>
          e.printStackTrace
          2
      }
      showInfo(() => indexFilesF.isCompleted)
      indexFilesF
    } else {
      Future.successful(3)
    }
    indexAction.andThen {
      case _ =>
        if (null != writer) {
          Try {
            writer.close()
          }.fold(
            e => e.printStackTrace(),
            _ => ())
        }
    }
  }

  def writerGen(file: Path): IndexWriter = {
    val f = {
      Files.createDirectories(file)
      //1、创建Derictory
      //Directory directory = new RAMDirectory();//这个方法是建立在内存中的索引
      val directory = FSDirectory.open(file)
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