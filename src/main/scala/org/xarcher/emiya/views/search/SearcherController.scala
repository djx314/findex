package org.xarcher.emiya.views.search

import scalafx.Includes._
import scalafx.beans.property.DoubleProperty
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Paint

class SearcherPane(fuzzySearchInput: FuzzySearchInput, exactSearchInput: ExactSearchInput, doSearch: DoSearch) extends HBox {
  prefHeight = 60
  fillHeight = false
  background = new Background(Array(new BackgroundFill(Paint.valueOf("#66ccff"), CornerRadii.Empty, Insets.Empty)))
  alignment = Pos.Center

  val fuzzyLabel = new Label("模糊关键词") {
    prefWidth = 80
    alignment = Pos.Center
  }

  val exactLabel = new Label("精确关键词") {
    prefWidth = 80
    alignment = Pos.Center
  }

  val fuzzyInputContent = new HBox {
    alignment = Pos.CenterLeft
    children = fuzzySearchInput
    fuzzySearchInput.prefWidth <== (prefWidth - 10)
  }

  val exactInputContent = new HBox {
    alignment = Pos.CenterLeft
    children = exactSearchInput
    exactSearchInput.prefWidth <== (prefWidth - 16)
  }

  val fuzzyContent = new HBox {
    alignment = Pos.CenterLeft
    children = List(
      fuzzyLabel,
      fuzzyInputContent)
  }

  val exactContent = new HBox {
    alignment = Pos.CenterLeft
    children = List(
      exactLabel,
      exactInputContent)
  }

  children = List(
    fuzzyContent,
    exactContent)

  val percent = DoubleProperty(0.5)

  fuzzyInputContent.prefWidth <== (fuzzyContent.prefWidth - fuzzyLabel.prefWidth)
  fuzzyContent.prefWidth <== (prefWidth * percent - 3)
  exactInputContent.prefWidth <== (exactContent.prefWidth - exactLabel.prefWidth)
  exactContent.prefWidth <== (prefWidth * (-percent + 1) + 3)

  fuzzyContent.handleEvent(MouseEvent.MouseEntered) {
    e: MouseEvent =>
      percent.value = 0.6
      ()
  }

  exactContent.handleEvent(MouseEvent.MouseEntered) {
    e: MouseEvent =>
      percent.value = 0.4
      ()
  }

  handleEvent(MouseEvent.MouseExited) {
    e: MouseEvent =>
      percent.value = 0.5
      ()
  }

}