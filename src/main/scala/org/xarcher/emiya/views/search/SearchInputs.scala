package org.xarcher.emiya.views.search

import scala.concurrent.ExecutionContext
import scalafx.scene.control.TextField

class FuzzySearchInput() extends TextField {
  self =>
  prefHeight = 30
}

class ExactSearchInput() extends TextField {
  self =>
  prefHeight = 30
}