package org.xarcher.xPhoto

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import java.util.{Date, Timer, TimerTask}

import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object FileIndex {

  val txtGen: Path => Either[Throwable, String] = { path =>
    Try {
      IOUtils.toString(path.toUri.toURL, "utf-8")
    }.toEither
  }

  val indexer: Map[String, Path => Either[Throwable, String]] = Map(
    "txt" -> txtGen,
    "js" -> txtGen,
    "scala" -> txtGen,
    "java" -> txtGen,
    "php" -> txtGen,
    "css" -> txtGen,
    "conf" -> txtGen,
    "properties" -> txtGen
  )

  def index(file: Path): Future[Int] = {
    val writer = writerGen

    val fileQueue = mutable.Queue.empty[File]
    val dirQueue = mutable.Queue.empty[File]

    @tailrec
    def indexFiles(eachDir: File): Boolean = {
      //println("1111" + eachDir.getCanonicalFile.toString)
      val subFiles = eachDir.listFiles().toList
      val subDirs = subFiles.filter(_.isDirectory)
      val simpleFiles = subFiles.filterNot(_.isDirectory)

      dirQueue.enqueue(subDirs: _*)
      //println(s"添加文件夹${subDirs}")
      fileQueue.enqueue(simpleFiles: _*)
      //println(s"添加文件${simpleFiles}")
      if (dirQueue.isEmpty) {
        true
      } else {
        indexFiles(dirQueue.dequeue())
      }
    }

    var count = 0

    def tranFiles: Future[Boolean] = {
      if (fileQueue.isEmpty && dirQueue.isEmpty) {
        Future.successful(true)
      } else if (! fileQueue.isEmpty) {
        val file = fileQueue.dequeue()
        indexFile(writer, file.toPath).flatMap { _ =>
          count += 1
          if (count % 100 == 0) {
            println(s"索引了:${count}个文件")
          }
          tranFiles
        }: Future[Boolean]
      } else {
        //fileQueue.size > 0 && dirQueue.isEmpty
        val promise = Promise[Future[Boolean]]
        val timer = new Timer()
        val task = new TimerTask {
          override def run(): Unit = {
            promise.success(tranFiles)
          }
        }
        timer.schedule(task, 500)
        promise.future.flatten
      }
    }

    if (Files.isDirectory(file)) {
      Future {
        indexFiles(file.toFile)
      }.recover {
        case e => e.printStackTrace
      }
      val promise = Promise[Future[Boolean]]
      val timer = new Timer()
      val task = new TimerTask {
        override def run(): Unit = {
          promise.success(tranFiles)
        }
      }
      timer.schedule(task, 500)
      promise.future.flatten.map { _ =>
        println(s"索引:${file.toRealPath()}完成，一共索引了:${count}个文件")
        1
      }.recover {
        case e =>
          e.printStackTrace
          2
      }.andThen {
        case _ =>
          if (null != writer) {
            Try {
              writer.close()
            }.fold(
              e => e.printStackTrace(),
              _ => ()
            )
          }
      }
    } else {
      Future.successful(3)
    }
  }

  def writerGen: IndexWriter = {
    val f = {
      //1、创建Derictory
      //        Directory directory = new RAMDirectory();//这个方法是建立在内存中的索引
      val directory = FSDirectory.open(Paths.get(path))
      //这个方法是建立在磁盘上面的索引
      //        2、创建IndexWriter，用完后要关闭
      val analyzer = new CJKAnalyzer()
      val indexWriterConfig = new IndexWriterConfig(analyzer)
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      new IndexWriter(directory, indexWriterConfig)
    }
    f
  }

  val path = "./lucenceTemp"

  def indexFile(writer: IndexWriter, file: Path): Future[Int] = Future {
    val fileName = file.getFileName.toString
    val strOpt = indexer.find { case (extName, _) => fileName.endsWith(s".${extName}") }.map(_._2).flatMap(_.apply(file).toOption)

    for {
      str <- strOpt
    } yield {
      val document = new Document()
      document.add(new TextField("fileName", file.toRealPath().toString, Field.Store.YES))
      document.add(new TextField("content", str, Field.Store.YES))
      document.add(new TextField("filePath", file.toRealPath().toString, Field.Store.YES))
      writer.addDocument(document)
      //println(s"索引:${file.toRealPath()}成功")
    }
    1
  }.recover {
    case e: IOException =>
      e.printStackTrace()
      0
  }

}

case class IndexInfo(filePath: String, fileName: String, content: String)

trait FetchIndex {

  def fetch(file: File): IndexInfo

}