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


}