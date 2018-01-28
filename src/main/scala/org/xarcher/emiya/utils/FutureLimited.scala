package org.xarcher.emiya.utils

import java.util.Timer
import java.util.concurrent.Executors

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

trait FutureWrapper {

  def runFuture: Future[Boolean]
  val weight: Int

}

class FutureLimited(val exceptWeight: Int, val name: String)(implicit ec: ExecutionContext) {

  protected val futureQueue = mutable.Queue.empty[FutureWrapper]
  protected val executeQueue = mutable.Queue.empty[FutureWrapper]

  @volatile protected var execAction: Future[Boolean] = Future.successful(true)

  protected def runLoop: Boolean = if (execAction.isCompleted) {
    execAction = loop()
    true
  } else {
    false
  }

  {
    import java.util.TimerTask
    val timer = new Timer()
    timer.schedule(new TimerTask() {
      override def run(): Unit = {
        println(s"${name}-futureQueue:${futureQueue.size}")
        println(s"${name}-executeQueue:${executeQueue.size}")
        println(s"${name}-executeQueueIsSuccess:${executeQueue.map(_.runFuture.isCompleted).toList}")
        def needRun = executeQueue.map(_.weight).sum > exceptWeight
        println(s"bb-${name}-${executeQueue.map(_.weight).sum}-${exceptWeight}-${needRun}")
      }
    }, 1000, 1000)
  }

  protected def loop(): Future[Boolean] = {
    def needRun = executeQueue.map(_.weight).sum > exceptWeight
    if (!name.startsWith("db")) {
      (name).toString
      (executeQueue.map(_.weight).sum).toString
      (needRun).toString
      //println(s"aa-${name}-${executeQueue.map(_.weight).sum}-${exceptWeight}-${needRun}")
    }
    if (needRun) {
      val queue = executeQueue.dequeueAll(_ => true)
      Future.sequence(queue.map(_.runFuture)).flatMap((_: Seq[Boolean]) => loop())
    } else {
      val firstF = Try {
        futureQueue.dequeue()
      }
      firstF match {
        case Success(first) =>
          //println(name + "22" * 100)
          //println(first.toString)
          executeQueue += first
          loop()
        case Failure(_) =>
          //当队列已经没有元素的时候，应当尽快结束函数以便后续元素添加
          /*Future {
            if (!executeQueue.isEmpty) {
              val queue = executeQueue.dequeueAll(_ => true)
              queue.map(_.runFuture): Seq[Future[Boolean]]
            }
          }
          Future.successful(true)*/
          //println("33" * 100)
          val queue = executeQueue.dequeueAll(_ => true)
          val result = Future.sequence(queue.map(_.runFuture))
          result.map { s =>
            if (!name.startsWith("db")) {
              println("bb" * 8 + s"-${name}-${s.toList}")
            }
          }.recover {
            case e: Exception =>
              e.printStackTrace
          }
          result.flatMap((_: Seq[Boolean]) =>
            if (futureQueue.isEmpty) {
              Future.successful(true)
            } else {
              loop()
            })
        case s =>
          //println("11" * 100)
          //println(s.toString)
          Future.successful(true)
      }
    }
  }

  def limit[T](futureFunc: () => Future[T]): Future[T] = {
    limit(futureFunc, 1)
  }

  def limit[T](futureFunc: () => Future[T], weight: Int): Future[T] = {
    val weight1 = weight
    val promise = Promise[T]

    val wrapper: FutureWrapper = new FutureWrapper {
      override lazy val runFuture: Future[Boolean] = {
        val endPromise = Promise[Boolean]
        futureFunc().onComplete {
          case Success(r) =>
            promise.trySuccess(r)
            endPromise.trySuccess(true)
          case Failure(e) =>
            promise.tryFailure(e)
            endPromise.trySuccess(false)
        }
        endPromise.future
      }

      override val weight = weight1
    }
    futureQueue += wrapper

    //消费队列里面的 future
    runLoop

    promise.future
  }

}

object FutureLimited {
  def create(exceptWeight: Int, name: String): FutureLimited = {
    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    new FutureLimited(exceptWeight, name)(ec)
  }
}