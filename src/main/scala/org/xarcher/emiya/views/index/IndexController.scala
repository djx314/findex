package org.xarcher.emiya.views.index

import java.io.File
import java.net.URI
import java.nio.file.Paths

import org.xarcher.emiya.service.ContentService
import org.xarcher.xPhoto.{ FileIndex, FileTables }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.layout._
import scalafx.scene.text.{ Font, Text }
import scalafx.stage.{ DirectoryChooser, Stage }

class IndexController(
  fileSelectButton: FileSelectButton,
  startIndexButton: StartIndexButton,
  fileListWrapper: FileListWrapper,
  selectedFile: SelectedFile,
  removeIndexButton: RemoveIndexButton,
  fileIndex: FileIndex,
  stage: Stage,
  contentService: ContentService)(implicit ec: ExecutionContext) extends BorderPane {
  top = new HBox {
    children = List(fileSelectButton, startIndexButton, removeIndexButton)
  }
  center = fileListWrapper.FileList

  fileSelectButton.onAction = {
    event: ActionEvent =>
      val fileOpt = Option(
        new DirectoryChooser {
          selectedFile.indexDirOpt.map(f => initialDirectory = f)
          title = "选择索引文件"
        }.showDialog(stage))
      fileOpt.map { file =>
        contentService.create(file.toPath).flatMap((_: Boolean) => fileListWrapper.refreshAll).andThen {
          case Failure(e) =>
            e.printStackTrace
        }: Future[Boolean]
        selectedFile.indexDirOpt = Option(file)
      }
      ()
  }

  removeIndexButton.onAction = {
    event: ActionEvent =>
      fileListWrapper.removeSelected.flatMap((_: Boolean) => fileListWrapper.refreshAll): Future[Boolean]
  }

  startIndexButton.onAction = {
    event: ActionEvent =>
      /*selectedFile.indexDirOpt.map(file =>
        fileIndex.index(file.toPath).map { (_: Int) =>
          new Alert(Alert.AlertType.Information) {
            contentText = selectedFile.indexDirOpt.map(_ => "索引完成").getOrElse("没有文件")
          }.showAndWait()
        }).getOrElse {
        new Alert(Alert.AlertType.Information) {
          contentText = selectedFile.indexDirOpt.map(_.toPath.toRealPath().toString).getOrElse("没有文件")
        }.showAndWait()
      }*/
      Future.sequence(fileListWrapper.FileList.currentItems.map(item => fileIndex.index(Paths.get(URI.create(item.rootUri))))): Future[List[Int]]
      ()
  }
}

class SelectedFile() {
  var indexDirOpt: Option[File] = Option.empty
}

class FileSelectButton() extends Button("", new ImageView(getClass.getResource("/add.png").toURI.toASCIIString)) {

  tooltip = new Tooltip("增加索引目录") {
    font = Font(14)
  }

}

class StartIndexButton( /*selectedFile: SelectedFile, fileIndex: FileIndex*/ )(implicit ec: ExecutionContext) extends Button("", new ImageView(getClass.getResource("/arrow_right.png").toURI.toASCIIString)) {

  tooltip = new Tooltip("开始索引选中目录") {
    font = Font(14)
  }

}

class RemoveIndexButton(selectedFile: SelectedFile, fileIndex: FileIndex)(implicit ec: ExecutionContext) extends Button("", new ImageView(getClass.getResource("/arrow_right.png").toURI.toASCIIString)) {

  tooltip = new Tooltip("删除选中目录") {
    font = Font(14)
  }

}

class FileListWrapper(contentService: ContentService, val fileTables: FileTables)(implicit ec: ExecutionContext) {
  object FileList extends ListView[fileTables.IndexContentRow] {

    items = ObservableBuffer(List.empty[fileTables.IndexContentRow])

    selectionModel.value.setSelectionMode(SelectionMode.Multiple)

    def currentItems: List[fileTables.IndexContentRow] = selectionModel.value.selectedItems.toList

    selectionModel.value.selectedItems.onChange {
      (a, b) =>
        println(a.toString)
        println(b.toString)
    }

    cellFactory = { _ => new Aa() }

    class Aa extends javafx.scene.control.ListCell[fileTables.IndexContentRow] {
      override protected def updateItem(item: fileTables.IndexContentRow, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if ((!empty) && (item != null)) {
          val path = Paths.get(URI.create(item.rootUri))
          val name = Option(path.getFileName).map(_.toString).getOrElse(path.toRealPath().toString)
          setGraphic(new Text(name) {
          }: Node)
        } else {
          setGraphic(null)
        }
      }
    }

  }

  def refreshAll: Future[Boolean] = {
    import fileTables._
    import fileTables.profile.api._
    db.run(IndexContent.result).map { contents =>
      Platform.runLater { () =>
        FileList.items = ObservableBuffer(contents.toList)
      }
      true
    }
  }

  def removeSelected: Future[Boolean] = {
    import fileTables._
    import fileTables.profile.api._
    val ids = FileList.currentItems.map(_.id)
    db.run(IndexContent.filter(s => s.id inSet ids).delete).map { contents =>
      true
    }
  }

  refreshAll

}