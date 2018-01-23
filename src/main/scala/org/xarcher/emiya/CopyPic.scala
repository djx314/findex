package org.xarcher.xPhoto

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import java.util.Date

import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object FileIndex {

  val indexer: Map[String, Path => String] = Map(
    "txt" -> { path =>
      IOUtils.toString(path.toUri.toURL, "utf-8")
    }
  )

  def index(file: Path): Future[Int] = {
    if (Files.isDirectory(file)) {
      Future.successful(2)
    } else {
      Future.successful(3)
    }
  }

  var writer: Future[IndexWriter] = {
    val f = Future {
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

  def indexFile(writer: IndexWriter, file: Path) = {
    var writer: IndexWriter = null
    val f = Future {
      //3、创建Document对象
      articles.map { case (model, article) if article.content.map(_.trim).filterNot(_.isEmpty).isDefined =>
        val contentGet = article.content.get
        val newContent = Jsoup.parse(contentGet).text()

        val document = new Document()
        document.add(new SortedNumericDocValuesField("generalid", model.generalid))
        //上面已经做了过滤了
        document.add(new TextField("content", newContent, Field.Store.YES))
        document.add(new SortedNumericDocValuesField("updatetime", model.updatetime.map(_.getTime).getOrElse(new Date().getTime)))
        document.add(new TextField("author", article.author.getOrElse(""), Field.Store.YES))
        document.add(new TextField("title", model.title, Field.Store.YES))

        document.add(new StoredField("generalid_save", model.generalid))
        document.add(new StoredField("updatetime_save", model.updatetime.map(_.getTime).getOrElse(new Date().getTime)))
        document.add(new StoredField("url_save", s"http://www.heshan.gov.cn/Item/${model.generalid}.aspx"))
        document.add(new StoredField("author_save", article.author.getOrElse("")))
        document.add(new StoredField("title_save", model.title))


        writer.addDocument(document)
        1
      }.sum
    }.recover {
      case e: IOException =>
        e.printStackTrace()
        0
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
  }

}

case class IndexInfo(filePath: String, fileName: String, content: String)

trait FetchIndex {

  def fetch(file: File): IndexInfo

}