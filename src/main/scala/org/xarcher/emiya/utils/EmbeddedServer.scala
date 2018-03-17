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
import org.slf4j.LoggerFactory
import org.xarcher.xPhoto.IndexExecutionContext

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

// we must import the dsl

import scala.concurrent.Future

class EmbeddedServer(shutdownHook: ShutdownHook)(implicit executionContext: ExecutionContext) {

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

  val index: Index = "findex0505"
  val typeName: String = "file_content"

  val logger = LoggerFactory.getLogger(getClass)

  protected lazy val initEs: Future[HttpClient] = {
    Future {
      val localNode = LocalNode("findex0303", "./esTmp/tmpDataPath0303")
      shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          Try {
            logger.info("开始关闭 elasticSearch 服务端")
            localNode.close()
          } match {
            case Failure(e) => logger.error("关闭 elasticSearch 服务端遇到错误", e)
            case Success(_) => logger.info("关闭 elasticSearch 服务端成功")
          }
        }
      })
      val client = localNode.http(false)
      shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          Try {
            logger.info("开始关闭 elasticSearch 客户端")
            client.client.close()
            client.close()
          } match {
            case Failure(e) => logger.error("关闭 elasticSearch 客户端遇到错误", e)
            case Success(_) => logger.info("关闭 elasticSearch 客户端成功")
          }
        }
      })
      client
    }.andThen {
      case Failure(e) =>
        logger.error("创建 elasticSearch 实例遇到错误", e)
      case Success(_) =>
        logger.info("创建 elasticSearch 实例成功")
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
    initEs
      .flatMap {
        client =>
          client.execute {
            createIndex(index.name).mappings(
              mapping(typeName)
                .fields(
                  intField("db_id"),
                  keywordField("file_name"),
                  textField("file_content"),
                  keywordField("file_path"),
                  keywordField("law_file_name"),
                  intField("content_id"))
                .dynamic(DynamicMapping.Strict))
          }
      }
  }

}