package org.xarcher.emiya.views

import org.xarcher.xPhoto.FileSearch

import scala.concurrent.Future
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scala.concurrent.ExecutionContext.Implicits.global

class SearchController(searchInput: SearchInput, searcherButton: SearcherButton, searchContent: SearchContent) extends VBox {
  style = "-fx-background-color: #66ccff; -fx-alignment: center; -fx-fill-width: false;"
  children = List(
    searchInput,
    searcherButton,
    searchContent)

  searchContent.prefHeight <== height - 60
  searchContent.prefWidth <== width
}

class SearcherButton(searchContent: SearchContent, searchInput: SearchInput) extends Button("搜索") {
  prefHeight = 30
  handleEvent(MouseEvent.MouseClicked) {
    event: MouseEvent =>
      Future {
        FileSearch.search(searchInput.text.get()).map { info =>
          Platform.runLater(() => {
            searchContent.content = new VBox {
              children = info.map { eachInfo =>
                new VBox {
                  children = new VBox {

                    val fileName = eachInfo.fileNameFlow
                    (fileName: Region).prefWidth <== searchContent.width - 200
                    (fileName: Region).prefHeight = 23

                    val content = eachInfo.contentFlow
                    (content: Region).prefWidth <== searchContent.width

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

}

class SearchInput() extends TextField {
  prefHeight = 30
}

class SearchContent() extends ScrollPane {
  fitToWidth = true
}