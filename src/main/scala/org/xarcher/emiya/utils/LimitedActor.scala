package org.xarcher.emiya.utils

import akka.actor.Actor
import org.xarcher.xPhoto.IndexExecutionContext

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object LimitedActor {

  case class AddWrapperModel(wrapper: FutureWrapper)
  case class Start(maxWeightSum: Long)
  case object Consume
  case class Plus(weight: Long)
  case class Minus(weight: Long)
  case object Shutdown

}

class LimitedActor(executionContext: ExecutionContext) extends Actor {

  @volatile protected var futureQueue = mutable.Queue.empty[FutureWrapper]
  @volatile protected var weightSum: Long = 0
  @volatile protected var maxWeightSum: Long = -1

  implicit val exec = executionContext

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
    case LimitedActor.Shutdown =>
      futureQueue.dequeueAll(_ => true).map(_.runFuture)
      context.stop(self)
  }

}