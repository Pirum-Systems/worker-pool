package com.pirum.exercises.worker

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import com.pirum.exercises.worker.Worker.TaskAssignment
import com.pirum.exercises.worker.Worker.TaskRequest

object SimpleTaskQueue {

  sealed trait Command

  case class RequestWrapper(request: TaskRequest) extends Command
  case object Stop extends Command

  def apply(
    jobDetails: JobDetails,
    workerFactory: ActorRef[TaskRequest] => Behavior[Worker.Command]
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val taskRequestMapper: ActorRef[TaskRequest] = context.messageAdapter(req => RequestWrapper(req))
      (0 until jobDetails.workers).map { id =>
        context.watch(context.spawn(workerFactory(taskRequestMapper), s"worker-$id"))
      }

      def ready(tasks: List[Task], workers: Int): Behavior[Command] =
        Behaviors.receiveMessagePartial[Command] { 
          case RequestWrapper(TaskRequest(replyTo)) if tasks.isEmpty =>
            replyTo ! Worker.Stop
            Behaviors.same
          case RequestWrapper(TaskRequest(replyTo)) =>
            val (head :: tail) = tasks
            replyTo ! TaskAssignment(head)
            ready(tail, workers)
          case Stop =>
            Behaviors.stopped
        }.receiveSignal {
          case (ctx, Terminated(_)) if workers > 1 =>
            ready(tasks, workers - 1)
          case (ctx, Terminated(_)) =>
            Behaviors.stopped
        }

      ready(jobDetails.tasks, jobDetails.workers)
    }
  
}
