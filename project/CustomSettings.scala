import sbt._
import sbt.Keys._

object CustomSettings {
  
  def customSettings = scalaSettings ++ resolversSettings ++ extAlias ++ assemblyPluginSettings ++ nativePackageSettings
  def commonProjectSettings = scalaSettings ++ resolversSettings ++ extAlias
  
  def scalaSettings =
    Seq(
      scalaVersion := "2.12.4",
      scalacOptions ++= Seq("-feature", "-deprecation")
    )
  
  def resolversSettings =
    Seq(
      resolvers ++= Seq(
        "mavenRepoJX" at "http://repo1.maven.org/maven2/",
        "bintray/non" at "http://dl.bintray.com/non/maven",
        Resolver.sonatypeRepo("release"),
        Resolver.url("typesafe-ivy", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
      )//,
      //externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)
    )
  
  def extAliasInfo = List(
    Option("xeclipse" -> "eclipse with-source=true skip-parents=false")/*,
    if (OSName.isWindows)
      Option(windowsGitInitCommandMap)
    else if (OSName.isLinux)
      Option(linuxGitInitCommandMap)
    else None*/
  )

  def extAlias = extAliasInfo.collect { case Some(s) => s }
    .foldLeft(List.empty[Def.Setting[_]]){ (s, t) => s ++ addCommandAlias(t._1, t._2) }
  
  //git init command
  /*val windowsGitInitCommandMap = "windowsGitInit" ->
    """|;
        |git config --global i18n.commitencoding utf-8;
        |git config --global i18n.logoutputencoding gbk;
        |git config --global core.autocrlf true;
        |git config core.editor \"extras/npp.6.5.1/startNote.bat\"
      """.stripMargin

  val linuxGitInitCommandMap = "linuxGitInit" ->
    """|;
        |git config --global i18n.commitencoding utf-8;
        |git config --global i18n.logoutputencoding utf-8;
        |git config --global core.autocrlf true;
        |git config core.editor gedit
      """.stripMargin*/

  val assemblyPluginSettings = {

    import sbtassembly.AssemblyKeys._
    import sbtassembly.{MergeStrategy, PathList}

    sbtassembly.AssemblyPlugin.assemblySettings.++(
      Seq(
        mainClass in assembly := Some("org.xarcher.xPhoto.Emiya")/*,
        assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { old => {
          case "reference.conf" => MergeStrategy.concat
          case PathList("play", "reference-overrides.conf") => MergeStrategy.concat
          case PathList("META-INF", "spring.tooling") => MergeStrategy.discard
          case x => old(x)
        } }*/
      )
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