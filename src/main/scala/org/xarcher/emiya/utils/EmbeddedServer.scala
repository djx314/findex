package org.xarcher.emiya.utils

import java.nio.file.{ Files, Path, Paths, StandardCopyOption }

import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.core.CoreContainer
import org.apache.solr.schema.LongPointField
import org.xarcher.xPhoto.FileTables._

import scala.collection.immutable.Queue
import scala.concurrent.Future

class EmbeddedServer(shutdownHook: ShutdownHook) {

  val pathStr = "./ext_persistence_不索引/file_index_solr_db"
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
  Files.copy(getClass.getResource("/solr-assets/solrconfig.xml").openStream, fileIndexConf.resolve("solrconfig.xml"), StandardCopyOption.REPLACE_EXISTING)

  val solrServer = {
    //System.setProperty("solr.solr.home", path)
    //val initializer = new CoreContainer()
    //val coreContainer = initializer.initialize()
    //val embbed = new EmbeddedSolrServer(initializer, "miao-miao-jiang")
    val container = CoreContainer.createAndLoad(path)
    //container.load()
    //val aa: cjkTokenizerFactory = ???
    val embbed = new EmbeddedSolrServer(container, "file_index")
    shutdownHook.addHook(() => Future.successful(embbed.close()))
    embbed
  }

}