package org.xarcher.emiya.utils

import java.util.{ Timer, TimerTask }

import akka.actor.Actor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

object TimeLimitedActor {

  case class AddWrapperModel(wrapper: FutureWrapper)
  case class Start(maxWeightSum: Long, millions: Long)
  case object Consume
  //case class Plus(weight: Long)
  case class Minus(weight: Long)
  case object Reset

}

class TimeLimitedActor() extends Actor {

  @volatile protected var futureQueue = mutable.Queue.empty[FutureWrapper]
  @volatile protected var weightSum: Long = 0
  @volatile protected var maxWeightSum: Long = -1

  override lazy val receive = {
    case TimeLimitedActor.Start(maxWeightSum1, millions1) =>
      maxWeightSum = maxWeightSum1
      val self1 = self
      val task = new TimerTask {
        override def run(): Unit = {
          self1 ! TimeLimitedActor.Reset
        }
      }
      val timer = new Timer()
      timer.schedule(task, 0, millions1)
    case TimeLimitedActor.Reset =>
      weightSum = maxWeightSum
      self ! TimeLimitedActor.Consume
    case TimeLimitedActor.Consume =>
      if (futureQueue.headOption.map(s => s.weight < weightSum).getOrElse(false)) {
        futureQueue.dequeueFirst(_ => true) match {
          case Some(wrapper) =>
            val self1 = self
            self1 ! TimeLimitedActor.Minus(wrapper.weight)
            wrapper.runFuture
          case None =>
        }
      }
    case TimeLimitedActor.Minus(weight) =>
      weightSum -= weight
      self ! TimeLimitedActor.Consume
    case TimeLimitedActor.AddWrapperModel(wrapper) =>
      futureQueue += wrapper
      self ! TimeLimitedActor.Consume
  }

}