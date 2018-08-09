package org.xarcher.emiya.views.search

import java.net.URI
import java.nio.file.Paths
import javafx.beans.value.{ ChangeListener, ObservableValue }

import org.xarcher.emiya.views.index.FileListWrapper
import org.xarcher.xPhoto.{ FileSearch, OutputInfo }
import org.xarcher.xPhoto.FileTables.IndexContentRow

import scala.concurrent.ExecutionContext
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.Includes._
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.input.{ KeyCode, KeyEvent, MouseEvent }
import scalafx.scene.layout._
import scalafx.scene.paint.Paint
import scalafx.scene.text.{ Font, Text, TextAlignment }

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
  fileListWrapper: FileListWrapper)(implicit executionContext: ExecutionContext) {

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

    val tabs = contents.map { eachContent =>
      val infoLabel = new Label("") {
        alignment = Pos.Center
        maxWidth = Double.MaxValue
        padding = Insets.apply(6, 0, 6, 0)
        background = new Background(Array(new BackgroundFill(Paint.valueOf("#f4f4f4"), CornerRadii.Empty, Insets.Empty)))
      }

      val scrollPane = new ScrollPane {
        self =>
        fitToWidth = true

        content = contentVBox

        lazy val contentVBox = new VBox

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

        val initSize = 4

        fileSearch.searchFromView(eachContent, fuzzyKey, exactKey, 0, initSize).map { wrap =>
          append(wrap.info)
          Platform.runLater {
            infoLabel.text = s"已为你搜索到 ${wrap.countSum} 条结果"
            infoLabel.visible = true
          }
          /*wrap.nextIndexOpt.foreach { count =>
            vvalue.addListener(changeListener(count, 2))
          }*/
        }

        def changeListener(start: Int, rows: Int): ChangeListener[Number] = new ChangeListener[Number] {
          changeListenerSelf =>

          override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
            val scrollHeight = self.contentVBox.height.value - self.height.value
            val heightToButtom = scrollHeight - scrollHeight * newValue.doubleValue
            val lastHeightSum = contentVBox.children.takeRight(2).map(s => (s.asInstanceOf[javafx.scene.layout.Region]: Region).height.value).sum
            if (heightToButtom < lastHeightSum) {
              self.vvalue.removeListener(changeListenerSelf)
              fileSearch.searchFromView(eachContent, fuzzyKey, exactKey, start, rows).map { wrap =>
                append(wrap.info)
                Platform.runLater {
                  infoLabel.text = s"已为你搜索到 ${wrap.countSum} 条结果"
                  infoLabel.visible = true
                }
                wrap.nextIndexOpt.foreach { count =>
                  vvalue.addListener(changeListener(count, rows))
                }
              }
            }
          }
        }

      }
      new Tab {
        val path = Paths.get(URI.create(eachContent.rootUri))
        text = path.getFileName.toString
        tooltip = new Tooltip(path.toRealPath().toString) {
          font = Font(14)
        }
        content = new VBox {
          children = List(
            infoLabel,
            scrollPane)
        }
      }: Tab
    }
    resultTabPane.tabs = tabs
  }

}