package org.xarcher.xPhoto

import java.awt.Desktop
import java.io.File

import org.fxmisc.richtext.InlineCssTextArea
import org.slf4j.LoggerFactory
import org.xarcher.emiya.utils.EmbeddedServer
import org.xarcher.xPhoto.FileTables.IndexContentRow
import scalafx.Includes._

import scala.concurrent.{ExecutionContext, Future}
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{Background, BackgroundFill, CornerRadii, Region}
import scalafx.scene.paint.Paint
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.requests.searches.{SearchBodyBuilderFn, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.queries.BoolQuery

case class OuputWrap(info: List[OutputInfo], nextIndexOpt: Option[Int], countSum: Long)

class FileSearch(embeddedServer: EmbeddedServer) {

  val logger = LoggerFactory.getLogger(classOf[FileSearch])

  val path = "./ext_persistence_不索引/lucenceTemp"
  import FileTables.profile._

  def searchFromView(content: IndexContentRow, fuzzyKey: String, exactKey: String, start: Int, rows: Int)(implicit ec: ExecutionContext): Future[OuputWrap] = {
    lazy val exactSplitFronts = exactKey.trim.split(' ').toList.map(_.trim).filterNot(_.isEmpty)
    lazy val exactFilterString = exactSplitFronts.map { t =>
      s"*$t*"
    }

    val exactEmptyBoolean = List.empty[BoolQuery]
    val exactBool1 =
      if (exactFilterString.isEmpty)
        exactEmptyBoolean
      else
        boolQuery().should(exactFilterString.map(s => wildcardQuery("search_law_body", s))) :: exactEmptyBoolean

    embeddedServer.esLocalClient
      .flatMap { client =>
        client.execute {
          val searchDef = search(embeddedServer.index)
            .query(boolQuery().filter(matchQuery("content_id", content.id)).must(exactBool1))
            //TODO
            /*.types(embeddedServer.typeName)*/
            .start(start)
            .limit(rows)
          println(SearchBodyBuilderFn(searchDef).string())
          searchDef
        }
      }
      .map {
        case s: RequestFailure =>
          println(s)
          OuputWrap(List.empty, Option.empty, 0)
        case result: RequestSuccess[SearchResponse] =>
          println(result)
          val infoSize = result.result.size
          val nextIndexOpt =
            if (infoSize >= rows)
              Option(start + infoSize)
            else Option.empty

          val infos = result.result.to[IndexInfo]
          val extraInfos = infos.zipWithIndex.map {
            case (info, index) =>
              OutputInfo(searchIndex = index, filePath = info.filePath, fileName = info.fileName, content = info.fileContent, contentId = info.contentId)
          }
          OuputWrap(extraInfos.toList, Option.empty, 0)
      }

    /*val titleSize = 80
    val textSize = 300

    lazy val exactSplitFronts = exactKey.trim.split(' ').toList.map(_.trim).filterNot(_.isEmpty)
    lazy val exactFilterString = Option(exactSplitFronts).map { t => t.map(s => s"*$s*") }.filterNot(_.isEmpty)
    lazy val exactQueryString = exactFilterString.map {
      case head :: Nil =>
        head
      case s =>
        s.mkString("(", " AND ", ")")
    }
    lazy val exactQueryWithField = exactQueryString.map(s => s"law_file_sum:${s}")

    val fuzzyQueryWithField = Option(fuzzyKey).map(_.trim).filterNot(_.isEmpty).map(s => s"file_sum:${s}")

    val queryStrig = (fuzzyQueryWithField -> exactQueryWithField) match {
      case (Some(fuzzy), Some(exact)) =>
        s"(${fuzzy}) AND (${exact})"
      case (None, Some(exact)) =>
        exact
      case (Some(fuzzy), None) =>
        fuzzy
      case (None, None) =>
        "*:*"
    }

    val highlightSize = 400

    logger.info(s"执行了查询语句：${queryStrig}")
    val f = Future {
      val query = new SolrQuery()
      query.addFilterQuery(s"content_id:${content.id}")
      query.setQuery(queryStrig)
      query.addField("*")
      query.set("q.op", "OR")
      query.setStart(start)
      query.setRows(rows)
      query.setHighlightFragsize(highlightSize)
      query.addHighlightField("file_name")
      query.addHighlightField("file_content")
      query.setHighlightSimplePre("|||") //标记，高亮关键字前缀
      query.setHighlightSimplePost("|||") //后缀
      query.setHighlight(true)

      val queryResponse = embeddedServer.solrServer.query(query)

      //Storing the results of the query
      val resultDocs = queryResponse.getResults()
      val docs = resultDocs.asScala.toList
      val hightlighting = queryResponse.getHighlighting.asScala.mapValues(_.asScala.mapValues(_.asScala.toList))
      val infos = docs.zipWithIndex.map {
        case (doc, index) =>
          def fieldGen(field: String) = {
            val highOpt = Option(doc.getFieldValue("id")).map(_.asInstanceOf[String].trim).flatMap(s => hightlighting.get(s)).flatMap(s => s.get(field).flatMap(_.headOption))
            lazy val contentOpt = Option(doc.getFieldValue(field)).map(_.asInstanceOf[String].trim.take(highlightSize)).filterNot(_.isEmpty).getOrElse("")
            highOpt.getOrElse(contentOpt)
          }
          OutputInfo(
            searchIndex = start + index + 1,
            fileName = fieldGen("file_name"),
            content = fieldGen("file_content"),
            filePath = Option(doc.getFieldValue("file_path")).map(_.asInstanceOf[String].trim).filterNot(_.isEmpty).getOrElse(""),
            contentId = Option(doc.getFieldValue("content_id")).map(_.asInstanceOf[Int]).getOrElse(-1))
      }

      //Saving the operations
      embeddedServer.solrServer.commit()

      val infoSize = infos.size
      val nextIndexOpt = if (infoSize >= rows)
        Option(start + infoSize)
      else Option.empty

      OuputWrap(infos, nextIndexOpt, resultDocs.getNumFound)
    }
    f.andThen {
      case Success(list) =>
      //println(list)
      case Failure(e) =>
        e.printStackTrace
    }*/
    //Future.successful(OuputWrap(List.empty, Option.empty, 10))
  }

}

case class OutputInfo(searchIndex: Int, filePath: String, fileName: String, content: String, contentId: Int) {

  def fileNameFlow: InlineCssTextArea = {
    val strs = s"$searchIndex - $fileName".split("\\|\\|\\|").toList
    val str2 = strs

    val height   = 16
    val str      = str2.mkString("")
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
    val strs     = content.split("\\|\\|\\|").toList
    val textArea = new InlineCssTextArea(strs.mkString(""))
    (textArea: Region).prefHeight = fontSize * 9
    textArea.setWrapText(true)
    textArea.setEditable(false)
    val strsLength   = strs.map(_.length)
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
    handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
      Desktop.getDesktop.open(new File(filePath))
      ()
    }
  }

  def dirBtn: Button = new Button("打开文件夹") {
    handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
      (Desktop.getDesktop: java.awt.Desktop).browse(new File(filePath).getParentFile.toURI)
      ()
    }
  }

}
