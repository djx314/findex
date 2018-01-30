package org.xarcher.xPhoto

import akka.actor.{ ActorRef, ActorSystem, Terminated }

import scalafx.application.JFXApp
import scalafx.scene.Scene
import com.softwaremill.macwire._
import org.xarcher.emiya.views._
import org.xarcher.emiya.views.index._
import org.xarcher.emiya.views.search._

import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.scene.image.Image
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._
import org.xarcher.emiya.utils._

import scala.concurrent.ExecutionContext
import scalafx.stage.WindowEvent
import scalafx.Includes._

object Emiya extends JFXApp {

  private lazy val system = ActorSystem("miao-system")
  private lazy val fileTables = wire[FileTables]
  private lazy val fileIndex = wire[FileIndex]
  private lazy val shutdownHook = wire[ShutdownHook]

  shutdownHook.addHook(() => system.terminate().map((_: Terminated) => ()))

  private def limitedActor: ActorRef @@ LimitedActor =
    wireAnonymousActor[LimitedActor].taggedWith[LimitedActor]

  private def timeLimitedActor: ActorRef @@ TimeLimitedActor =
    wireAnonymousActor[TimeLimitedActor].taggedWith[TimeLimitedActor]

  private def futureLimitedGen = wire[FutureLimitedGen]
  private def futureTimeLimitedGen = wire[FutureTimeLimitedGen]

  private lazy val IndexExecutionContext = wire[IndexExecutionContext]
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

  stage.onCloseRequest = {
    e: WindowEvent =>
      shutdownHook.exec()
      ()
  }

  parentBox.prefHeight <== cusScene.height
  parentBox.prefWidth <== cusScene.width

}