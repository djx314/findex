package org.xarcher.xPhoto

import akka.actor.{ ActorRef, ActorSystem, Terminated }

import scalafx.application.JFXApp
import scalafx.scene.Scene
import com.softwaremill.macwire._
import org.xarcher.emiya.views._
import org.xarcher.emiya.views.index._
import org.xarcher.emiya.views.search._

import scalafx.scene.image.Image
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._
import org.xarcher.emiya.service.{ ContentService, FileIgnoreService }
import org.xarcher.emiya.utils._

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }
import scalafx.stage.WindowEvent
import scalafx.Includes._
import scalafx.scene.input.MouseEvent

object Emiya extends JFXApp {

  private lazy val system = ActorSystem("miao-system")
  private lazy val fileTables = wire[FileDB]
  private lazy val fileIndex = wire[FileIndex]
  private lazy val shutdownHook = wire[ShutdownHook]

  shutdownHook.addHook(new Thread() { override def run: Unit = { system.terminate().map((_: Terminated) => ())(indexExecutionContext.indexEc) } })

  private def limitedActor: ActorRef @@ LimitedActor =
    wireAnonymousActor[LimitedActor].taggedWith[LimitedActor]

  private def timeLimitedActor: ActorRef @@ TimeLimitedActor =
    wireAnonymousActor[TimeLimitedActor].taggedWith[TimeLimitedActor]

  private def futureLimitedGen: () => FutureLimitedGen = { () =>
    wire[FutureLimitedGen]
  }
  private def futureTimeLimitedGen: () => FutureTimeLimitedGen = { () =>
    wire[FutureTimeLimitedGen]
  }

  private lazy val fileExtraction = wire[FileExtraction]
  private lazy val fileIgnoreService = wire[FileIgnoreService]
  private lazy val contentService = wire[ContentService]
  private lazy val embeddedServer = {
    val a: EmbeddedServer = wire[EmbeddedServer]
    a.esLocalClient
    a
  }

  private lazy val fileSearch = wire[FileSearch]

  private lazy val indexExecutionContext: IndexExecutionContext = wire[IndexExecutionContext]
  implicit lazy val ec = indexExecutionContext.indexEc
  private lazy val selectedFile = wire[SelectedFile]
  private lazy val fileSelectButton = wire[FileSelectButton]
  private lazy val startIndexButton = wire[StartIndexButton]
  private lazy val removeIndexButton = wire[RemoveIndexButton]
  private lazy val indexController: IndexController = wire[IndexController]
  private lazy val fuzzySearchInput = wire[FuzzySearchInput]
  private lazy val exactSearchInput = wire[ExactSearchInput]
  private lazy val doSearch = wire[DoSearch]
  private lazy val fileUpdate = wire[FileUpdate]
  private lazy val compareGen = wire[CompareGen]
  private lazy val resultTabPane = wire[ResultTabPane]
  private lazy val SearcherPane = wire[SearcherPane]
  private lazy val searchController: SearchController = wire[SearchController]
  private lazy val parentBox: ParentBox = wire[ParentBox]
  private lazy val fileListWrapper = wire[FileListWrapper]

  private lazy val cusScene = new Scene {
    content = parentBox
  }

  stage = new JFXApp.PrimaryStage {
    title.value = "喵喵酱的文件搜索"
    height = 700
    width = 900
    scene = cusScene

    fileListWrapper.FileList.selectionModel.value.selectedItems.onChange {
      (obsList, changes) =>
        //必须先声明一下，否则报错
        obsList.toList
        doSearch.search(
          fuzzySearchInput.text.value,
          exactSearchInput.text.value,
          obsList.toList)
        ()
    }
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