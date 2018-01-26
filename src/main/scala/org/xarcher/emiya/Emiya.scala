package org.xarcher.xPhoto

import scalafx.application.JFXApp
import scalafx.scene.Scene
import com.softwaremill.macwire._
import org.xarcher.emiya.views._
import org.xarcher.emiya.views.index._
import org.xarcher.emiya.views.search._

import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.scene.image.Image

object Emiya extends JFXApp {

  private lazy val selectedFile = wire[SelectedFile]
  private lazy val fileSelectButton = wire[FileSelectButton]
  private lazy val startIndexButton = wire[StartIndexButton]
  private lazy val indexController: IndexController = wire[IndexController]
  private lazy val FuzzySearchInput = wire[FuzzySearchInput]
  private lazy val ExactSearchInput = wire[ExactSearchInput]
  private lazy val DoSearch = wire[DoSearch]
  private lazy val resultContent = wire[ResultContent]
  private lazy val SearcherPane = wire[SearcherPane]
  private lazy val searchController: SearchController = wire[SearchController]
  private lazy val parentBox: ParentBox = wire[ParentBox]
  private lazy val fileList = wire[FileList]

  private lazy val cusScene = new Scene {
    content = parentBox
  }

  stage = new JFXApp.PrimaryStage {
    title.value = "喵喵酱的文件搜索"
    height = 700
    width = 900
    scene = cusScene
  }

  stage.getIcons.add(new Image(this.getClass.getResourceAsStream("/icon.png")))

  parentBox.prefHeight <== cusScene.height
  parentBox.prefWidth <== cusScene.width

}