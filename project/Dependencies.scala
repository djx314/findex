import sbt._
import sbt.Keys._

object Dependencies {

  val lucenceV = "7.2.1"
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
    "org.apache.lucene" % "lucene-highlighter" % lucenceV
  )

  val openhtmlVersion = "0.0.1-RC11"
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