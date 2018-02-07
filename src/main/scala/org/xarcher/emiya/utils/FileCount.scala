package org.xarcher.emiya.utils

import java.net.URI
import java.nio.file.{ Files, Path, Paths }

import org.xarcher.xPhoto.FileTables._

import scala.collection.immutable.Queue

sealed trait FileCompare

case class AddToLucence(toAdd: IndexPathRow) extends FileCompare
case class RemoveFromLucence(toRemove: IndexPathRow) extends FileCompare
case class Modified(toModified: IndexPathRow) extends FileCompare

sealed trait CompareFileContent

case class LocalPath(path: Path) extends CompareFileContent
case class DBPath(dbModel: IndexPathRow) extends CompareFileContent
case object EqualsName extends CompareFileContent

class CompareGen() {

  def compareWithSorted(localPaths: List[Path], dbPaths: List[IndexPathRow], parentId: Int, contentId: Int): List[FileCompare] = {
    val mappedPaths = localPaths.map(s => s.toRealPath().toUri.toASCIIString -> s)
    val sortedLocalPaths = mappedPaths.sortBy(_._1)
    val sortedDBPaths = dbPaths.sortBy(_.uri)
    compareLoop(Queue(sortedLocalPaths.map(_._2): _*), Queue(sortedDBPaths: _*), EqualsName, partentId = parentId, contentId = contentId)
  }

  def compareLoop(localPaths: Queue[Path], dbPaths: Queue[IndexPathRow], content: CompareFileContent, partentId: Int, contentId: Int): List[FileCompare] = {
    content match {
      case LocalPath(local) =>
        //println("55" * 100)
        //println("66" + local.toRealPath().toString)
        dbPaths.dequeueOption match {
          case Some((headDB, tailDB)) =>
            val dbUri = headDB.uri
            val localUri = local.toUri.toASCIIString
            if (localUri > dbUri) {
              RemoveFromLucence(headDB) :: compareLoop(localPaths, tailDB, LocalPath(local), partentId, contentId)
            } else if (localUri < dbUri) {
              AddToLucence(IndexPathRow(
                id = -1,
                uri = local.toRealPath().toUri.toASCIIString,
                isDirectory = Files.isDirectory(local),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                isFetched = if (Files.isDirectory(local)) false else true,
                isFinish = false,
                parentDirId = partentId,
                contentId = contentId)) :: compareLoop(localPaths, tailDB, DBPath(headDB), partentId, contentId)
            } else {
              val localModified = Files.getLastModifiedTime(local).toMillis
              val dbModified = headDB.lastModified.getTime
              if (localModified == dbModified) {
                Modified(
                  headDB.copy(isFetched = true)) :: compareLoop(localPaths, tailDB, EqualsName, partentId, contentId)
              } else {
                Modified(
                  headDB.copy(
                    isDirectory = Files.isDirectory(local),
                    lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                    isFetched = if (Files.isDirectory(local)) false else true,
                    isFinish = false)) ::
                  compareLoop(localPaths, tailDB, EqualsName, partentId, contentId)
              }
            }
          case None =>
            //println("11" * 100)
            (local :: localPaths.toList).map { path =>
              AddToLucence(
                IndexPathRow(
                  id = -1,
                  uri = path.toRealPath().toUri.toASCIIString,
                  isDirectory = Files.isDirectory(path),
                  lastModified = new java.sql.Date(Files.getLastModifiedTime(path).toMillis),
                  isFetched = if (Files.isDirectory(path)) false else true,
                  isFinish = false,
                  parentDirId = partentId,
                  contentId = contentId))
              //println("44:" + aa + ":isDirectory:" + Files.isDirectory(path) + ":pathStr:" + local.toString + ":isExists:" + Files.exists(local))
            }
        }
      case DBPath(dbPath) =>
        localPaths.dequeueOption match {
          case Some((local, localQueue)) =>
            val dbUri = dbPath.uri
            val localUri = local.toUri.toASCIIString
            if (localUri < dbUri) {
              AddToLucence(IndexPathRow(
                id = -1,
                uri = local.toRealPath().toUri.toASCIIString,
                isDirectory = Files.isDirectory(local),
                lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                isFetched = if (Files.isDirectory(local)) false else true,
                isFinish = false,
                parentDirId = partentId,
                contentId = contentId)) :: compareLoop(localQueue, dbPaths, DBPath(dbPath), partentId, contentId)
            } else if (localUri > dbUri) {
              RemoveFromLucence(dbPath) :: compareLoop(localQueue, dbPaths, LocalPath(local), partentId, contentId)
            } else {
              val localModified = Files.getLastModifiedTime(local).toMillis
              val dbModified = dbPath.lastModified.getTime
              if (localModified == dbModified) {
                Modified(
                  dbPath.copy(isFetched = true)) :: compareLoop(localQueue, dbPaths, EqualsName, partentId, contentId)
              } else {
                Modified(
                  dbPath.copy(
                    isDirectory = Files.isDirectory(local),
                    lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                    isFetched = if (Files.isDirectory(local)) false else true,
                    isFinish = false)) ::
                  compareLoop(localQueue, dbPaths, EqualsName, partentId, contentId)
              }
            }
          case None =>
            (dbPath :: dbPaths.toList).map { path =>
              RemoveFromLucence(path)
            }
        }
      case EqualsName =>
        localPaths.dequeueOption match {
          case Some((local, localQueue)) =>
            //println("22" * 100)
            //println("77" + local.toRealPath().toString)
            compareLoop(localQueue, dbPaths, LocalPath(local), partentId, contentId)
          case None =>
            //println("33" * 100)
            dbPaths.dequeueOption match {
              case Some((dbPath, dbQueue)) =>
                //println("88" + Paths.get(URI.create(dbPath.uri)).toRealPath().toString)
                compareLoop(localPaths, dbQueue, DBPath(dbPath), partentId, contentId)
              case None =>
                List.empty[FileCompare]
            }
        }
    }
  }

}