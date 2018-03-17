package org.xarcher.emiya.utils

import java.util.Timer
import java.util.concurrent.{ Executors, TimeUnit }

import akka.actor.ActorRef
import com.softwaremill.tagging.@@
import org.xarcher.xPhoto.IndexExecutionContext

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

trait FutureWrapper {

  def runFuture: Future[Boolean]
  val weight: Long

}

class FutureLimited(val exceptWeight: Int, val name: String, limitedActor: ActorRef, shutdownHook: ShutdownHook)(implicit ec: ExecutionContext) {

  def limit[T](futureFunc: () => Future[T], key: String): Future[T] = {
    limit(futureFunc, 1, key)
  }

  def limit[T](futureFunc: () => Future[T], weight: Long, key: String): Future[T] = {
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
            //e.printStackTrace
            promise.failure(e)
            endPromise.trySuccess(true)
        }
        endPromise.future
      }

      override val weight = weight1
    }

    limitedActor ! LimitedActor.AddWrapperModel(wrapper)

    val aa = promise.future

    /*{
      import java.util.TimerTask
      val timer = new Timer()
      shutdownHook.addHook(new Thread() {
        override def run(): Unit = {
          timer.cancel()
        }
      })
      timer.schedule(new TimerTask() {
        override def run(): Unit = {
          if (!aa.isCompleted) {
            println(s"actor 限流逻辑11执行异常-${name}-${key}-未完成")
          } else {
            //println(s"${name}-${key}-已完成")
            timer.cancel()
          }
        }
      }, 12000, 12000)
    }*/

    aa.andThen {
      case Failure(e) =>
      //e.printStackTrace
    }
  }

}

class FutureLimitedGen(limitedActor: ActorRef @@ LimitedActor, shutdownHook: ShutdownHook)(implicit executionContext: ExecutionContext) {
  def create(exceptWeight: Int, name: String): FutureLimited = {
    /*val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    shutdownHook.addHook(new Thread() {
      override def run(): Unit = {
        ec.shutdown() //.awaitTermination(20, TimeUnit.SECONDS)
      }
    })*/
    shutdownHook.addHook(new Thread() { override def run(): Unit = { limitedActor ! LimitedActor.Shutdown } })

    limitedActor ! LimitedActor.Start(exceptWeight)
    new FutureLimited(exceptWeight, name, limitedActor, shutdownHook)
  }
}