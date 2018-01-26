package org.xarcher.emiya.views.search

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout._

class SearchController(searcherPane: SearcherPane, resultContent: ResultContent) extends BorderPane {
  //fillWidth = true
  //alignment = Pos.Center
  /*children = List(
    searcherPane,
    resultContent)*/

  top = searcherPane
  center = resultContent

  searcherPane.prefWidth <== width
  /*resultContent.prefHeight <== (height - searcherPane.prefHeight)
  resultContent.prefWidth <== prefWidth*/
}