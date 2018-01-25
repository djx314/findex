package org.xarcher.emiya.views

import java.io.File

import org.xarcher.xPhoto.FileIndex

import scala.concurrent.ExecutionContext
import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.stage.{ DirectoryChooser, Stage }

class IndexController(fileSelectButton: FileSelectButton, startIndexButton: StartIndexButton)(implicit ec: ExecutionContext) extends VBox {
  style = "-fx-alignment: center;"
  children = List(
    fileSelectButton,
    startIndexButton)
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