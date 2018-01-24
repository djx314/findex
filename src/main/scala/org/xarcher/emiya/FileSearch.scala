package org.xarcher.xPhoto

import java.awt.Desktop
import java.io.{ File, IOException }
import java.nio.file.{ Files, Path, Paths }
import java.util.{ Date, Timer, TimerTask }

import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{ DirectoryReader, IndexWriter, IndexWriterConfig }
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.highlight.{ Highlighter, QueryScorer, SimpleFragmenter, SimpleHTMLFormatter }
import org.apache.lucene.store.FSDirectory
import scalafx.Includes._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }
import scalafx.scene.control.Button
import scalafx.scene.input.MouseEvent
import scalafx.scene.paint.Color
import scalafx.scene.text.{ Font, Text, TextFlow }

object FileSearch {

  val path = "./lucenceTemp"

  def search(keyWord: String): Future[List[OutputInfo]] = {
    var indexSearcher: IndexSearcher = null
    val f = Future {
      val directory = FSDirectory.open(Paths.get(path))
      val analyzer = new CJKAnalyzer()
      indexSearcher = new IndexSearcher(DirectoryReader.open(directory))

      val fields = Array("fileName", "content")
      val mparser = new MultiFieldQueryParser(fields, analyzer)
      val mQuery = mparser.parse(keyWord)

      val docs = indexSearcher.search(mQuery, 20).scoreDocs

      docs.map { doc =>
        val hitDoc = indexSearcher.doc(doc.doc)
        val model = OutputInfo(
          fileName = Option(hitDoc.get("fileName")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
          content = Option(hitDoc.get("content")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
          filePath = Option(hitDoc.get("filePath")).map(_.trim).filterNot(_.isEmpty).getOrElse(""))

        // 生成高亮器
        val textSize = 300

        val formatter = new SimpleHTMLFormatter("|||", "|||")
        val scorer = new QueryScorer(mQuery)
        val highlighter = new Highlighter(formatter, scorer)
        highlighter.setTextFragmenter(new SimpleFragmenter(textSize))
        // 使用高亮器：对content属性值进行摘要并高亮
        val newContent = highlighter.getBestFragment(analyzer.tokenStream("", model.content), model.content)

        val titleSize = 80
        val titleHighlighter = new Highlighter(formatter, scorer)
        titleHighlighter.setTextFragmenter(new SimpleFragmenter(titleSize))
        val newTitle = titleHighlighter.getBestFragment(analyzer.tokenStream("", model.fileName), model.fileName)
        model.copy(content = Option(newContent).getOrElse(model.content.take(textSize)), fileName = Option(newTitle).getOrElse(model.fileName.take(titleSize)))
      }.toList
    }
    f
  }

}

case class OutputInfo(filePath: String, fileName: String, content: String) {

  def fileNameFlow: TextFlow = {
    val strs = fileName.split("\\|\\|\\|").toList
    //val strs1 = if (strs.startsWith("|||")) strs else "" :: strs
    val str2 = strs.zipWithIndex.map {
      case (item, index) =>
        if (index % 2 == 1) {
          new Text(item) {
            font = Font.font("微软雅黑", 16)
            fill = Color.Red
          }
        } else {
          new Text(item) {
            font = Font.font("微软雅黑", 16)
            fill = Color.Black
          }
        }
    }

    new TextFlow {
      children = str2
    }
  }

  def contentFlow: TextFlow = {
    val strs = content.split("\\|\\|\\|").toList
    //val strs1 = if (strs.startsWith("|||")) strs else "" :: strs
    val str2 = strs.zipWithIndex.map {
      case (item, index) =>
        if (index % 2 == 1) {
          new Text(item) {
            font = Font.font("微软雅黑", 16)
            fill = Color.Red
          }
        } else {
          new Text(item) {
            font = Font.font("微软雅黑", 16)
            fill = Color.Black
          }
        }
    }

    new TextFlow {
      children = str2
    }
  }

  def fileBtn: Button = new Button("打开文件") {
    handleEvent(MouseEvent.MouseClicked) {
      event: MouseEvent =>
        Desktop.getDesktop.open(new File(filePath))
        ()
    }
  }

  def dirBtn: Button = new Button("打开文件夹") {
    handleEvent(MouseEvent.MouseClicked) {
      event: MouseEvent =>
        (Desktop.getDesktop: java.awt.Desktop).open(new File(filePath).getParentFile)
        ()
    }
  }

}