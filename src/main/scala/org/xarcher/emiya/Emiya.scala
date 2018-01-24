package org.xarcher.xPhoto

import java.io.File
import java.util.concurrent.{ ExecutorService, Executors, ThreadFactory }

import org.fxmisc.richtext.InlineCssTextArea

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scalafx.Includes._
import scalafx.application.{ JFXApp, Platform }
import scalafx.scene.{ Node, Scene }
import scalafx.scene.control._
import scalafx.scene.input.{ DragEvent, MouseEvent, TransferMode }
import scalafx.scene.layout._
import scalafx.stage.{ DirectoryChooser, FileChooser }
import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.beans.property.DoubleProperty
import scalafx.scene.paint.Color
import scalafx.scene.text.{ Font, Text, TextFlow }

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

    def empty[T] = new AbsVar[T] {}

  }

  //val field = VarModel.empty[scalafx.scene.control.TextField]
  val stageS = VarModel.empty[JFXApp.PrimaryStage]
  val sceneS = VarModel.empty[Scene]
  val parentBox = VarModel.empty[VBox]
  val pictureContent = VarModel.empty[HBox]
  val inputContent = VarModel.empty[VBox]

  val searchBtn = VarModel.empty[Button]
  val searchInput = VarModel.empty[TextField]
  val searchContent = VarModel.empty[ScrollPane]

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
                    indexFile.map(file =>
                      FileIndex.index(file.toPath).map { (_: Int) =>
                        new Alert(Alert.AlertType.Information) {
                          contentText = indexFile.map(_ => "索引完成").getOrElse("没有文件")
                        }.showAndWait()
                      }).getOrElse {
                      new Alert(Alert.AlertType.Information) {
                        contentText = indexFile.map(_.toPath.toRealPath().toString).getOrElse("没有文件")
                      }.showAndWait()
                    }
                    ()
                }
              })
          },
          inputContent setTo new VBox {
            style = "-fx-background-color: #336699; -fx-alignment: center; -fx-fill-width: false;"

            searchContent setTo new ScrollPane {
              fitToWidth = true
            }

            //val th = Thread.currentThread()
            try
              children = List(
                /*{
                  val textArea = new InlineCssTextArea("asfasfsafweatertrytruytutyfuiytfuftuftyutyu")
                  textArea.setStyle(4, 8, "-fx-fill: red;")
                  textArea.prefWidth(120)
                  textArea.setWrapText(true)
                  textArea.setEditable(false)
                  textArea: Node
                },*/
                searchInput setTo new TextField {
                  prefHeight = 30
                },
                searchBtn setTo new Button("搜索") {
                  prefHeight = 30
                  handleEvent(MouseEvent.MouseClicked) {
                    event: MouseEvent =>
                      Future {
                        FileSearch.search(searchInput.get.text.get()).map { info =>
                          Platform.runLater(() => {
                            searchContent.get.content = new VBox {
                              children = info.map { eachInfo =>
                                new VBox {
                                  children = new VBox {

                                    val fileName = eachInfo.fileNameFlow
                                    (fileName: Region).prefWidth <== searchContent.get.width - 200
                                    (fileName: Region).prefHeight = 23

                                    val content = eachInfo.contentFlow
                                    (content: Region).prefWidth <== searchContent.get.width

                                    children = List(
                                      new HBox {
                                        children = List(
                                          fileName: Node,
                                          eachInfo.fileBtn: Node,
                                          eachInfo.dirBtn)
                                      },
                                      new HBox {
                                        children = content: Node
                                      })
                                  }
                                }
                              }
                            }
                          })
                        }
                      }
                      ()
                  }
                },
                searchContent.get
              /*new Label {
                  text = "99<span style=\"color: #ff0000;\">11223344</span>55667788"
                },
                new TextFlow {
                  children = List(
                    new Text("我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱") {
                      font = Font.font("微软雅黑", 16)
                      fill = Color.Red
                    },
                    new Text("我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱") {
                      font = Font.font("微软雅黑", 16)
                      fill = Color.Yellow
                    },
                    new Text("我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱我是喵喵酱") {
                      font = Font.font("微软雅黑", 16)
                      fill = Color.Blue
                    })
                }*/
              )
            catch {
              case e: Exception =>
                e.printStackTrace
            }
          })
      }
    }
  }

  pictureContent.get.prefHeight <== parentBox.get.height * 0.2
  parentBox.get.prefHeight <== sceneS.get.height
  parentBox.get.prefWidth <== sceneS.get.width
  inputContent.get.prefHeight <== parentBox.get.height * 0.8
  searchContent.get.prefHeight <== inputContent.get.height - 60
  searchContent.get.prefWidth <== inputContent.get.width

}