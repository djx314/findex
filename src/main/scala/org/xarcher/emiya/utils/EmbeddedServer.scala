package org.xarcher.emiya.utils

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.embedded.LocalNode
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
import com.sksamuel.elastic4s.http.{ HttpClient, RequestFailure, RequestSuccess }
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.xarcher.xPhoto.IndexExecutionContext

import scala.util.{ Failure, Success }

// we must import the dsl

import scala.concurrent.Future

class EmbeddedServer(shutdownHook: ShutdownHook, exContextWrap: IndexExecutionContext) {

  /*val pathStr = "./ext_persistence_不索引/file_index_solr_db"
  val path = Paths.get(pathStr)
  Files.createDirectories(path)

  val solrXmlPath = path.resolve("solr.xml")
  Files.copy(getClass.getResource("/solr-assets/solr.xml").openStream, solrXmlPath, StandardCopyOption.REPLACE_EXISTING)

  val fileIndexRepo = path.resolve("file_index_db").resolve("file_index")
  Files.createDirectories(fileIndexRepo)
  Files.copy(getClass.getResource("/solr-assets/core.properties").openStream, fileIndexRepo.resolve("core.properties"), StandardCopyOption.REPLACE_EXISTING)

  val fileIndexConf = fileIndexRepo.resolve("conf")
  Files.createDirectories(fileIndexConf)
  Files.copy(getClass.getResource("/solr-assets/schema.xml").openStream, fileIndexConf.resolve("schema.xml"), StandardCopyOption.REPLACE_EXISTING)
  Files.copy(getClass.getResource("/solr-assets/solrconfig.xml").openStream, fileIndexConf.resolve("solrconfig.xml"), StandardCopyOption.REPLACE_EXISTING)*/

  /*val solrServer = {
    val container = CoreContainer.createAndLoad(path)
    val embbed = new EmbeddedSolrServer(container, "file_index")
    shutdownHook.addHook(() => Future.successful(embbed.close()))
    embbed
  }*/

  implicit protected val ec = exContextWrap.indexEc

  val index: Index = "findex0303"
  val typeName: String = "file_content"

  //org.apache.logging.log4j.core.Logger
  println("1414" * 300)

  protected lazy val initEs: Future[HttpClient] = {
    println("1212" * 100)
    println(ec)

    val localNode = LocalNode("findex", "./esTmp/tmpDataPath")
    //shutdownHook.addHook(() => Future.successful(localNode.close()))
    val client = localNode.http(true)
    Future.successful {
      client
    }
  }

  lazy val esLocalClient: Future[HttpClient] = {
    createIndexInitAction.flatMap {
      _: Either[RequestFailure, RequestSuccess[CreateIndexResponse]] =>
        initEs
    }
  }

  /*
  val doc = new SolrInputDocument()
                    doc.addField("id", f.id)
                    doc.addField("file_name", info.fileName)
                    doc.addField("file_content", info.content)
                    doc.addField("file_path", info.filePath)
                    doc.addField("law_file_name", info.fileName)
                    doc.addField("content_id", content.id)

                    info.content.grouped(32766 / 3 - 200).zipWithIndex.foldLeft("") {
                      case (prefix, (content, index)) =>
                        doc.addField("law_file_content_" + index, prefix + content)
                        content.takeRight(20)
                    }
                    doc.addField("law_file_path", info.filePath)
                    embeddedServer.solrServer.add("file_index", doc)
   */

  protected lazy val createIndexInitAction = {
    initEs.flatMap {
      client =>
        println("55" * 100)
        println(client)
        client.execute {
          createIndex(index.name).mappings(
            mapping(typeName)
              .fields(
                keywordField("file_name"),
                textField("file_content"),
                keywordField("file_path"),
                keywordField("law_file_name"),
                intField("content_id"))
              .dynamic(DynamicMapping.Strict))
        }.andThen {
          case Success(s) =>
            println("11" * 100)
            println(s)
          case Failure(e) =>
            println("22" * 100)
            e.printStackTrace
        }
    }
  }

}