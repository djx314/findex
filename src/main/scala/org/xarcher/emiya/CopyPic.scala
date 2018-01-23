package org.xarcher.xPhoto

import java.io.File
import java.nio.file.{Files, Path}

import scala.concurrent.Future

object FileIndex {

  def index(file: Path): Future[Int] = {
    if (Files.isDirectory(file)) {
      Future.successful(2)
    } else {
      Future.successful(3)
    }
  }

}

case class IndexInfo(filePath: String, fileName: String, content: String)

trait FetchIndex {

  def fetch(file: File): IndexInfo

}