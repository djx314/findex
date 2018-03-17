package org.xarcher.emiya.utils

import java.io.{ File, FileInputStream }
import java.nio.file.Path
import java.util.concurrent.Executors

import akka.actor.ActorRef
import com.softwaremill.tagging.@@
import org.apache.commons.io.IOUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.xarcher.cpoi._
import org.xarcher.xPhoto.IndexExecutionContext

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

class FileExtraction(implicit executionContext: ExecutionContext) {

  object CPoi {

    def load(workbook: Workbook): Stream[Stream[Stream[CCell]]] = {

      val sheetWraps = for {
        i <- (0 until workbook.getNumberOfSheets).toStream
        sheet = workbook.getSheetAt(i) if (sheet != null)
      } yield for {
        k <- (0 to sheet.getLastRowNum).toStream
        row = sheet.getRow(k) if (row != null)
      } yield for {
        j <- (0 until row.getLastCellNum).toStream
        cell = row.getCell(j) if (cell != null)
      } yield {
        CCell(cell)
      }

      sheetWraps
    }

  }

  val logger = LoggerFactory.getLogger(getClass)

  val txtGen: Path => Future[Either[Throwable, String]] = { path =>
    Future {
      Try {
        IOUtils.toString(path.toUri.toURL, "utf-8")
      }.toEither
    }
  }

  val poiGen: Path => Future[Either[Throwable, String]] = { path =>
    Future {
      val workbook = Try {
        Try {
          new HSSFWorkbook(new FileInputStream(path.toFile))
        }.getOrElse(new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(path.toFile))))
      }
      workbook.map { wk =>
        object PoiOperations extends PoiOperations
        import PoiOperations._
        try {
          Right(CPoi.load(wk).map(_.map(_.map(_.tryValue[String]).collect { case Some(text) => text }.mkString("\t")).mkString("\n")).mkString("\n"))
        } catch {
          case e: Throwable =>
            logger.error(s"索引 Excel 文件失败，文件路径：${path.toRealPath().toString}", e)
            Left(e)
        }
      }.toEither.flatMap(identity)
    }
  }

  val htmlGen: Path => Future[Either[Throwable, String]] = { path =>
    Future {
      Try {
        Jsoup.parse(path.toFile, "utf-8").text()
      }.toEither
    }
  }

  val docPoiGen: Path => Future[Either[Throwable, String]] = path =>
    Future {
      Try {
        Try {
          import org.apache.poi.hwpf.extractor.WordExtractor
          import java.io.InputStream
          val is: InputStream = new FileInputStream(path.toFile)
          val ex: WordExtractor = new WordExtractor(is)
          val text2003 = ex.getText
          text2003
        }.getOrElse {
          import org.apache.poi.POIXMLDocument
          import org.apache.poi.xwpf.extractor.XWPFWordExtractor
          val opcPackage = POIXMLDocument.openPackage(path.toFile.getCanonicalPath)
          val extractor = new XWPFWordExtractor(opcPackage)
          val text2007 = extractor.getText
          text2007
        }
      }.toEither.left.map { e =>
        //e.printStackTrace
        e
      }
    }

  val indexer: Map[String, Path => Future[Either[Throwable, String]]] = Map(
    "txt" -> txtGen,
    "log" -> txtGen,
    "json" -> txtGen,
    "md" -> txtGen,
    "js" -> txtGen,
    "scala" -> txtGen,
    "java" -> txtGen,
    "php" -> txtGen,
    "css" -> txtGen,
    "conf" -> txtGen,
    "bat" -> txtGen,
    "htm" -> htmlGen,
    "html" -> htmlGen,
    "properties" -> txtGen,
    "xls" -> poiGen,
    "xlsx" -> poiGen,
    "et" -> poiGen,
    "doc" -> docPoiGen,
    "docx" -> docPoiGen,
    "wps" -> docPoiGen)

}