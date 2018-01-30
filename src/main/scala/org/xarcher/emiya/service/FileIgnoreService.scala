package org.xarcher.emiya.service

import java.io.File
import java.nio.file.Path

class FileIgnoreService() {

  val ignoreDir: Path => Boolean = { file =>
    val fileName = file.getFileName.toString
    fileName.endsWith("_不索引") || (fileName == "target")
  }

  val ignoreFile: Path => Boolean = { file =>
    val fileName = file.getFileName.toString
    fileName.takeWhile(_ != '.').endsWith("_不索引")
  }

}