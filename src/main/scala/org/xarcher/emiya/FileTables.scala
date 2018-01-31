package org.xarcher.xPhoto

import java.sql.Date

import org.xarcher.emiya.utils.{ FutureLimited, FutureLimitedGen, LimitedActor, ShutdownHook }
import slick.jdbc.{ H2Profile, JdbcProfile, SQLiteProfile }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class FileDB(futureLimitedGen: FutureLimitedGen, shutdownHook: ShutdownHook) {
  val profile = FileTables.profile

  val fLimited = futureLimitedGen.create(4, "dbPool")

  import profile.api._

  trait ExtDB {
    protected val db: Database
    final def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = {
      fLimited.limit(() => db.run(a), "dbPool")
    }
  }

  lazy val db = Database.forURL(driver = "org.h2.Driver", url = "jdbc:h2:./ext_persistence_不索引/db/file_db.h2")

  shutdownHook.addHook { () =>
    Future.successful(db.close())
  }

  lazy val writeDB: ExtDB = {
    val db1 = db
    new ExtDB {
      override protected val db = db1
    }
  }

}

object FileTables extends FileTables {
  override val profile = H2Profile
}

trait FileTables {

  val profile: JdbcProfile

  import profile.api._

  lazy val schema = /*FilePrepare.schema ++ DirectoryPrepare.schema ++*/ IndexContent.schema ++ IndexPath.schema

  /*case class FilePrepareRow(
    id: Int,
    parentDirId: Int,
    filePath: String,
    isFinish: Boolean)

  class FilePrepare(tag: Tag) extends Table[FilePrepareRow](tag, "file_prepare") {
    val id = column[Int]("id", O.AutoInc)
    val parentDirId = column[Int]("parent_dir_id")
    val filePath = column[String]("file_path")
    val isFinish = column[Boolean]("is_finish")
    override val * = (id, parentDirId, filePath, isFinish).mapTo[FilePrepareRow]
  }

  val FilePrepare = TableQuery[FilePrepare]

  case class DirectoryPrepareRow(
    id: Int,
    dirPath: String,
    isFinish: Boolean)

  class DirectoryPrepare(tag: Tag) extends Table[DirectoryPrepareRow](tag, "directory_prepare") {
    val id = column[Int]("id", O.AutoInc)
    val dirPath = column[String]("dir_path")
    val isFinish = column[Boolean]("is_finish")
    override val * = (id, dirPath, isFinish).mapTo[DirectoryPrepareRow]
  }

  val DirectoryPrepare = TableQuery[DirectoryPrepare]*/

  case class IndexContentRow(
    id: Int,
    rootUri: String,
    order: Int,
    createTime: Date,
    isFinish: Boolean)

  class IndexContent(tag: Tag) extends Table[IndexContentRow](tag, "index_content") {
    val id = column[Int]("id", O.AutoInc)
    val rootUri = column[String]("root_uri")
    val order = column[Int]("order")
    val createTime = column[Date]("create_time")
    val isFinish = column[Boolean]("is_finish")
    override val * = (id, rootUri, order, createTime, isFinish).mapTo[IndexContentRow]
  }

  val IndexContent = TableQuery[IndexContent]

  case class IndexPathRow(
    id: Int,
    uri: String,
    isDirectory: Boolean,
    lastModified: Date,
    isFinish: Boolean,
    parentDirId: Int,
    contentId: Int)

  class IndexPath(tag: Tag) extends Table[IndexPathRow](tag, "index_path") {
    val id = column[Int]("id", O.AutoInc)
    val uri = column[String]("uri")
    val isDirectory = column[Boolean]("is_directory")
    val lastModified = column[Date]("last_modified")
    val isFinish = column[Boolean]("is_finish")
    val parentDirId = column[Int]("parent_dir_id")
    val contentId = column[Int]("content_id")
    override val * = (id, uri, isDirectory, lastModified, isFinish, parentDirId, contentId).mapTo[IndexPathRow]
  }

  val IndexPath = TableQuery[IndexPath]

}