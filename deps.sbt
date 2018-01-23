libraryDependencies += "net.coobird" % "thumbnailator" % "0.4.8"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies ++= Dependencies.lucence

val slickVersion = "3.2.1"

libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion

libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion exclude("com.zaxxer", "HikariCP-java6")