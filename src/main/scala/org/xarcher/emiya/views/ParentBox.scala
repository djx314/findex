package org.xarcher.emiya.views

import scalafx.scene.layout._

class ParentBox(indexController: IndexController, searchController: SearchController) extends HBox {
  fillHeight = true
  children = List(
    indexController,
    searchController)

  indexController.prefWidth <== width * 0.2
  searchController.prefWidth <== width * 0.8
}