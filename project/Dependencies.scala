import sbt._
import sbt.Keys._

object Dependencies {
  /*val lucenceV = "7.2.1"

  val lucence = Seq(
    //核心包
    "org.apache.lucene" % "lucene-core" % lucenceV,
    //一般分词器，适用于英文分词
    "org.apache.lucene" % "lucene-analyzers-common" % lucenceV,
    //中文分词器
    "org.apache.lucene" % "lucene-analyzers-smartcn" % lucenceV,
    "org.apache.lucene" % "lucene-analyzers-common" % lucenceV,
    //对分词索引查询解析
    "org.apache.lucene" % "lucene-queryparser" % lucenceV,
    //检索关键字高亮显示
    "org.apache.lucene" % "lucene-highlighter" % lucenceV,
    "org.apache.solr" % "solr-core" % lucenceV,
    "org.apache.lucene" % "lucene-analyzers-icu" % lucenceV
  )*/
  val elastic4sVersion = "6.2.3"
  val elastic4s = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,

    // for the http client
    "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,

    // if you want to use reactive streams
    //"com.sksamuel.elastic4s" %% "elastic4s-streams" % elastic4sVersion,

    // testing
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test",
    "com.sksamuel.elastic4s" %% "elastic4s-circe" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion
  )

  val openhtmlVersion = "0.0.1-RC12"
  val openhtmltopdf = Seq(
    "com.openhtmltopdf" % "openhtmltopdf-core" % openhtmlVersion,
    ("com.openhtmltopdf" % "openhtmltopdf-pdfbox" % openhtmlVersion)
      .exclude("commons-logging", "commons-logging"),
    "com.openhtmltopdf" % "openhtmltopdf-java2d" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-rtl-support" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-jsoup-dom-converter" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-slf4j" % openhtmlVersion,
    ("com.openhtmltopdf" % "openhtmltopdf-svg-support" % openhtmlVersion)
      .exclude("xml-apis", "xml-apis")
      .exclude("commons-logging", "commons-logging")
  )

}