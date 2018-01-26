package org.xarcher.emiya.views

import org.xarcher.emiya.views.index.IndexController
import org.xarcher.emiya.views.search.SearchController

import scalafx.scene.layout._

class ParentBox(indexController: IndexController, searchController: SearchController) extends BorderPane {

  left = indexController
  center = searchController

  indexController.prefWidth <== width * 0.2
}