package org.xarcher.emiya.utils

import java.net.URI
import java.nio.file.{ Files, Paths }
import java.util.stream.Collectors

import org.xarcher.xPhoto.{ FileDB }
import org.xarcher.xPhoto.FileTables._

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConverters._

class FileUpdate(
  compareGen: CompareGen,
  fileDB: FileDB) {

  import profile.api._

  def updateIndexRow(pathRow: IndexPathRow, content: IndexContentRow)(implicit executionContext: ExecutionContext): Future[Int] = {
    val subRowsF = fileDB.db.run(IndexPath.filter(s => (s.parentDirId === pathRow.id) && (s.contentId === content.id)).to[List].result)
    //val subFiles = Paths.get(URI.create(pathRow.uri)).toFile.listFiles.toList.map(_.toPath)
    val subFiles = Files.list(Paths.get(URI.create(pathRow.uri))).collect(Collectors.toList()).asScala.toList

    val dealsAction = subRowsF.map { rows =>
      val compares = compareGen.compareWithSorted(subFiles, rows, parentId = pathRow.id, contentId = content.id)
      val actions = compares.map {
        case AddToLucence(row) =>
          //println("+ " + row)
          IndexPath.returning(IndexPath.map(_.id)).into((model, id) => model.copy(id = id)) += row
        case RemoveFromLucence(row) =>
          //println("- " + row)
          IndexPath.filter(_.id === row.id).delete.map((_: Int) => row)
        case Modified(row) =>
          //println("* " + row)
          IndexPath.filter(_.id === row.id).update(row).map((_: Int) => row)
      }
      val actions1: DBIO[Seq[IndexPathRow]] = (
        IndexPath.filter(_.id === pathRow.id).map(_.isFetched).update(true) >>
        DBIO.sequence(actions)).transactionally
      fileDB.db.run(actions1)
    }
    dealsAction.flatten.map(_.size)
  }

}