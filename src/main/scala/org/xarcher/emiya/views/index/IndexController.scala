package org.xarcher.emiya.views.index

import java.io.File
import java.net.URI
import java.nio.file.Paths

import org.xarcher.emiya.service.ContentService
import org.xarcher.emiya.views.search.DoSearch
import org.xarcher.xPhoto.{ FileDB, FileIndex, FileTables }

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
      Future.sequence(fileListWrapper.FileList.currentItems.map(item => fileIndex.index(item))): Future[List[Int]]
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

class FileListWrapper(contentService: ContentService, fileDB: FileDB)(implicit ec: ExecutionContext) {
  object FileList extends ListView[FileTables.IndexContentRow] {
    selectionModel.value.selectionMode = SelectionMode.Multiple
    //items = ObservableBuffer(List.empty[FileTables.IndexContentRow])
    def currentItems: List[FileTables.IndexContentRow] = selectionModel.value.selectedItems.toList
    /*selectionModel.value.selectedItems.onChange {
      (a, b) =>
        doSearch.commonSearch
    }*/
    cellFactory = { _ => new CellFactory() }

    class CellFactory extends javafx.scene.control.ListCell[FileTables.IndexContentRow] {
      override protected def updateItem(item: FileTables.IndexContentRow, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        setItem(item)
        if ((!empty) && (item != null)) {
          val path = Paths.get(URI.create(item.rootUri))
          val name = Option(path.getFileName).map(_.toString).getOrElse(path.toRealPath().toString)
          setGraphic(new Text(name) {
            /*tooltip = new Tooltip(path.toRealPath().toString) {
              font = Font(14)
            }*/
          }: Node)
        } else {
          setGraphic(null)
          setText(null)
        }
      }
    }

  }

  def refreshAll: Future[Boolean] = {
    import FileTables._
    import FileTables.profile.api._
    fileDB.db.run(IndexContent.result).map { contents =>
      Platform.runLater { () =>
        FileList.items = ObservableBuffer(contents.toList)
      }
      true
    }
  }

  def removeSelected: Future[Boolean] = {
    import FileTables._
    import FileTables.profile.api._
    val ids = FileList.currentItems.map(_.id)
    for {
      (_: Int) <- fileDB.db.run {
        (IndexPath.filter(_.contentId inSetBind ids).delete >>
          IndexContent.filter(s => s.id inSetBind ids).delete).transactionally
      }
    } yield {
      true
    }
  }

  refreshAll

}