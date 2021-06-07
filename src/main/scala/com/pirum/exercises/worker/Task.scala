package com.pirum.exercises.worker

import akka.actor.typed.ActorRef
import com.pirum.exercises.worker.Controller.SimulationSchedulerRef
import com.pirum.exercises.worker.SimulationScheduler._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

// A task that either succeeds after n seconds, fails after n seconds, or never terminates
sealed trait Task {
  def id: String
  def execute: Future[Unit]
}

case class SimulatedTask(
  id: String,
  result: Try[Unit],
  after: FiniteDuration,
  scheduler: ActorRef[Request]
) extends Task {
  override def execute: Future[Unit] = {
    val promise = Promise[Unit]()
    scheduler ! Request(Simulation(promise, result), after)
    promise.future
  }
}

case class SimulatedHangingTask(id: String) extends Task {
  override def execute: Future[Unit] = Promise[Unit]().future
}

object Task {

  def succeed(id: String, after: FiniteDuration)(implicit ssr: SimulationSchedulerRef): Task = {
    SimulatedTask(id, Success(()), after, ssr.ref)
  }

  def fail(id: String, reason: Throwable, after: FiniteDuration)(implicit ssr: SimulationSchedulerRef): Task = {
    SimulatedTask(id, Failure(reason), after, ssr.ref)
  }

  def hang(id: String): Task = {
    SimulatedHangingTask(id)
  }
}
