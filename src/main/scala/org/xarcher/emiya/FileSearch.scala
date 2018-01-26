package org.xarcher.xPhoto

import java.awt.Desktop
import java.io.File
import java.nio.file.Paths

import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.highlight.{ Highlighter, QueryScorer, SimpleFragmenter, SimpleHTMLFormatter }
import org.apache.lucene.store.FSDirectory
import org.fxmisc.richtext.{ InlineCssTextArea, StyledTextArea }

import scalafx.Includes._
import scala.concurrent.{ ExecutionContext, Future }
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{ Background, BackgroundFill, CornerRadii, Region }
import scalafx.scene.paint.Paint
import scalafx.scene.text.TextAlignment

object FileSearch {

  val path = "./lucenceTemp"

  def search(keyWord: String)(implicit ec: ExecutionContext): Future[List[OutputInfo]] = {
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

  def fileNameFlow: InlineCssTextArea = {
    val strs = fileName.split("\\|\\|\\|").toList
    val str2 = strs

    val height = 16
    val str = str2.mkString("")
    val textArea = new InlineCssTextArea(str)
    textArea.setWrapText(true)
    textArea.setEditable(false)
    textArea.setStyle(0, str.length, s"-fx-font-size: ${height}px;")
    (textArea: Region).prefHeight = height + 10
    textArea.background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))
    str2.map(_.length).zipWithIndex.foldLeft(0) {
      case (start, (len, index)) =>
        if (index % 2 == 1) {
          textArea.setStyle(start, start + len, s"-fx-fill: red; -fx-font-size: ${height}px;")
        }
        start + len
    }
    textArea
    /*new TextFlow {
      children = textArea: Node
    }*/
  }

  def contentFlow: InlineCssTextArea = {
    val strs = content.split("\\|\\|\\|").toList
    val str2 = strs
    val textArea = new InlineCssTextArea(str2.mkString(""))
    textArea.setWrapText(true)
    textArea.setEditable(false)
    str2.map(_.length).zipWithIndex.foldLeft(0) {
      case (start, (len, index)) =>
        if (index % 2 == 1) {
          textArea.setStyle(start, start + len, "-fx-fill: red;")
        }
        start + len
    }
    textArea
    /*new TextFlow {
      children = textArea: Node
    }*/
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
        (Desktop.getDesktop: java.awt.Desktop).browse(new File(filePath).getParentFile.toURI)
        ()
    }
  }

}