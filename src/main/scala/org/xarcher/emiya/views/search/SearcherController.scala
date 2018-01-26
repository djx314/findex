package org.xarcher.emiya.views.search

import scalafx.Includes._
import scalafx.beans.property.DoubleProperty
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Paint
import scalafx.scene.text.TextAlignment

class SearcherPane(fuzzySearchInput: FuzzySearchInput, exactSearchInput: ExactSearchInput, doSearch: DoSearch) extends HBox {
  self =>

  prefHeight = 60
  fillHeight = false
  background = new Background(Array(new BackgroundFill(Paint.valueOf("#66ccff"), CornerRadii.Empty, Insets.Empty)))
  alignment = Pos.Center

  val fuzzyLabel = new Label("模糊关键词") {
    prefWidth = 80
    alignment = Pos.Center
    textAlignment = TextAlignment.Center
  }

  val exactLabel = new Label("精确关键词") {
    prefWidth = 80
    alignment = Pos.Center
    textAlignment = TextAlignment.Center
  }

  val fuzzyInputContent = new Pane {
    alignment = Pos.Center
    children = fuzzySearchInput
    fuzzySearchInput.prefWidth <== (width - 10)
  }
  fuzzyLabel.prefHeight <== fuzzyInputContent.height

  val exactInputContent = new Pane {
    alignment = Pos.Center
    children = exactSearchInput
    exactSearchInput.prefWidth <== (width - 16)
  }
  exactLabel.prefHeight <== exactInputContent.height

  val fuzzyContent = new BorderPane {
    left = fuzzyLabel
    center = fuzzyInputContent
  }

  val exactContent = new BorderPane {
    left = exactLabel
    center = exactInputContent
  }

  children = List(
    fuzzyContent,
    exactContent)

  val percent = DoubleProperty(0.5)

  fuzzyInputContent.prefWidth <== (fuzzyContent.width - fuzzyLabel.width)
  fuzzyContent.prefWidth <== (width * percent - 3)
  exactInputContent.prefWidth <== (exactContent.width - exactLabel.width)
  exactContent.prefWidth <== (width * (-percent + 1) + 3)

  fuzzyContent.onMouseEntered = {
    e: MouseEvent =>
      percent.value = 0.6
      ()
  }

  exactContent.onMouseEntered = {
    e: MouseEvent =>
      percent.value = 0.4
      ()
  }

  onMouseEntered = {
    e: MouseEvent =>
      percent.value = 0.5
      ()
  }

}