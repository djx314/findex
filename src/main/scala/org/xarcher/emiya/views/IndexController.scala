package org.xarcher.emiya.views

import java.io.File

import org.xarcher.xPhoto.FileIndex

import scala.concurrent.ExecutionContext
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.event.{ ActionEvent, Event, EventIncludes, EventType }
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.stage.{ DirectoryChooser, Stage }

class IndexController(fileSelectButton: FileSelectButton, startIndexButton: StartIndexButton, fileList: FileList)(implicit ec: ExecutionContext) extends VBox {
  style = "-fx-alignment: center;"
  children = List(
    fileSelectButton,
    startIndexButton,
    fileList)
}

class SelectedFile() {
  var indexDirOpt: Option[File] = Option.empty
}

class FileSelectButton(stage: Stage, selectedFile: SelectedFile) extends Button("文件选择") {

  handleEvent(MouseEvent.MouseClicked) {
    event: MouseEvent =>
      val file = new DirectoryChooser {
        selectedFile.indexDirOpt.map(f => initialDirectory = f)
        title = "选择索引文件"
      }.showDialog(stage)
      selectedFile.indexDirOpt = Option(file)
      ()
  }

}

class StartIndexButton(selectedFile: SelectedFile)(implicit ec: ExecutionContext) extends Button("开始索引") {

  handleEvent(MouseEvent.MouseClicked) {
    event: MouseEvent =>
      selectedFile.indexDirOpt.map(file =>
        FileIndex.index(file.toPath).map { (_: Int) =>
          new Alert(Alert.AlertType.Information) {
            contentText = selectedFile.indexDirOpt.map(_ => "索引完成").getOrElse("没有文件")
          }.showAndWait()
        }).getOrElse {
        new Alert(Alert.AlertType.Information) {
          contentText = selectedFile.indexDirOpt.map(_.toPath.toRealPath().toString).getOrElse("没有文件")
        }.showAndWait()
      }
      ()
  }

}

case class ListDirModel(name: String, filePath: String)

class FileList() extends ListView[ListDirModel] {

  items = ObservableBuffer(List(
    ListDirModel("11", "2222"),
    ListDirModel("aa", "bbbb")))

  selectionModel.value.setSelectionMode(SelectionMode.Multiple)

  (selectionModel.value.getSelectedItems: ObservableBuffer[ListDirModel]).onChange {
    (a, b) =>
      println(a.toString)
      println(b.toString)
  }
  /*type LEditEvent[T] = javafx.scene.control.ListView.EditEvent[T]

  handleEvent(javafx.scene.control.ListView.editAnyEvent[ListDirModel]: EventType[LEditEvent[ListDirModel]]) {
    { e: ListView.EditEvent[ListDirModel] =>
      println("1111")
      ()
    }
  }*/
  cellFactory = { _ => new Aa() }

  class Aa extends javafx.scene.control.ListCell[ListDirModel] {
    override protected def updateItem(item: ListDirModel, empty: Boolean): Unit = {
      super.updateItem(item, empty)
      if ((!empty) && (item != null)) {
        setGraphic(new Text(item.name) {
        }: Node)
      }
    }
  }

}