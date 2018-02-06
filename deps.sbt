libraryDependencies += "net.coobird" % "thumbnailator" % "0.4.8"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies ++= Dependencies.lucence

val poiVersion = "3.17"
libraryDependencies ++= Seq(
  //poi
  "org.apache.poi" % "poi",
  "org.apache.poi" % "poi-ooxml",
  //"org.apache.poi" % "poi-ooxml-schemas",
  "org.apache.poi" % "poi-scratchpad"
)
.map(_ % poiVersion exclude("stax", "stax-api") exclude("org.apache.poi", "poi-ooxml-schemas"))

libraryDependencies ++= Seq(
  //joda-time
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.9.2",
  "org.apache.poi" % "ooxml-schemas" % "1.3"
)

val slickVersion = "3.2.1"
libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion exclude("com.zaxxer", "HikariCP-java6")

libraryDependencies ++= Dependencies.openhtmltopdf

libraryDependencies += "org.fxmisc.richtext" % "richtextfx" % "0.8.1"

val macwire = Seq(
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided",
  "com.softwaremill.macwire" %% "macrosakka" % "2.3.0" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.3.0"/*,
  "com.softwaremill.macwire" %% "proxy" % "2.3.0"*/
)

libraryDependencies ++= macwire

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21"
libraryDependencies += "com.h2database" % "h2" % "1.4.196"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.9"

resolvers ++= Seq("restlet" at "http://maven.restlet.org")