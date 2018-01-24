package org.xarcher.xPhoto

import slick.jdbc.{ JdbcProfile, SQLiteProfile }

object FileTables extends FileTables {
  override val profile: SQLiteProfile = SQLiteProfile

  import profile.api._

  val db = Database.forURL(driver = "org.sqlite.JDBC", url = "jdbc:sqlite:./file_db.db3")
}

trait FileTables {

  val profile: JdbcProfile

  import profile.api._

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