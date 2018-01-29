package org.xarcher.xPhoto

import akka.actor.ActorRef
import com.softwaremill.tagging.@@
import org.xarcher.emiya.utils.{ FutureLimited, FutureLimitedGen, LimitedActor, ShutdownHook }
import slick.jdbc.{ H2Profile, JdbcProfile, SQLiteProfile }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class FileTables(futureLimitedGen: FutureLimitedGen, shutdownHook: ShutdownHook) extends FileTables1 {
  override val profile: H2Profile = H2Profile

  val fLimited = futureLimitedGen.create(6, "dbPool")

  import profile.api._

  trait ExtDB {
    //@volatile var count = 0

    protected val db: Database
    final def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = {
      /*val c = count
      count += 1
      println(s"开始$c")
      fLimited.limit(() => db.run(a)).map {
        result =>
          println(s"完成$c")
          result
      }*/
      fLimited.limit(() => db.run(a), "")
    }
  }

  lazy val db = Database.forURL(driver = "org.h2.Driver", url = "jdbc:h2:./file_db_不索引.h2")

  shutdownHook.addHook(() => Future.successful(db.close()))

  lazy val writeDB: ExtDB = {
    val db1 = db
    new ExtDB {
      override protected val db = db1
    }
  }

}

trait FileTables1 {

  val profile: JdbcProfile

  import profile.api._

  lazy val schema = FilePrepare.schema ++ DirectoryPrepare.schema

  case class FilePrepareRow(
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

  val DirectoryPrepare = TableQuery[DirectoryPrepare]

}