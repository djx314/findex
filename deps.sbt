libraryDependencies += "net.coobird" % "thumbnailator" % "0.4.8"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies ++= Dependencies.lucence

val poiVersion = "3.17"

libraryDependencies ++= Seq(
  //poi
  "org.apache.poi" % "poi",
  "org.apache.poi" % "poi-ooxml",
  "org.apache.poi" % "poi-ooxml-schemas"
)
.map(_ % poiVersion exclude("stax", "stax-api")) ++:
Seq(
  //joda-time
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.9.2"
)