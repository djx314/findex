package org.xarcher.emiya.utils

import java.nio.file.{ Files, Path }

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
    compareLoop(Queue(sortedLocalPaths.map(_._2): _*), Queue(sortedDBPaths: _*), EqualsName, parentId, contentId)
  }

  def compareLoop(localPaths: Queue[Path], dbPaths: Queue[IndexPathRow], content: CompareFileContent, partentId: Int, contentId: Int): List[FileCompare] = {
    content match {
      case LocalPath(local) =>
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
                isFinish = false,
                parentDirId = partentId,
                contentId = contentId)) :: compareLoop(localPaths, tailDB, DBPath(headDB), partentId, contentId)
            } else {
              val localModified = Files.getLastModifiedTime(local).toMillis
              val dbModified = headDB.lastModified.getTime
              if (localModified == dbModified) {
                compareLoop(localPaths, tailDB, EqualsName, partentId, contentId)
              } else {
                Modified(
                  headDB.copy(
                    isDirectory = Files.isDirectory(local),
                    lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                    isFinish = false)) ::
                  compareLoop(localPaths, tailDB, EqualsName, partentId, contentId)
              }
            }
          case None =>
            localPaths.map { path =>
              AddToLucence(
                IndexPathRow(
                  id = -1,
                  uri = path.toRealPath().toUri.toASCIIString,
                  isDirectory = Files.isDirectory(path),
                  lastModified = new java.sql.Date(Files.getLastModifiedTime(path).toMillis),
                  isFinish = false,
                  parentDirId = partentId,
                  contentId = contentId))
            }.toList
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
                isFinish = false,
                parentDirId = partentId,
                contentId = contentId)) :: compareLoop(localQueue, dbPaths, DBPath(dbPath), partentId, contentId)
            } else if (localUri > dbUri) {
              RemoveFromLucence(dbPath) :: compareLoop(localQueue, dbPaths, LocalPath(local), partentId, contentId)
            } else {
              val localModified = Files.getLastModifiedTime(local).toMillis
              val dbModified = dbPath.lastModified.getTime
              if (localModified == dbModified) {
                compareLoop(localQueue, dbPaths, EqualsName, partentId, contentId)
              } else {
                Modified(
                  dbPath.copy(
                    isDirectory = Files.isDirectory(local),
                    lastModified = new java.sql.Date(Files.getLastModifiedTime(local).toMillis),
                    isFinish = false)) ::
                  compareLoop(localQueue, dbPaths, EqualsName, partentId, contentId)
              }
            }
          case None =>
            dbPaths.map { path =>
              RemoveFromLucence(
                path)
            }.toList
        }
      case EqualsName =>
        localPaths.dequeueOption match {
          case Some((local, localQueue)) =>
            compareLoop(localQueue, dbPaths, LocalPath(local), partentId, contentId)
          case None =>
            dbPaths.dequeueOption match {
              case Some((dbPath, dbQueue)) =>
                compareLoop(localPaths, dbQueue, DBPath(dbPath), partentId, contentId)
              case None =>
                List.empty[FileCompare]
            }
        }
    }
  }

}