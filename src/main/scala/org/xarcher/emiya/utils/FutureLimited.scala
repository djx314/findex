package org.xarcher.emiya.utils

import java.util.concurrent.Executors

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

trait FutureWrapper {

  def runFuture: Future[Boolean]
  val weight: Int

}

class FutureLimited(exceptWeight: Int)(implicit ec: ExecutionContext) {

  protected val futureQueue = mutable.Queue[FutureWrapper]()
  protected val executeQueue = mutable.Queue[FutureWrapper]()

  @volatile protected var execAction: Future[Boolean] = Future.successful(true)

  protected def runLoop: Boolean = if (execAction.isCompleted) {
    execAction = loop()
    true
  } else {
    false
  }

  protected def loop(): Future[Boolean] = {
    def needRun = executeQueue.map(_.weight).sum > exceptWeight
    if (needRun) {
      val queue = executeQueue.dequeueAll(_ => true)
      Future.sequence(queue.map(_.runFuture)).flatMap((_: Seq[Boolean]) => loop())
    } else {
      val firstF = Try {
        futureQueue.dequeue()
      }
      firstF match {
        case Success(first) =>
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
          val queue = executeQueue.dequeueAll(_ => true)
          Future.sequence(queue.map(_.runFuture)).map((_: Seq[Boolean]) => true)
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
      override def runFuture: Future[Boolean] = {
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
  def create(exceptWeight: Int): FutureLimited = {
    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    new FutureLimited(exceptWeight)(ec)
  }
}