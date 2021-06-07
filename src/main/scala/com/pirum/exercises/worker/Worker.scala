package com.pirum.exercises.worker

import java.util.UUID

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.pirum.exercises.worker.Result._

import scala.util.Success
import scala.util.Failure

object Worker {

  sealed trait Command

  case object Stop extends Command
  case class TaskRequest(replyTo: ActorRef[Command]) extends Command
  case class TaskAssignment(task: Task) extends Command
  case class TaskResult(result: Result, token: UUID) extends Command

  def apply(queue: ActorRef[TaskRequest], replyTo: ActorRef[Result]): Behavior[Command] =
    Behaviors.setup { context =>
      def await(): Behavior[Command] = {
        queue ! TaskRequest(context.self)
        context.log.debug(s"worker ${context.self.path.name} awaiting work")
        Behaviors.receiveMessagePartial {
          case TaskAssignment(task) => work(task)
          case Stop => stop()
          case TaskResult(result, _) =>
            logLateResult(result)
            Behaviors.same
        }
      }

      def work(task: Task): Behavior[Command] = {
        val expectedToken = UUID.randomUUID()
        val start = System.nanoTime()

        context.log.debug(s"worker ${context.self.path.name} working on task $task")
        context.pipeToSelf(task.execute) {
          case Success(_) =>
            TaskResult(Completed(task, System.nanoTime() - start), expectedToken)
          case Failure(reason) =>
            TaskResult(Failed(task, reason, System.nanoTime() - start), expectedToken)
        }

        Behaviors.receiveMessagePartial {
          case TaskResult(result, actualToken) if expectedToken == actualToken =>
            replyTo ! result
            await()
          case TaskResult(result, _) =>
            logLateResult(result)
            Behaviors.same
          case Stop =>
            stop()
        }
      }

      def logLateResult(result: Result): Unit = {
        context.log.error(s"result ${result} arrived after timeout and will be ignored")
      }

      def stop(): Behavior[Command] = {
        context.log.debug(s"worker ${context.self.path.name} stopped")
        Behaviors.stopped
      }

      await()
    }
  
}
