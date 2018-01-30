package org.xarcher.emiya.service

import java.nio.file.Path
import java.util.Date

import org.xarcher.xPhoto.FileTables

import scala.concurrent.{ ExecutionContext, Future }

class ContentService(val fileTables: FileTables)(implicit ec: ExecutionContext) {

  import fileTables._
  import fileTables.profile.api._

  def create(path: Path): Future[Boolean] = {
    val content = IndexContentRow(
      id = -1,
      rootUri = path.toUri.toASCIIString,
      //isDirectory = Files.isDirectory(path),
      //lastModified = new java.sql.Date(Files.getLastModifiedTime(path).value),
      order = 100,
      createTime = new java.sql.Date(new Date().getTime),
      isFinish = false)
    db.run(
      IndexContent.returning(IndexContent.map(_.id)).into((model, id) => model.copy(id = id)) += content).map((_: IndexContentRow) => true)
  }

  def list: Future[List[IndexContentRow]] = {
    db.run(IndexContent.sortBy(s => (s.createTime.desc)).to[List].result)
  }

}