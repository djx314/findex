package org.xarcher.emiya.utils

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.indexes.CreateIndexResponse
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import org.slf4j.LoggerFactory
import pl.allegro.tech.embeddedelasticsearch.{EmbeddedElastic, IndexSettings, PopularProperties}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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

  val index: Index     = "findex0909"
  val typeName: String = "file_content"

  val logger = LoggerFactory.getLogger(getClass)

  protected lazy val initEs: Future[ElasticClient] = {
    Future {
      val embeddedElastic = EmbeddedElastic
        .builder()
        .withElasticVersion("5.0.0")
        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
        .withSetting(PopularProperties.CLUSTER_NAME, "my_cluster")
        .withPlugin("analysis-stempel")
        .withIndex("cars", IndexSettings.builder().withType("car", java.lang.ClassLoader.getSystemResourceAsStream("car-mapping.json")).build())
        .withIndex(
            "books"
          , IndexSettings
            .builder()
            .withType(
                "paper_book", //pl.allegro.tech.embeddedelasticsearch.SampleIndices.PAPER_BOOK_INDEX_TYPE,
              java.lang.ClassLoader.getSystemResourceAsStream("paper-book-mapping.json")
            )
            .withType("audio_book", java.lang.ClassLoader.getSystemResourceAsStream("audio-book-mapping.json"))
            .withSettings(java.lang.ClassLoader.getSystemResourceAsStream("elastic-settings.json"))
            .build()
        )
        .build()
        .start()

      ElasticClient(JavaClient(ElasticProperties(s"http://localhost:9350")))

    }.andThen {
      case Failure(e) =>
        logger.error("创建 elasticSearch 实例遇到错误", e)
      case Success(_) =>
        logger.info("创建 elasticSearch 实例成功")
    }
  }

  val esLocalClient: Future[ElasticClient] = {
    createIndexInitAction.flatMap { _: Response[CreateIndexResponse] =>
      initEs
    }
  }

  protected lazy val createIndexInitAction = {
    initEs.flatMap { client =>
      client.execute {
        createIndex(index.name).mappings(
          mapping(typeName)
            .fields(
                intField("db_id")
              , keywordField("file_name")
              , textField("file_content")
              , keywordField("file_path")
              , keywordField("law_file_name")
              , intField("content_id")
              , objectField("law_body").dynamic(true)
              , keywordField("search_law_body")
            )
            .dynamic(DynamicMapping.Strict)
            .dynamicTemplates(dynamicTemplate("law_body_dyn").mapping(dynamicKeywordField().copyTo("search_law_body")).pathMatch("law_body.law_body_*")))
      }
    }
  }

}
