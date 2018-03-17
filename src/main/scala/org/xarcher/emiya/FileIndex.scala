package org.xarcher.xPhoto

import java.io.File
import java.net.URI
import java.nio.file.{ Files, Path, Paths }
import java.util.{ Date, Timer, TimerTask }

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.update.UpdateResponse
import com.sksamuel.elastic4s.http.{ RequestFailure, RequestSuccess }
import io.circe.{ Decoder, Encoder }

import scala.util.Success

//import org.apache.solr.common.SolrInputDocument
import org.slf4j.LoggerFactory
import org.xarcher.emiya.service.FileIgnoreService
import org.xarcher.emiya.utils._

import io.circe.syntax._
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Try }
import scala.concurrent.duration._

class FileIndex(
  fileDB: FileDB,
  futureLimitedGen: () => FutureLimitedGen,
  futureTimeLimitedGen: () => FutureTimeLimitedGen,
  fileExtraction: FileExtraction,
  fileIgnoreService: FileIgnoreService,
  fileUpdate: FileUpdate,
  embeddedServer: EmbeddedServer,
  shutdownHook: ShutdownHook)(implicit executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(getClass)

  val fileSizeIndexLimit = futureLimitedGen().create(60 * 1024 * 1024, "fileSizeIndexLimit")
  val fileTimeIndexLimit = futureTimeLimitedGen().create(60, "fileTimeIndexLimit", 1926)

  val timeLimited = futureTimeLimitedGen().create(8, "timeLimited", 1000)

  @volatile var needToShutdonw: Boolean = false
  shutdownHook.addHook(new Thread() {
    override def run(): Unit = {
      needToShutdonw = true
    }
  })

  import FileTables._
  import FileTables.profile.api._

  /*fileDB.db.run(schema.create).andThen {
    case Failure(e) =>
      e.printStackTrace
  }(indexEc)*/

  def fetchFiles(content: IndexContentRow): Future[Boolean] = {
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

    //还没有处理如果文件夹变成了文件或者文件变成了文件夹的情况
    val filesF = fileDB.db.run(IndexPath
      .filter(s => /*(s.isFinish === false) &&*/ (s.contentId === content.id) && (s.isDirectory === true) && (s.isFetched === false))
      .take(16).result).map(_.toList)

    filesF.flatMap { rows =>
      Future.sequence(
        rows.map(row =>
          fileUpdate.updateIndexRow(row, content))).map((_: Seq[Int]) => if (needToShutdonw) true else rows.isEmpty)
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

  def index(content: IndexContentRow): Future[Int] = {
    //val rootPath = Paths.get(URI.create(content.rootUri))

    def startFetchFiles(contentModel: IndexContentRow): Future[Boolean] = {
      val rootFile = Paths.get(URI.create(contentModel.rootUri))

      val f = if (!Files.isDirectory(rootFile)) {
        Future.successful(Option.empty)
      } else {
        fileDB.writeDB.run {
          IndexPath.filter(_.id === content.id).result.headOption.map {
            case Some(rootIndexModel) =>
              val lastModified = Files.getLastModifiedTime(rootFile).toMillis
              if (lastModified == rootIndexModel.lastModified.getTime) {
                Option.empty
              } else {
                Option(
                  rootIndexModel.copy(
                    uri = rootFile.toUri.toASCIIString,
                    isDirectory = Files.isDirectory(rootFile),
                    lastModified = new java.sql.Date(Files.getLastModifiedTime(rootFile).toMillis),
                    isFetched = false,
                    isFinish = false))
              }
            case None =>
              Option(IndexPathRow(
                id = -1,
                uri = rootFile.toUri.toASCIIString,
                parentDirId = -1,
                isDirectory = Files.isDirectory(rootFile),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(rootFile).toMillis),
                isFetched = false,
                isFinish = false,
                contentId = content.id))

          }
        }.flatMap {
          case Some(indexModel) if indexModel.id > 0 =>
            fileDB.writeDB.run {
              (IndexPath.filter(_.id === indexModel.id).update(indexModel) >> DBIO.successful(Option(indexModel))).transactionally
              /*id = -1,
                dirPath = rootDir.toPath.toRealPath().toString,
                isFinish = false)*/
            }
          case Some(indexModel) if indexModel.id < 0 =>
            fileDB.writeDB.run {
              IndexPath
                .returning(IndexPath.map(_.id))
                .into((dir, id) => Option(dir.copy(id = id))) += indexModel
              /*id = -1,
                  dirPath = rootDir.toPath.toRealPath().toString,
                  isFinish = false)*/
            }
          case _ =>
            Future.successful(Option.empty)
        }
      }
      f.flatMap((_: Option[IndexPathRow]) => fetchFiles(content))
    }

    def tranFiles(sum: Int, isFetchFileFinished: () => Boolean): Future[Int] = {
      val isIndexing = !isFetchFileFinished()
      val fileListF = fileDB.db.run(
        IndexPath.filter(s => (s.isFinish === false) && (s.isDirectory === false) && (s.contentId === content.id)).take(120).result)
      //(s"是否已查找文件完毕：${!isIndexing}")
      (for {
        fileList <- fileListF
      } yield {
        if (needToShutdonw) {
          Future.successful(sum)
        } else if (fileList.isEmpty && (!isIndexing)) {
          Future.successful(sum)
        } else if (!fileList.isEmpty) {
          val listF = Future.sequence(fileList.map { f =>
            val file = new File(URI.create(f.uri))

            def indexLimited[T](f: () => Future[T]): Future[T] = {
              val aa = () => fileTimeIndexLimit.limit(f, 1, s"索引文件（${file.toPath.toUri}）")
              fileSizeIndexLimit.limit(aa, Files.size(file.toPath), s"索引文件（${file.toPath.toUri}）")
            }

            indexLimited(() => {
              logger.debug(s"${new Date().toString}，正在索引：${f.uri}")
              indexFile(file.toPath, dbId = f.id, contentId = f.contentId).flatMap {
                case Right(info) =>
                  {
                    /*val doc = new SolrInputDocument()
                    doc.addField("id", f.id)
                    doc.addField("file_name", info.fileName)
                    doc.addField("file_content", info.content)
                    doc.addField("file_path", info.filePath)
                    doc.addField("law_file_name", info.fileName)
                    doc.addField("content_id", content.id)

                    info.content.grouped(32766 / 3 - 200).zipWithIndex.foldLeft("") {
                      case (prefix, (content, index)) =>
                        doc.addField("law_file_content_" + index, prefix + content)
                        content.takeRight(20)
                    }
                    doc.addField("law_file_path", info.filePath)
                    embeddedServer.solrServer.add("file_index", doc)
                    embeddedServer.solrServer.commit("file_index")*/

                    embeddedServer.esLocalClient.flatMap { client =>
                      client.execute {
                        indexInto(embeddedServer.index, embeddedServer.typeName)
                          .id(info.dbId.toString)
                          .doc(info.asJson)
                          .refresh(RefreshPolicy.NONE)
                      }
                    }.map { (s: Either[RequestFailure, RequestSuccess[IndexResponse]]) => info.dbId -> s }
                  }.transform {
                    r =>
                      r match {
                        case Success((dbId, result)) =>
                          result match {
                            case Left(failInfo) =>
                              logger.info(s"${new Date().toString}，文件：${info.filePath}的索引工作遇到错误，" +
                                s"错误信息：{ reason: ${failInfo.error.reason}, rootCause: ${failInfo.error.rootCause}")
                              Try { dbId -> 1 }
                            case Right(successResponse) =>
                              logger.info(s"${new Date().toString}，已完成文件：${info.filePath}的索引工作")
                              logger.trace(s"${new Date().toString}，已完成文件：${info.filePath}的索引工作\n索引内容：${info.fileContent}")
                              Try { dbId -> 0 }
                          }
                        case Failure(e) =>
                          logger.error(s"${new Date().toString}，索引：${Paths.get(URI.create(f.uri)).toRealPath().toString}失败")
                          Try { info.dbId -> 0 }
                        //e.printStackTrace
                      }
                  }
                case Left(id) =>
                  logger.error(s"${new Date().toString}，索引：${file.toPath.toRealPath()}失败，跳过此文件")
                  Future.successful(id -> 1)
              }.andThen {
                case Failure(e) =>
                  logger.info(s"索引文件：${f.uri}过程发生错误", e)
              }: Future[(Int, Int)]
            } /*, file.length, s"索引文件（${f.uri}）"*/ )
          })
          listF.flatMap { ids =>
            fileDB.writeDB.run(
              IndexPath.filter(_.id inSetBind ids.filter(_._2 > 0).map(_._1)).map(_.isFinish).update(true).transactionally)
              .map(_ => sum + ids.map(_._2).sum).flatMap(newSum => tranFiles(newSum, isFetchFileFinished))
          }: Future[Int]
        } else {
          val promise = Promise[Future[Int]]
          val timer = new Timer()
          shutdownHook.addHook(new Thread() { override def run: Unit = { Try { timer.cancel() } } })
          val task = new TimerTask {
            override def run(): Unit = {
              promise.success(tranFiles(sum, isFetchFileFinished))
              timer.cancel()
            }
          }
          timer.schedule(task, 500)
          promise.future.flatten
        }
      }).flatten.andThen {
        case Failure(e) =>
          logger.info("索引文件过程发生错误", e)
      }
    }

    def showInfo(isIndexFinished: () => Boolean): Future[Int] = {
      /*val timer = new Timer()
      shutdownHook.addHook(new Thread() { override def run: Unit = { Try { timer.cancel() } } })
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
      timer.schedule(task, 8000, 8000)*/
      Future.successful(1)
    }

    val indexAction = /*if (Files.isDirectory(rootPath))*/ {
      val fetchFilesF = startFetchFiles(content).recover {
        case e => e.printStackTrace
      }
      val indexFilesF = tranFiles(0, () => fetchFilesF.isCompleted)
      indexFilesF.map { count =>
        println(s"索引:${Paths.get(URI.create(content.rootUri)).toRealPath()}完成，一共索引了:${count}个文件")
        1
      }.recover {
        case e =>
          //e.printStackTrace
          2
      }
      showInfo(() => indexFilesF.isCompleted)
      indexFilesF
    } /*else {
      Future.successful(3)
    }*/
    indexAction
  }

  def indexFile(file: Path, dbId: Int, contentId: Int): Future[Either[Int, IndexInfo]] = {
    Future {
      val fileName = file.getFileName.toString
      if (Files.size(file) < (2 * 1024 * 1024)) {
        fileExtraction.indexer.find { case (extName, _) => fileName.endsWith(s".${extName}") }.map(_._2).map(_.apply(file).map { strEither =>
          /*strEither.right.map { str =>
            IndexInfo(dbId = id, filePath = file.toRealPath().toString, fileName = file.getFileName().toString, content = str)
          }.left.map(_ => id)*/
          strEither match {
            case Right(str) =>
              Right(IndexInfo(dbId = dbId, filePath = file.toRealPath().toString, fileName = file.getFileName().toString, fileContent = str, contentId = contentId))
            case Left(e) =>
              logger.error(s"索引文件发生错误，路径：${file.toRealPath().toString}", e)
              Left(dbId)
            //throw e
          }
        }).getOrElse {
          Future.successful(Left(dbId))
        }
      } else {
        Future.successful(Left(dbId))
      }
    }.flatten.andThen {
      case Failure(e) =>
        logger.error(s"索引单个文件发生错误", e)
    }
  }

}

case class IndexInfo(dbId: Int, filePath: String, fileName: String, fileContent: String, contentId: Int)

object IndexInfo {

  import io.circe.generic.extras.auto._

  implicit val config = io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames

  implicit val encoder: Encoder[IndexInfo] = {
    exportEncoder.instance
  }

  implicit val decoder: Decoder[IndexInfo] = {
    exportDecoder.instance
  }

}