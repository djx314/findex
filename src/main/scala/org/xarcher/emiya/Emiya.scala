package org.xarcher.xPhoto

import scalafx.application.JFXApp
import scalafx.scene.Scene
import com.softwaremill.macwire._
import org.xarcher.emiya.views._

object Emiya extends JFXApp {

  private lazy val FileSelectButton = wire[FileSelectButton]
  private lazy val StartIndexButton = wire[StartIndexButton]
  private lazy val IndexController: IndexController = wire[IndexController]
  private lazy val SearchInput = wire[SearchInput]
  private lazy val SearchContent = wire[SearchContent]
  private lazy val SearcherButton = wire[SearcherButton]
  private lazy val SearchController: SearchController = wire[SearchController]
  private lazy val ParentBox: ParentBox = wire[ParentBox]

  private lazy val CusScene = new Scene {
    content = ParentBox
  }

  stage = new JFXApp.PrimaryStage {
    title.value = "喵喵酱的文件搜索"
    height = 600
    width = 600
    scene = CusScene
  }

  ParentBox.prefHeight <== CusScene.height
  ParentBox.prefWidth <== CusScene.width

}