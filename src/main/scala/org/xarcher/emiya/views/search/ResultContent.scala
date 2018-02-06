package org.xarcher.emiya.views.search

import java.net.URI
import java.nio.file.Paths
import javafx.beans.value.{ ChangeListener, ObservableValue }

import org.xarcher.emiya.views.index.FileListWrapper
import org.xarcher.xPhoto.{ FileSearch, OutputInfo }
import org.xarcher.xPhoto.FileTables.IndexContentRow

import scala.concurrent.{ ExecutionContext, Future }
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.input.{ KeyCode, KeyEvent, MouseEvent }
import scalafx.scene.layout._
import scalafx.scene.paint.Paint
import scalafx.scene.text.Font

/*class ResultContent() extends ScrollPane {
  fitToWidth = true
  content = new TabPane()
}*/

class ResultTabPane() extends TabPane {
}

class DoSearch(
  fuzzySearchInput: FuzzySearchInput,
  exactSearchInput: ExactSearchInput,
  resultTabPane: ResultTabPane,
  fileSearch: FileSearch,
  fileListWrapper: FileListWrapper)(implicit ec: ExecutionContext) {

  fuzzySearchInput.text.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      //search(newValue, exactSearchInput.text.value, fileListWrapper.FileList.currentItems)
    }
  })
  fuzzySearchInput.handleEvent(KeyEvent.KeyReleased) {
    event: KeyEvent =>
      if (event.code == KeyCode.Enter) {
        search(fuzzySearchInput.text.value, exactSearchInput.text.value, fileListWrapper.FileList.currentItems)
      }
  }

  exactSearchInput.text.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      //search(fuzzySearchInput.text.value, newValue, fileListWrapper.FileList.currentItems)
    }
  })
  exactSearchInput.handleEvent(KeyEvent.KeyReleased) {
    event: KeyEvent =>
      if (event.code == KeyCode.Enter) {
        search(fuzzySearchInput.text.value, exactSearchInput.text.value, fileListWrapper.FileList.currentItems)
      }
  }

  def search(fuzzyKey: String, exactKey: String, contents: List[IndexContentRow]): Unit = {
    val tabsF = contents.map { eachContent =>
      val scrollPane = new ScrollPane {
        self =>
        fitToWidth = true

        content = contentVBox

        lazy val contentVBox = new VBox {
        }

        def append(outPutInfo: List[OutputInfo]): Unit = {
          val nodes = outPutInfo.map { eachInfo =>
            val vBox = new VBox {
              children = new VBox {
                val fileName: Region = eachInfo.fileNameFlow
                fileName.prefWidth <== self.width - 200
                val tooltip = new Tooltip(eachInfo.filePath) {
                  font = Font(14)
                }
                fileName.onMouseEntered = {
                  e: MouseEvent =>
                    if (!tooltip.showing.value) {
                      tooltip.show(fileName, e.screenX + 6, {
                        val node: Node = e.source.asInstanceOf[javafx.scene.Node]
                        val bounds1 = node.boundsInParent
                        val bounds2 = node.localToScene(bounds1.value)
                        val bounds3 = node.scene.value.getWindow.getY
                        val bounds4 = node.scene.value.getY

                        bounds2.getMaxY + bounds3 + bounds4
                      })
                    }
                    ()
                }
                fileName.onMouseExited = {
                  e: MouseEvent =>
                    tooltip.hide()
                    ()
                }

                val content = eachInfo.contentFlow
                (content: Region).prefWidth <== self.width

                val titleContent = new HBox {
                  prefHeight <== (fileName.prefHeight + 10)
                  //style = " -fx-alignment: center-left; -fx-padding: 8px 0px 6px 0px;"
                  background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))
                  padding = Insets.apply(8, 0, 6, 0)
                  fillHeight = false
                  children = List(
                    fileName,
                    eachInfo.fileBtn,
                    eachInfo.dirBtn)
                }

                children = List(
                  titleContent,
                  new HBox {
                    children = content: Node
                  })
              }
            }
            val convert = implicitly[VBox => javafx.scene.layout.VBox]
            convert(vBox)
          }

          Platform.runLater(contentVBox.children.addAll(nodes: _*))
        }

        fileSearch.search(eachContent, fuzzyKey, exactKey, 0, 4).map {
          case (outinfos, count) =>
            append(outinfos)
        }
      }
      new Tab() {
        val path = Paths.get(URI.create(eachContent.rootUri))
        text = path.getFileName.toString
        tooltip = new Tooltip(path.toRealPath().toString) {
          font = Font(14)
        }
        content = scrollPane
      }: Tab
    }
    resultTabPane.tabs = tabsF

    /*Future {
      val infosF = Future.sequence(contents.map(item => fileSearch.search(item, fuzzyKey, exactKey, 0, 4).map { case (s, nextIndex) => (item, s, nextIndex) }))
      infosF.map { infos =>
        Platform.runLater(() => {
          //resultTabPane.tabs = List.empty[Tab]
          val tabs = infos.map {
            case (item, info, nextIndex) =>
              val scrollPane = new ScrollPane {
                self =>

                def changeListener(start: Int, initRows: Int, nextRows: Int, isFirst: Boolean): ChangeListener[Number] = new ChangeListener[Number] {
                  changeListenerSelf =>

                  override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
                    val scrollHeight = self.contentVBox.height.value - self.height.value
                    val heightToButtom = scrollHeight - scrollHeight * newValue.doubleValue
                    val lastHeightSum = contentVBox.children.takeRight(2).map(s => (s.asInstanceOf[javafx.scene.layout.Region]: Region).height.value).sum
                    if (heightToButtom < lastHeightSum) {
                      self.vvalue.removeListener(changeListenerSelf)
                      val nodesF = fileSearch.search(item, fuzzyKey, exactKey, start, if (isFirst) initRows else nextRows).map {
                        case (infos, nextIndex) =>
                          infos.map { eachInfo =>
                            new VBox {
                              children = new VBox {
                                val fileName: Region = eachInfo.fileNameFlow
                                fileName.prefWidth <== self.width - 200
                                val tooltip = new Tooltip(eachInfo.filePath) {
                                  font = Font(14)
                                }
                                fileName.onMouseEntered = {
                                  e: MouseEvent =>
                                    if (!tooltip.showing.value) {
                                      tooltip.show(fileName, e.screenX + 6, {
                                        val node: Node = e.source.asInstanceOf[javafx.scene.Node]
                                        val bounds1 = node.boundsInParent
                                        val bounds2 = node.localToScene(bounds1.value)
                                        val bounds3 = node.scene.value.getWindow.getY
                                        val bounds4 = node.scene.value.getY

                                        bounds2.getMaxY + bounds3 + bounds4
                                      })
                                    }
                                    ()
                                }
                                fileName.onMouseExited = {
                                  e: MouseEvent =>
                                    tooltip.hide()
                                    ()
                                }

                                val content = eachInfo.contentFlow
                                (content: Region).prefWidth <== self.width

                                val titleContent = new HBox {
                                  prefHeight <== (fileName.prefHeight + 10)
                                  //style = " -fx-alignment: center-left; -fx-padding: 8px 0px 6px 0px;"
                                  background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))
                                  padding = Insets.apply(8, 0, 6, 0)
                                  fillHeight = false
                                  children = List(
                                    fileName,
                                    eachInfo.fileBtn,
                                    eachInfo.dirBtn)
                                }

                                children = List(
                                  titleContent,
                                  new HBox {
                                    children = content: Node
                                  })
                              }
                            }
                          } -> nextIndex
                      }
                      nodesF.map {
                        case (nodes, nextIndex) =>
                          Platform.runLater {
                            self.contentVBox.children.addAll(nodes.map { s => s: javafx.scene.Node }: _*)
                            self.vvalue.addListener(changeListener(nextIndex, initRows, nextRows, false))
                          }
                      }.recover {
                        case e: Exception =>
                          e.printStackTrace
                      }
                    } else {
                      //println("22" * 100)
                    }
                    /*println("contentVBoxHeight:" + self.contentVBox.height.value)
                    println("vvalue:" + newValue.doubleValue)
                    println("1 - vvalue:" + (1 - newValue.doubleValue))
                    println("heightToButtom:" + ())
                    println("heightToButtom:" + (165 / (1 - newValue.doubleValue) - self.contentVBox.height.value))*/
                  }
                }

                vvalue.addListener(changeListener(0, 4, 2, true))

                fitToWidth = true

                content = contentVBox

                lazy val contentVBox = new VBox {
                  children = info.map { eachInfo =>
                    new VBox {
                      children = new VBox {
                        val fileName: Region = eachInfo.fileNameFlow
                        fileName.prefWidth <== self.width - 200
                        val tooltip = new Tooltip(eachInfo.filePath) {
                          font = Font(14)
                        }
                        fileName.onMouseEntered = {
                          e: MouseEvent =>
                            if (!tooltip.showing.value) {
                              tooltip.show(fileName, e.screenX + 6, {
                                val node: Node = e.source.asInstanceOf[javafx.scene.Node]
                                val bounds1 = node.boundsInParent
                                val bounds2 = node.localToScene(bounds1.value)
                                val bounds3 = node.scene.value.getWindow.getY
                                val bounds4 = node.scene.value.getY

                                bounds2.getMaxY + bounds3 + bounds4
                              })
                            }
                            ()
                        }
                        fileName.onMouseExited = {
                          e: MouseEvent =>
                            tooltip.hide()
                            ()
                        }

                        val content = eachInfo.contentFlow
                        (content: Region).prefWidth <== self.width

                        val titleContent = new HBox {
                          prefHeight <== (fileName.prefHeight + 10)
                          //style = " -fx-alignment: center-left; -fx-padding: 8px 0px 6px 0px;"
                          background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))
                          padding = Insets.apply(8, 0, 6, 0)
                          fillHeight = false
                          children = List(
                            fileName,
                            eachInfo.fileBtn,
                            eachInfo.dirBtn)
                        }

                        children = List(
                          titleContent,
                          new HBox {
                            children = content: Node
                          })
                      }
                    }
                  }
                }
              }

              new Tab() {
                val path = Paths.get(URI.create(item.rootUri))
                text = path.getFileName.toString
                tooltip = new Tooltip(path.toRealPath().toString) {
                  font = Font(14)
                }
                content = scrollPane
              }: Tab
          }
          //resultTabPane.tabs = tabs
        })

      }
    }*/
  }

}