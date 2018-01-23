package org.xarcher.xPhoto

import java.io.File

import scala.util.Try
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button, CheckBox, Label}
import scalafx.scene.input.{DragEvent, MouseEvent, TransferMode}
import scalafx.scene.layout._
import scalafx.stage.{DirectoryChooser, FileChooser}

object Emiya extends JFXApp {

  object VarModel {

    trait AbsVar[T] {
      var model: T = _
      def setTo(model1: T): T = {
        model = model1
        model
      }
      def get: T = model
    }

    def empty[T] = new AbsVar[T] { }

  }

  //val field = VarModel.empty[scalafx.scene.control.TextField]
  val stageS = VarModel.empty[JFXApp.PrimaryStage]
  val sceneS = VarModel.empty[Scene]
  val parentBox = VarModel.empty[VBox]
  val pictureContent = VarModel.empty[HBox]
  val inputContent = VarModel.empty[VBox]
  //val needToFixWidth = VarModel.empty[CheckBox]
  //val needToWater = VarModel.empty[CheckBox]

  var fileChooseButton = VarModel.empty[Button]
  var startIndexButton = VarModel.empty[Button]
  var indexFile = Option.empty[File]
  val defaultWidth = 454

  stage = stageS setTo new JFXApp.PrimaryStage {
    title.value = "喵喵酱的文件搜索"
    height = 600
    width = 600

    scene = sceneS setTo new Scene {
      content = parentBox setTo new VBox {
        fillWidth = true
        children = List(
          pictureContent setTo new HBox {
            style = "-fx-alignment: center;"
            children = List(
              fileChooseButton setTo new Button("文件选择") {
                handleEvent(MouseEvent.MouseClicked) {
                  event: MouseEvent =>
                    val file = new DirectoryChooser {
                      indexFile.map(f => initialDirectory = f)
                      title = "选择索引文件"
                    }.showDialog(stageS.get)
                    indexFile = Option(file)
                    ()
                }
              },
              startIndexButton setTo new Button("开始索引") {
                handleEvent(MouseEvent.MouseClicked) {
                  event: MouseEvent =>
                    new Alert(Alert.AlertType.Information) {
                      contentText = indexFile.map(_.toPath.toRealPath().toString).getOrElse("没有文件")
                    }.showAndWait()
                    ()
                }
              }
            )
            /*handleEvent(DragEvent.DragOver) {
              event: DragEvent =>
                event.acceptTransferModes(TransferMode.Move)
                event.consume()
            }
            handleEvent(DragEvent.DragDropped) {
              event: DragEvent =>
                val db = event.dragboard
                var success = false
                val fileList = db.files
                if (! fileList.isEmpty) {
                  success = true
                  val modelsToAdd = fileList.map(s => CopyPic.convert(s)(needToFixWidth.get.selected.value)(field.get.text.value.toInt)(needToWater.get.selected.value))
                }
                event.dropCompleted = success
                event.consume
            }
            children = new Label {
              text = "拖动一个或多个图片文件到此处自动处理（会自动过滤非图片文件）"
            }*/
          },
          inputContent setTo new VBox {
            style = "-fx-background-color: #336699; -fx-alignment: center; -fx-fill-width: false;"

            /*field setTo new scalafx.scene.control.TextField {
              style = "-fx-alignment: center;"
              focused.onChange { (_, _, newValue) =>
                val newInt = Try { text.value.toInt }.toOption
                if (newInt.isEmpty) {
                  text = defaultWidth.toString
                }
              }
              text = defaultWidth.toString

            }

            needToFixWidth setTo new CheckBox {
              selected = true
            }

            needToWater setTo new CheckBox {
              selected = true
            }

            style = "-fx-background-color: #336699; -fx-alignment: center; -fx-fill-width: false;"
            try {
              children = new GridPane {
                hgap = 10
                vgap = 10


                add(new Label {
                  text = "是否改变宽度"
                }, 0, 0)
                add(needToFixWidth.get, 1, 0)

                add(new Label {
                  text = "默认宽度"
                }, 0, 1)
                add(new HBox {
                  children = List(
                    field.get,
                    new Label {
                      text = " px"
                    }
                  )
                }, 1, 1)


                add(new Label {
                  text = "是否打水印"
                }, 0, 2)
                add(needToWater.get, 1, 2)

              }
            } catch {
              case e: Exception => e.printStackTrace
            }*/

          }
        )
      }
    }
  }

  pictureContent.get.prefHeight <== parentBox.get.height * 0.2
  parentBox.get.prefHeight <== sceneS.get.height
  parentBox.get.prefWidth <== sceneS.get.width
  inputContent.get.prefHeight <== parentBox.get.height * 0.8

  //field.get.disable <== (! needToFixWidth.get.selected)

}