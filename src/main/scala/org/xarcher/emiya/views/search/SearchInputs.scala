package org.xarcher.emiya.views.search

import scala.concurrent.ExecutionContext
import scalafx.scene.control.TextField

class FuzzySearchInput()(implicit ec: ExecutionContext) extends TextField {
  self =>
  prefHeight = 30
}

class ExactSearchInput()(implicit ec: ExecutionContext) extends TextField {
  self =>
  prefHeight = 30
}