package org.xarcher.emiya.utils

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{ Failure, Try }

class ShutdownHook() {

  protected var hooks: ListBuffer[Thread] = new ListBuffer[Thread]

  def addHook(f: Thread): Unit = {
    hooks += f
  }

  def exec(): ListBuffer[Unit] = {
    /*hooks.foldLeft(Future.successful(())) { (front, each) =>
      front.transformWith { (_: Try[Unit]) =>
        //println("closing")
        each()
      }
    }*/
    //Future.sequence(hooks.map(_.apply())).map((_: ListBuffer[Unit]) => ())
    hooks.zipWithIndex.map {
      case (s, _) =>
        s.start
    }
  }

}