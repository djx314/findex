package org.xarcher.emiya.views.search

import javafx.beans.value.{ ChangeListener, ObservableValue }

import org.xarcher.xPhoto.FileSearch

import scala.concurrent.{ ExecutionContext, Future }
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.{ Control, ScrollPane, Tooltip }
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Paint
import scalafx.scene.text.Font
import scalafx.stage.{ Screen, Window }

class ResultContent() extends ScrollPane {
  fitToWidth = true
}

class DoSearch(fuzzySearchInput: FuzzySearchInput, exactSearchInput: ExactSearchInput, resultContent: ResultContent)(implicit ec: ExecutionContext) {

  fuzzySearchInput.text.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      search(fuzzySearchInput.text.value, exactSearchInput.text.value)
    }
  })
  exactSearchInput.text.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      search(fuzzySearchInput.text.value, exactSearchInput.text.value)
    }
  })

  def search(fuzzyKey: String, exactKey: String): Unit = {
    Future {
      FileSearch.search(fuzzyKey).map { info =>
        Platform.runLater(() => {
          resultContent.content = new VBox {
            children = info.map { eachInfo =>
              new VBox {
                children = new VBox {
                  val fileName: Region = eachInfo.fileNameFlow
                  fileName.prefWidth <== resultContent.width - 200
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
                  (content: Region).prefWidth <== resultContent.width

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
        })
      }
    }
  }

}