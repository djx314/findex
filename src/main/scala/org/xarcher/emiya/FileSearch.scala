package org.xarcher.xPhoto

import java.awt.Desktop
import java.io.File
import java.nio.file.{ Files, Paths }

import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.index.{ DirectoryReader, IndexReader, Term }
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.{ BooleanQuery, IndexSearcher, TermQuery, WildcardQuery }
import org.apache.lucene.search.highlight.{ Highlighter, QueryScorer, SimpleFragmenter, SimpleHTMLFormatter }
import org.apache.lucene.store.FSDirectory
import org.fxmisc.richtext.InlineCssTextArea
import org.xarcher.xPhoto.FileTables.IndexContentRow

import scalafx.Includes._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{ Background, BackgroundFill, CornerRadii, Region }
import scalafx.scene.paint.Paint

object FileSearch {

  val path = "./ext_persistence_不索引/lucenceTemp"
  import FileTables.profile._

  def search(content: IndexContentRow, fuzzyKey: String, exactKey: String)(implicit ec: ExecutionContext): Future[List[OutputInfo]] = {
    var indexSearcher: IndexSearcher = null

    val analyzer = new CJKAnalyzer()
    val titleSize = 80
    val textSize = 300

    lazy val splitFronts = exactKey.split(' ').toList.map(_.trim).filterNot(_.isEmpty)

    def exactKeyQuery = {
      val queryList1 = splitFronts.map { split =>
        val eachTerm = new Term("fileName", s"*${split}*")
        new WildcardQuery(eachTerm)
      }
      val queryList2 = exactKey.split(' ').toList.map(_.trim).filterNot(_.isEmpty).map { split =>
        val eachTerm = new Term("filePath", s"*${split}*")
        new WildcardQuery(eachTerm)
      }
      val queryList3 = exactKey.split(' ').toList.map(_.trim).filterNot(_.isEmpty).map { split =>
        val eachTerm = new Term("law_fileContent", s"*${split}*")
        new WildcardQuery(eachTerm)
      }
      val booleanQueryBuilder = new BooleanQuery.Builder()
      (queryList1 ::: queryList2 ::: queryList3).map(query =>
        booleanQueryBuilder.add(query, Occur.SHOULD))
      booleanQueryBuilder.build()
    }

    def fuzzyKeyQuery = {
      val fields = Array("fileName", "fileContent")
      val mparser = new MultiFieldQueryParser(fields, analyzer)
      mparser.parse(fuzzyKey)
    }

    val f = Future {
      val directory = FSDirectory.open(Paths.get(path).resolve(content.id.toString))
      indexSearcher = new IndexSearcher(DirectoryReader.open(directory))

      if (fuzzyKey.isEmpty && exactKey.trim.isEmpty) {
        List.empty
      } else if ((!fuzzyKey.isEmpty) && exactKey.trim.isEmpty) {
        val fuzzyKeyQuery1 = fuzzyKeyQuery
        val docs = indexSearcher.search(fuzzyKeyQuery1, 20).scoreDocs.toList

        // 生成高亮器
        val formatter = new SimpleHTMLFormatter("|||", "|||")
        val scorer = new QueryScorer(fuzzyKeyQuery1)
        val highlighter = new Highlighter(formatter, scorer)
        highlighter.setTextFragmenter(new SimpleFragmenter(textSize))

        val titleHighlighter = new Highlighter(formatter, scorer)
        titleHighlighter.setTextFragmenter(new SimpleFragmenter(titleSize))

        docs.map { doc =>
          val hitDoc = indexSearcher.doc(doc.doc)
          val model = OutputInfo(
            fileName = Option(hitDoc.get("fileName")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            content = Option(hitDoc.get("law_fileContent")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            filePath = Option(hitDoc.get("filePath")).map(_.trim).filterNot(_.isEmpty).getOrElse(""))

          val newTitle = titleHighlighter.getBestFragment(analyzer.tokenStream("", model.fileName), model.fileName)
          val newContent = highlighter.getBestFragment(analyzer.tokenStream("", model.content), model.content)
          model.copy(fileName = Option(newTitle).map(_.trim).filterNot(_.isEmpty).getOrElse(""), content = newContent)
        }

      } else if (fuzzyKey.isEmpty && (!exactKey.trim.isEmpty)) {
        val exactKeyQuery1 = exactKeyQuery
        val docs = indexSearcher.search(exactKeyQuery1, 20).scoreDocs.toList
        docs.map { doc =>
          val hitDoc = indexSearcher.doc(doc.doc)
          val model = OutputInfo(
            fileName = Option(hitDoc.get("fileName")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            content = Option(hitDoc.get("law_fileContent")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            filePath = Option(hitDoc.get("filePath")).map(_.trim).filterNot(_.isEmpty).getOrElse(""))

          val newTitle = splitFronts.foldLeft(model.fileName.take(titleSize)) { (name, toReplace) =>
            name.replaceAllLiterally(toReplace, s"|||${toReplace}|||")
          }.take(titleSize)
          val newContent = splitFronts.foldLeft(model.content.take(textSize)) { (name, toReplace) =>
            name.replaceAllLiterally(toReplace, s"|||${toReplace}|||")
          }.take(textSize)
          model.copy(fileName = Option(newTitle).map(_.trim).filterNot(_.isEmpty).getOrElse(""), content = newContent)
        }
      } else {
        val fuzzyKeyQuery1 = fuzzyKeyQuery
        val exactKeyQuery1 = exactKeyQuery

        val booleanQueryBuilder = new BooleanQuery.Builder()
        booleanQueryBuilder.add(fuzzyKeyQuery1, Occur.MUST)
        booleanQueryBuilder.add(exactKeyQuery1, Occur.MUST)
        val booleanQuery = booleanQueryBuilder.build()
        val docs = indexSearcher.search(booleanQuery, 20).scoreDocs.toList

        // 生成高亮器
        val formatter = new SimpleHTMLFormatter("|||", "|||")
        val scorer = new QueryScorer(fuzzyKeyQuery1)
        val highlighter = new Highlighter(formatter, scorer)
        highlighter.setTextFragmenter(new SimpleFragmenter(textSize))

        val titleHighlighter = new Highlighter(formatter, scorer)
        titleHighlighter.setTextFragmenter(new SimpleFragmenter(titleSize))

        docs.map { doc =>
          val hitDoc = indexSearcher.doc(doc.doc)
          val model = OutputInfo(
            fileName = Option(hitDoc.get("fileName")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            content = Option(hitDoc.get("law_fileContent")).map(_.trim).filterNot(_.isEmpty).getOrElse(""),
            filePath = Option(hitDoc.get("filePath")).map(_.trim).filterNot(_.isEmpty).getOrElse(""))

          val newTitle = titleHighlighter.getBestFragment(analyzer.tokenStream("", model.fileName), model.fileName)
          val newContent = highlighter.getBestFragment(analyzer.tokenStream("", model.content), model.content)

          model.copy(fileName = Option(newTitle).map(_.trim).filterNot(_.isEmpty).getOrElse(""), content = newContent)
        }

      }
    }
    /*val f = if (fuzzyKey.isEmpty && exactKey.isEmpty)
      Future.successful(List.empty)
    else
      Future {
        val directory = FSDirectory.open(Paths.get(path).resolve(content.id.toString))
        indexSearcher = new IndexSearcher(DirectoryReader.open(directory))

        /*val fields = Array("fileName", "content")
        val mparser = new MultiFieldQueryParser(fields, analyzer)
        val mQuery123456789 = mparser.parse(fuzzyKey)*/
        //val b = new BooleanQuery.Builder()
        //b.add()
        //val docs = indexSearcher.search(b, 20).scoreDocs
        val docs = indexSearcher.search(mQuery, 20).scoreDocs
        println(docs)

        docs.map { doc =>
          val hitDoc = indexSearcher.doc(doc.doc)
          println(hitDoc.get("law_fileName"))
          println(hitDoc.get("fileName"))
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
      }*/
    f.andThen {
      case Success(list) =>
      //println(list)
      case Failure(e) =>
        e.printStackTrace
    }
  }

}

case class OutputInfo(filePath: String, fileName: String, content: String) {

  def fileNameFlow: InlineCssTextArea = {
    println(fileName)
    println(fileName.split("\\|\\|\\|"))
    println(fileName.split("\\|\\|\\|").toList)
    val strs = fileName.split("\\|\\|\\|").toList
    val str2 = strs

    val height = 16
    val str = str2.mkString("")
    val textArea = new InlineCssTextArea(str)
    textArea.setWrapText(true)
    textArea.setEditable(false)
    (textArea: Region).prefHeight = height + 4
    textArea.background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))

    textArea.setStyle(0, str.length, s"-fx-font-size: ${height}px;")
    str2.map(_.length).zipWithIndex.foldLeft(0) {
      case (start, (len, index)) =>
        if (index % 2 == 1) {
          textArea.setStyle(start, start + len, s"-fx-fill: red; -fx-font-size: ${height}px;")
        }
        start + len
    }

    textArea
  }

  def contentFlow: InlineCssTextArea = {
    val fontSize = 14
    val strs = content.split("\\|\\|\\|").toList
    val textArea = new InlineCssTextArea(strs.mkString(""))
    (textArea: Region).prefHeight = fontSize * 9
    textArea.setWrapText(true)
    textArea.setEditable(false)
    val strsLength = strs.map(_.length)
    val toatalLength = strsLength.sum

    textArea.setStyle(0, toatalLength, s"-fx-font-size: ${fontSize}px;")
    strsLength.zipWithIndex.foldLeft(0) {
      case (start, (len, index)) =>
        if (index % 2 == 1) {
          textArea.setStyle(start, start + len, s"-fx-fill: red; -fx-font-size: ${fontSize}px;")
        }
        start + len
    }

    textArea
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