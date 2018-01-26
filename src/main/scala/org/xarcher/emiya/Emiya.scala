package org.xarcher.xPhoto

import scalafx.application.JFXApp
import scalafx.scene.Scene
import com.softwaremill.macwire._
import org.xarcher.emiya.views._
import scala.concurrent.ExecutionContext.Implicits.global

object Emiya extends JFXApp {

  private lazy val selectedFile = wire[SelectedFile]
  private lazy val fileSelectButton = wire[FileSelectButton]
  private lazy val startIndexButton = wire[StartIndexButton]
  private lazy val indexController: IndexController = wire[IndexController]
  private lazy val searchInput = wire[SearchInput]
  private lazy val searchContent = wire[SearchContent]
  private lazy val SearcherPane = wire[SearcherPane]
  private lazy val searchController: SearchController = wire[SearchController]
  private lazy val parentBox: ParentBox = wire[ParentBox]
  private lazy val fileList = wire[FileList]

  private lazy val cusScene = new Scene {
    content = parentBox
  }

  stage = new JFXApp.PrimaryStage {
    title.value = "喵喵酱的文件搜索"
    height = 600
    width = 600
    scene = cusScene
  }

  parentBox.prefHeight <== cusScene.height
  parentBox.prefWidth <== cusScene.width

}