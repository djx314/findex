package org.xarcher.emiya.utils

import java.nio.file.{ Files, Path, Paths }

import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.core.CoreContainer
import org.apache.solr.schema.LongPointField
import org.xarcher.xPhoto.FileTables._

import scala.collection.immutable.Queue
import scala.concurrent.Future

class EmbeddedServer(shutdownHook: ShutdownHook) {

  val pathStr = "./ext_persistence_不索引/lucenceTemp1111"
  val path = Paths.get(pathStr)
  Files.createDirectories(path)

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