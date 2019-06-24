import sbt._
import sbt.Keys._

object Dependencies {
  val elastic4sVersion = "7.0.2"
  val elastic4s = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % elastic4sVersion,
    "org.slf4j" % "log4j-over-slf4j" % "1.7.25",
    "pl.allegro.tech" % "embedded-elasticsearch" % "2.7.0"
  )

  libraryDependencies += "org.openjfx" % "javafx" % "13-ea+9"

  val openhtmlVersion = "0.0.1-RC20"
  val openhtmltopdf = Seq(
    "com.openhtmltopdf" % "openhtmltopdf-core" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-pdfbox" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-java2d" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-rtl-support" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-jsoup-dom-converter" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-slf4j" % openhtmlVersion,
    "com.openhtmltopdf" % "openhtmltopdf-svg-support" % openhtmlVersion
  ).map(
    s =>
      s.exclude("xml-apis", "xml-apis")
        .exclude("commons-logging", "commons-logging"))

}
