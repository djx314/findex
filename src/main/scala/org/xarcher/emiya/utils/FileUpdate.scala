package org.xarcher.emiya.utils

import java.net.URI
import java.nio.file.{ Files, Path, Paths }

import org.xarcher.xPhoto.{ FileDB, IndexExecutionContext }
import org.xarcher.xPhoto.FileTables._

import scala.concurrent.Future

class FileUpdate(
  compareGen: CompareGen,
  fileDB: FileDB,
  indexExecutionContext: IndexExecutionContext) {

  import profile.api._

  implicit val ec = indexExecutionContext.indexEc

  def updateIndexRow(pathRow: IndexPathRow, content: IndexContentRow): Future[Int] = {
    val subRowsF = fileDB.db.run(IndexPath.filter(s => (s.parentDirId === pathRow.id) && (s.contentId === content.id)).to[List].result)
    val subFiles = Paths.get(URI.create(pathRow.uri)).toFile.listFiles.toList.map(_.toPath)
    val dealsAction = subRowsF.map { rows =>
      val compares = compareGen.compareWithSorted(subFiles, rows, parentId = pathRow.id, contentId = content.id)
      val actions = compares.map {
        case AddToLucence(row) =>
          IndexPath.returning(IndexPath.map(_.id)).into((model, id) => model.copy(id = id)) += row
        case RemoveFromLucence(row) =>
          IndexPath.filter(_.id === row.id).delete.map((_: Int) => row)
        case Modified(row) =>
          IndexPath.filter(_.id === row.id).update(row).map((_: Int) => row)
      }
      val actions1: DBIO[Seq[IndexPathRow]] = DBIO.sequence(actions).transactionally
      fileDB.db.run(actions1)
    }
    dealsAction.flatten.map(_.size)
  }

}