package org.xarcher.emiya.utils

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.embedded.LocalNode
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.{ CreateIndexResponse, IndexResponse }
import com.sksamuel.elastic4s.http.{ ElasticClient, RequestFailure, RequestSuccess, Response }
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.slf4j.LoggerFactory

import scala.concurrent.{ Await, ExecutionContext }
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

  val index: Index = "findex0909"
  val typeName: String = "file_content"

  val logger = LoggerFactory.getLogger(getClass)

  protected lazy val initEs: Future[ElasticClient] = {
    Future {
      val localNode = LocalNode("findex0404", "./esTmp/tmpDataPath0303")
      /*shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          Try {
            logger.info("开始关闭 elasticSearch 服务端")
            localNode.close()
          } match {
            case Failure(e) => logger.error("关闭 elasticSearch 服务端遇到错误", e)
            case Success(_) => logger.info("关闭 elasticSearch 服务端成功")
          }
        }
      })*/
      val client = localNode.client(true)
      /*shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          Try {
            logger.info("开始关闭 elasticSearch 客户端")
            client.client.close()
            //client.close()
          } match {
            case Failure(e) => logger.error("关闭 elasticSearch 客户端遇到错误", e)
            case Success(_) => logger.info("关闭 elasticSearch 客户端成功")
          }
        }
      })*/
      client
    }.andThen {
      case Failure(e) =>
        logger.error("创建 elasticSearch 实例遇到错误", e)
      case Success(_) =>
        logger.info("创建 elasticSearch 实例成功")
    }
  }

  val esLocalClient: Future[ElasticClient] = {
    createIndexInitAction.flatMap {
      _: Response[CreateIndexResponse] =>
        initEs
    }
  }

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
                  intField("content_id"),
                  objectField("law_body").dynamic(true),
                  keywordField("search_law_body"))
                .dynamic(DynamicMapping.Strict)
                .dynamicTemplates(dynamicTemplate("law_body_dyn").mapping(dynamicKeywordField().copyTo("search_law_body")).pathMatch("law_body.law_body_*")))
          }
      }
  }

}