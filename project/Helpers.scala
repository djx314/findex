/*package org.xarcher.sbt

import akka.actor.{ActorSystem, Props, Actor}

import java.io.{BufferedReader, InputStreamReader, InputStream}

import sbt._
import sbt.Keys._

object Helpers {

  class ProcessGen extends Actor {

    override def receive = {
      case process: java.lang.Process =>
        val rightListener = context.actorOf(Props[CmdOutListener])
        val errorListener = context.actorOf(Props[CmdOutListener])
        rightListener ! (process.getInputStream -> "成功")
        errorListener ! (process.getErrorStream -> "失败")
    }

  }

  class CmdOutListener extends Actor {
    override def receive = {
      case (s: InputStream, result: String) => {
        val inputReader = new InputStreamReader(s)
        val inputBuReader = new BufferedReader(inputReader)
        try {
          Iterator continually inputBuReader.readLine takeWhile (_ != null) foreach (t => { println(s"$result : $t") })
        } catch {
          case e: Exception => e.printStackTrace
        } finally {
          inputBuReader.close()
          inputReader.close()
          s.close()
        }
        context.stop(self)
      }
    }
  }

  val system = ActorSystem("CmdSystem", None, Option(getClass.getClassLoader))
  val processListener = system.actorOf(Props[ProcessGen], name = "processGenActor")

  def execCommonLine(process: java.lang.Process) = {
    processListener ! process
    println(process.waitFor)
  }

}*/