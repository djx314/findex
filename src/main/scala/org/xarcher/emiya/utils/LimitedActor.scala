package org.xarcher.emiya.utils

import akka.actor.Actor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

object LimitedActor {

  case class AddWrapperModel(wrapper: FutureWrapper)
  case class Start(maxWeightSum: Long)
  case object Consume
  case class Plus(weight: Long)
  case class Minus(weight: Long)

}

class LimitedActor() extends Actor {

  @volatile protected var futureQueue = mutable.Queue.empty[FutureWrapper]
  @volatile protected var weightSum: Long = 0
  @volatile protected var maxWeightSum: Long = -1

  override lazy val receive = {
    case LimitedActor.Start(maxWeightSum1) =>
      maxWeightSum = maxWeightSum1
    case LimitedActor.Consume =>
      if (weightSum < maxWeightSum) {
        futureQueue.dequeueFirst(_ => true) match {
          case Some(wrapper) =>
            val self1 = self
            weightSum += wrapper.weight
            wrapper.runFuture.andThen {
              case _ =>
                self1 ! LimitedActor.Minus(wrapper.weight)
            }
          case None =>
        }
      }
    case LimitedActor.Minus(weight) =>
      weightSum -= weight
      self ! LimitedActor.Consume
    case LimitedActor.AddWrapperModel(wrapper) =>
      futureQueue += wrapper
      self ! LimitedActor.Consume
  }

}