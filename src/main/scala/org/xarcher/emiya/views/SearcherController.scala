package org.xarcher.emiya.views

import javafx.beans.value.{ ChangeListener, ObservableValue }

import org.xarcher.xPhoto.FileSearch

import scala.concurrent.{ ExecutionContext, Future }
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout._

class SearchController(searcherPane: SearcherPane, searchContent: SearchContent) extends VBox {
  style = "-fx-background-color: #66ccff; -fx-alignment: center; -fx-fill-width: true;"
  fillWidth = true
  children = List(
    //searchInput,
    searcherPane,
    searchContent)

  searcherPane.prefWidth <== prefWidth
  searchContent.prefHeight <== (height - searcherPane.prefHeight)
  searchContent.prefWidth <== prefWidth
}

class SearcherPane(fuzzySearchInput: FuzzySearchInput, exactSearchInput: ExactSearchInput, doSearch: DoSearch) extends HBox {
  prefHeight = 60
  fillHeight = false
  style = "-fx-background-color: #66ccff; -fx-alignment: center;"

  val fuzzyLabel = new Label("模糊关键词") {
    prefWidth = 90
    style = "-fx-alignment: center;"
  }

  val exactLabel = new Label("精确关键词") {
    prefWidth = 90
    style = "-fx-alignment: center;"
  }

  val fuzzyInputContent = new HBox {
    style = "-fx-alignment: center-left;"
    children = fuzzySearchInput
  }

  val fuzzyContent = new HBox {
    style = "-fx-alignment: center-left;"
    children = fuzzyInputContent
  }

  val exactInputContent = new HBox {
    style = "-fx-alignment: center-left;"
    children = exactSearchInput
  }
  val exactContent = new HBox {
    style = "-fx-alignment: center-left;"
    children = exactSearchInput
  }

  children = List(
    fuzzyLabel,
    fuzzyInputContent,
    exactLabel,
    exactInputContent)

  fuzzyInputContent.prefWidth <== (prefWidth - fuzzyLabel.prefWidth)
  fuzzySearchInput.prefWidth <== fuzzyInputContent.prefWidth * 0.90

}

class FuzzySearchInput()(implicit ec: ExecutionContext) extends TextField {
  self =>

  prefHeight = 30

}

class ExactSearchInput()(implicit ec: ExecutionContext) extends TextField {
  self =>

  prefHeight = 30

}

class SearchContent() extends ScrollPane {
  fitToWidth = true
}

class DoSearch(fuzzySearchInput: FuzzySearchInput, exactSearchInput: ExactSearchInput, searchContent: SearchContent)(implicit ec: ExecutionContext) {

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
          searchContent.content = new VBox {
            children = info.map { eachInfo =>
              new VBox {
                children = new VBox {

                  val fileName = eachInfo.fileNameFlow
                  (fileName: Region).prefWidth <== searchContent.width - 200

                  val content = eachInfo.contentFlow
                  (content: Region).prefWidth <== searchContent.width

                  val titleContent = new HBox {
                    prefHeight <== ((fileName: Region).prefHeight + 10)
                    style = "-fx-background-color: #eeeeee; -fx-alignment: center-left; -fx-padding: 8px 0px 6px 0px;"
                    fillHeight = false
                    children = List(
                      fileName: Node,
                      eachInfo.fileBtn: Node,
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