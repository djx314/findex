package org.xarcher.emiya.views.search

import javafx.beans.value.{ ChangeListener, ObservableValue }

import org.xarcher.xPhoto.FileSearch

import scala.concurrent.{ ExecutionContext, Future }
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout._
import scalafx.scene.paint.Paint

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

                  val fileName = eachInfo.fileNameFlow
                  (fileName: Region).prefWidth <== resultContent.width - 200

                  val content = eachInfo.contentFlow
                  (content: Region).prefWidth <== resultContent.width

                  val titleContent = new HBox {
                    prefHeight <== ((fileName: Region).prefHeight + 10)
                    //style = " -fx-alignment: center-left; -fx-padding: 8px 0px 6px 0px;"
                    background = new Background(Array(new BackgroundFill(Paint.valueOf("#eeeeee"), CornerRadii.Empty, Insets.Empty)))
                    padding = Insets.apply(8, 0, 6, 0)
                    fillHeight = false
                    children = List(
                      fileName: Node,
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