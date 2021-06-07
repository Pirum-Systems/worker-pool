package com.pirum.exercises.worker

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.pirum.exercises.worker.Result._
import akka.actor.typed.Terminated

object Controller {

  sealed trait Command

  case class SpawnSimulationScheduler(replyTo: ActorRef[SimulationSchedulerRef]) extends Command
  case class SimulationSchedulerRef(ref: ActorRef[SimulationScheduler.Request]) extends Command
  case class Job(jobDetails: JobDetails) extends Command
  case class ResultWrapper(result: Result) extends Command
  case object Tick extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>

      val resultMapper: ActorRef[Result] = context.messageAdapter(res => ResultWrapper(res))

      def init(): Behavior[Command] =
        Behaviors.receiveMessagePartial {
          case SpawnSimulationScheduler(replyTo) =>
            val ref = context.spawn(SimulationScheduler(), "simulation-scheduler")
            replyTo ! SimulationSchedulerRef(ref)
            Behaviors.same
          case Job(jobDetails) =>
            context.watch(context.spawn(SimpleTaskQueue(jobDetails, resultMapper), "simple-queue"))
            val timeouts = jobDetails.tasks.map(task => task.id).toSet
            Behaviors.withTimers { scheduler =>
              scheduler.startSingleTimer(Tick, jobDetails.timeout)
              ready(Nil, Nil, timeouts)
            }
        }

      def ready(completed: List[Completed], failed: List[Failed], timeouts: Set[String]): Behavior[Command] =
        Behaviors.receiveMessagePartial[Command] {
          case Tick => display(completed, failed, timeouts)
          case ResultWrapper(c @ Completed(task, _)) =>
            ready(c :: completed, failed, timeouts - task.id)
          case ResultWrapper(f @ Failed(task, _, _)) =>
            ready(completed, f :: failed, timeouts - task.id)
        }.receiveSignal {
          case (context, Terminated(_)) => display(completed, failed, timeouts)
        }

      def display(completed: List[Completed], failed: List[Failed], timeouts: Set[String]): Behavior[Command] = {
        println("result.successful = " + completed.sortBy(entry => entry.durationNanos).map(_.task.id))
        println("result.failed = " + failed.sortBy(entry => entry.durationNanos).map(_.task.id))
        println("result.timedOut = " + timeouts)
        Behaviors.stopped
      }

      init()
    }
  
}
