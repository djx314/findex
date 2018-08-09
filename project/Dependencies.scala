import sbt._
import sbt.Keys._

object Dependencies {
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
    "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion,
    "org.slf4j" % "log4j-over-slf4j" % "1.7.25"
  )/*.map(s =>
    s.exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
    .exclude("org.apache.logging.log4j", "log4j-api")
    .exclude("org.apache.logging.log4j", "log4j-core")
    .exclude("org.apache.logging.log4j", "log4j-1.2-api"))*/

  val openhtmlVersion = "0.0.1-RC12"
  val openhtmltopdf = Seq(
    "com.openhtmltopdf" % "openhtmltopdf-core" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-pdfbox" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-java2d" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-rtl-support" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-jsoup-dom-converter" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-slf4j" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-svg-support" % openhtmlVersion
  ).map(s =>
    s.exclude("xml-apis", "xml-apis")
    .exclude("commons-logging", "commons-logging")
  )

}