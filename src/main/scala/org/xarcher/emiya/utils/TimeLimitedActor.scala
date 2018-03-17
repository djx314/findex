package org.xarcher.emiya.utils

import java.util.{ Timer, TimerTask }

import akka.actor.Actor
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try

object TimeLimitedActor {

  case class AddWrapperModel(wrapper: FutureWrapper)
  case class Start(maxWeightSum: Long, millions: Long)
  case object Consume
  //case class Plus(weight: Long)
  case class Minus(weight: Long)
  case object Reset
  case object Shutdown

}

class TimeLimitedActor(shutdownHook: ShutdownHook) extends Actor {

  @volatile protected var futureQueue = mutable.Queue.empty[FutureWrapper]
  @volatile protected var weightSum: Long = 0
  @volatile protected var maxWeightSum: Long = -1
  protected val timer: Timer = new Timer()

  val logger = LoggerFactory.getLogger(getClass)

  override lazy val receive = {
    case TimeLimitedActor.Start(maxWeightSum1, millions1) =>
      maxWeightSum = maxWeightSum1
      val self1 = self
      val task = new TimerTask {
        override def run(): Unit = {
          self1 ! TimeLimitedActor.Reset
        }
      }
      //val timer = new Timer()
      /*shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          timer.cancel()
        }
      })*/
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
    case TimeLimitedActor.Shutdown =>
      futureQueue.dequeueAll(_ => true).map(_.runFuture)
      logger.info("关闭按时间限流定时器")
      timer.cancel()
      context.stop(self)
  }

}