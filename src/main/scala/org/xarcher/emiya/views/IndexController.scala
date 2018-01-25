package org.xarcher.emiya.views

import java.io.File
import org.xarcher.xPhoto.FileIndex

import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.stage.{ DirectoryChooser, Stage }
import scala.concurrent.ExecutionContext.Implicits.global

class IndexController(fileSelectButton: FileSelectButton, startIndexButton: StartIndexButton) extends HBox {
  style = "-fx-alignment: center;"
  children = List(
    fileSelectButton,
    startIndexButton)
}

object SelectedFile {
  var indexDirOpt: Option[File] = Option.empty
}

class FileSelectButton(stage: Stage) extends Button("文件选择") {

  handleEvent(MouseEvent.MouseClicked) {
    event: MouseEvent =>
      val file = new DirectoryChooser {
        SelectedFile.indexDirOpt.map(f => initialDirectory = f)
        title = "选择索引文件"
      }.showDialog(stage)
      SelectedFile.indexDirOpt = Option(file)
      ()
  }

}

class StartIndexButton() extends Button("开始索引") {

  handleEvent(MouseEvent.MouseClicked) {
    event: MouseEvent =>
      SelectedFile.indexDirOpt.map(file =>
        FileIndex.index(file.toPath).map { (_: Int) =>
          new Alert(Alert.AlertType.Information) {
            contentText = SelectedFile.indexDirOpt.map(_ => "索引完成").getOrElse("没有文件")
          }.showAndWait()
        }).getOrElse {
        new Alert(Alert.AlertType.Information) {
          contentText = SelectedFile.indexDirOpt.map(_.toPath.toRealPath().toString).getOrElse("没有文件")
        }.showAndWait()
      }
      ()
  }

}