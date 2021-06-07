package com.pirum.exercises.worker

import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import java.util.UUID

object SimulationScheduler {
  
  sealed trait Command

  case class Simulation(promise: Promise[Unit], result: Try[Unit]) extends Command
  case class Request(simulation: Simulation, after: FiniteDuration) extends Command
  case object Stop extends Command

  def apply(): Behavior[Command] =
    Behaviors.withTimers { scheduler =>
      Behaviors.receiveMessage {
        case Request(simulation, after) =>
          scheduler.startSingleTimer(UUID.randomUUID(), simulation, after)
          Behaviors.same
        case Simulation(promise, result) =>
          promise.complete(result)
          Behaviors.same
        case Stop => 
          scheduler.cancelAll()
          Behaviors.stopped
      }
    }

}
