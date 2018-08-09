import sbt._
import sbt.Keys._

object CustomSettings {
  
  def customSettings = scalaSettings ++ assemblyPluginSettings ++ nativePackageSettings
  def commonProjectSettings = scalaSettings
  
  def scalaSettings =
    Seq(
      scalaVersion := "2.12.6",
      scalacOptions ++= Seq("-feature", "-deprecation")
    )

  val assemblyPluginSettings = {

    import sbtassembly.AssemblyKeys._
    import sbtassembly.{MergeStrategy, PathList}

      Seq(
        mainClass in assembly := Some("org.xarcher.xPhoto.Emiya"),
        assemblyMergeStrategy in assembly := {
          val old = (assemblyMergeStrategy in assembly).value

          { path =>
            path match {
              case "reference.conf" => MergeStrategy.concat
              case PathList("play", "reference-overrides.conf") => MergeStrategy.concat
              case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
              case PathList("META-INF", "spring.tooling") => MergeStrategy.discard
              case x => old(x)
            }
          }
        }
      )

  }

  val nativePackageSettings = {

    import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
    import com.typesafe.sbt.packager.windows.WindowsPlugin.autoImport._
    import com.typesafe.sbt.SbtNativePackager.autoImport._
    import com.typesafe.sbt.packager.jdkpackager.JDKPackagerPlugin.autoImport._

    Seq(
      mappings in Windows := (mappings in Universal).value,
      // general package information (can be scoped to Windows)
      maintainer := "djx314",
      packageSummary := "xPhoto",
      packageDescription := """xPhoto.""",
      // wix build information
      wixProductId in Windows := "ce07be71-510d-414a-92d4-dff47631848a",
      wixProductUpgradeId in Windows := "4552fb0e-e257-4dbd-9ecb-dba9dbacf424",
      jdkPackagerType := "exe"
    )

  }

}