package org.xarcher.emiya.utils

import java.util.Timer
import java.util.concurrent.Executors

import akka.actor.ActorRef
import com.softwaremill.tagging.@@

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

trait FutureWrapper {

  def runFuture: Future[Boolean]
  val weight: Long

}

class FutureLimited(val exceptWeight: Int, val name: String, limitedActor: ActorRef)(implicit ec: ExecutionContext) {
  /*@volatile protected var futureQueue = mutable.Queue.empty[FutureWrapper]
  @volatile protected var executeQueue = mutable.Queue.empty[FutureWrapper]

  def addFutureWrapper(wrapper: FutureWrapper) = {
    futureQueue += wrapper
  }
  def addExecWrapper(wrapper: FutureWrapper) = {
    executeQueue += wrapper
  }

  @volatile protected var execAction: Future[Boolean] = Future.successful(true)

  protected def runLoop: Boolean = this.synchronized {
    if (execAction.isCompleted) {
      if (execAction.isCompleted) {
        execAction = execAction.flatMap(_ => loop())
      }
      true
    } else {
      false
    }
  }

  {
    import java.util.TimerTask
    val timer = new Timer()
    timer.schedule(new TimerTask() {
      override def run(): Unit = {
        println(s"${name}-futureQueue:${futureQueue.size}")
        println(s"${name}-executeQueue:${executeQueue.size}")
        println(s"${name}-executeQueueIsSuccess:${executeQueue.map(_.runFuture.isCompleted).toList}")
        println(s"${name}-是否已完成:${execAction.isCompleted}")
        def needRun = executeQueue.map(_.weight).sum > exceptWeight
        println(s"bb-${name}-${executeQueue.map(_.weight).sum}-${exceptWeight}-${needRun}")
      }
    }, 4000, 4000)
  }

  protected def loop(): Future[Boolean] = this.synchronized {
    def needRun = executeQueue.map(_.weight).sum > exceptWeight
    if (!name.startsWith("db")) {
      (name).toString
      (executeQueue.map(_.weight).sum).toString
      (needRun).toString
      //println(s"aa-${name}-${executeQueue.map(_.weight).sum}-${exceptWeight}-${needRun}")
    }
    if (needRun) {
      val queue = { executeQueue.dequeueAll(_ => true) }
      Future.sequence(queue.map(_.runFuture)).flatMap((_: Seq[Boolean]) => loop())
    } else {
      { futureQueue.dequeueFirst(_ => true) } match {
        case Some(first) =>
          //executeQueue += first
          addExecWrapper(first)
          loop()
        case None =>
          val queue = { executeQueue.dequeueAll(_ => true) }
          val result = Future.sequence(queue.map(_.runFuture))
          result.map { s =>
            if (!name.startsWith("db")) {
              //println("bb" * 8 + s"-${name}-${s.toList}")
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
        //Future.successful(true)
        //case s =>
        //println("11" * 100)
        //println(s.toString)
        //Future.successful(true)
      }
    }
  }

  def limit1111[T](futureFunc: () => Future[T], weight: Int, key: String): Future[T] = this.synchronized {
    val weight1 = weight
    val promise = Promise[T]

    val wrapper: FutureWrapper = new FutureWrapper {
      override lazy val runFuture: Future[Boolean] = {
        val endPromise = Promise[Boolean]
        futureFunc().andThen {
          case Success(r) =>
            promise.success(r)
            endPromise.trySuccess(true)
          case Failure(e) =>
            e.printStackTrace
            promise.failure(e)
            endPromise.trySuccess(true)
        }
        endPromise.future
      }

      override val weight = weight1
    }
    //futureQueue += wrapper
    addFutureWrapper(wrapper)

    //消费队列里面的 future
    runLoop

    val aa = promise.future

    if (!key.isEmpty) {
      {
        import java.util.TimerTask
        val timer = new Timer()
        timer.schedule(new TimerTask() {
          override def run(): Unit = {
            if (!aa.isCompleted) {
              println("12" * 50 + s"${name}-${key}-未完成")
            } else {
              //println(s"${name}-${key}-已完成")
              timer.cancel()
            }
          }
        }, 3000, 3000)
      }
    }

    aa.andThen {
      case Failure(e) =>
        e.printStackTrace
    }
  }*/
  def limit[T](futureFunc: () => Future[T], key: String): Future[T] = {
    limit(futureFunc, 1, key)
  }

  def limit[T](futureFunc: () => Future[T], weight: Long, key: String): Future[T] = this.synchronized {
    val weight1 = weight
    val promise = Promise[T]

    val wrapper: FutureWrapper = new FutureWrapper {
      override lazy val runFuture: Future[Boolean] = {
        val endPromise = Promise[Boolean]
        futureFunc().andThen {
          case Success(r) =>
            promise.success(r)
            endPromise.trySuccess(true)
          case Failure(e) =>
            e.printStackTrace
            promise.failure(e)
            endPromise.trySuccess(true)
        }
        endPromise.future
      }

      override val weight = weight1
    }

    limitedActor ! LimitedActor.AddWrapperModel(wrapper)

    val aa = promise.future

    {
      import java.util.TimerTask
      val timer = new Timer()
      timer.schedule(new TimerTask() {
        override def run(): Unit = {
          if (!aa.isCompleted) {
            println(s"actor 限流逻辑执行异常-${name}-${key}-未完成")
          } else {
            //println(s"${name}-${key}-已完成")
            timer.cancel()
          }
        }
      }, 12000, 12000)
    }

    aa.andThen {
      case Failure(e) =>
        e.printStackTrace
    }
  }

}

class FutureLimitedGen(limitedActor: ActorRef @@ LimitedActor) {
  def create(exceptWeight: Int, name: String): FutureLimited = {
    val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    limitedActor ! LimitedActor.Start(exceptWeight)
    new FutureLimited(exceptWeight, name, limitedActor)(ec)
  }
}