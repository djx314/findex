import sbt._
import sbt.Keys._

val printlnDo = println("""
|                                                     _   _         _
|                                                    | | (_)       | |
|  ___   _ __    _   _   _ __ ___     __ _      ___  | |  _   ___  | |__
| / _ \ | '_ \  | | | | | '_ ` _ \   / _` |    / _ \ | | | | / __| | '_ \
||  __/ | | | | | |_| | | | | | | | | (_| |   |  __/ | | | | \__ \ | | | |
| \___| |_| |_|  \__,_| |_| |_| |_|  \__,_|    \___| |_| |_| |___/ |_| |_|
""".stripMargin
)

lazy val findex = (project in file("."))

fork := true

CustomSettings.scalaSettings
CustomSettings.resolversSettings
CustomSettings.assemblyPluginSettings
CustomSettings.nativePackageSettings

enablePlugins(JDKPackagerPlugin)

enablePlugins(WindowsPlugin)

javaOptions in run += "-Xmx800M"