package com.pirum.exercises.worker

import akka.actor.typed.ActorSystem
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._

object Main extends App with Program {

  /* PLEASE READ THE NOTES IN README */

  import Controller._
  import akka.actor.typed.scaladsl.AskPattern._

  implicit val system: ActorSystem[Command] = ActorSystem(Controller(), "root")
  implicit val ec: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)
  implicit val simulation: SimulationSchedulerRef =
    Await.result(system.ask(Controller.SpawnSimulationScheduler(_)), 3.seconds)

  val demo = List(
    Task.succeed("Task1", 5.seconds),
    Task.succeed("Task2", 4.seconds),
    Task.succeed("Task3", 3.seconds),
    Task.hang("Task4"),
    Task.succeed("Task5", 1.seconds),
    Task.fail("Task6", new Exception("boom"), 5.seconds),
    Task.succeed("Task7", 4.seconds),
    Task.succeed("Task8", 3.seconds),
    Task.succeed("Task9", 2.seconds),
    Task.fail("Task10", new Exception("boom"), 1.seconds),
    Task.succeed("Task11", 5.seconds),
    Task.succeed("Task12", 4.seconds),
    Task.hang("Task13"),
    Task.succeed("Task14", 2.seconds),
    Task.succeed("Task15", 1.seconds),
  )

  def program(tasks: List[Task], timeout: FiniteDuration, workers: Int): Unit = {
    system ! Job(JobDetails(tasks, timeout, workers))
  }
  
  program(demo, timeout = 10.seconds, workers = 8)
  println("Good luck 🤓")
  
}
