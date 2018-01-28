package org.xarcher.emiya.views.index

import java.io.File

import org.xarcher.xPhoto.FileIndex

import scala.concurrent.ExecutionContext
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.text.{ Font, Text }
import scalafx.stage.{ DirectoryChooser, Stage }

class IndexController(fileSelectButton: FileSelectButton, startIndexButton: StartIndexButton, fileList: FileList)(implicit ec: ExecutionContext) extends BorderPane {
  top = new HBox {
    children = List(fileSelectButton, startIndexButton)
  }
  center = fileList
}

class SelectedFile() {
  var indexDirOpt: Option[File] = Option.empty
}

class FileSelectButton(stage: Stage, selectedFile: SelectedFile) extends Button("", new ImageView(getClass.getResource("/add.png").toURI.toASCIIString)) {

  tooltip = new Tooltip("增加索引目录") {
    font = Font(14)
  }

  onAction = {
    event: ActionEvent =>
      val file = new DirectoryChooser {
        selectedFile.indexDirOpt.map(f => initialDirectory = f)
        title = "选择索引文件"
      }.showDialog(stage)
      selectedFile.indexDirOpt = Option(file)
      ()
  }

}

class StartIndexButton(selectedFile: SelectedFile, fileIndex: FileIndex)(implicit ec: ExecutionContext) extends Button("", new ImageView(getClass.getResource("/arrow_right.png").toURI.toASCIIString)) {

  tooltip = new Tooltip("开始索引选中目录") {
    font = Font(14)
  }

  onAction = {
    event: ActionEvent =>
      selectedFile.indexDirOpt.map(file =>
        fileIndex.index(file.toPath).map { (_: Int) =>
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