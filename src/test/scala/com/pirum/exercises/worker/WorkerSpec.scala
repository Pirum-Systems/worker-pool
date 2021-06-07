package com.pirum.exercises.worker

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.pirum.exercises.worker.Result._
import com.pirum.exercises.worker.SimulationScheduler._
import com.pirum.exercises.worker.Worker._
import org.scalatest.flatspec.AnyFlatSpecLike

import scala.concurrent.duration._
import scala.util._

class WorkerSpec extends ScalaTestWithActorTestKit with AnyFlatSpecLike { 
  it should "request and process tasks" in new Fixture {
    // after init
    val worker = testKit.spawn(Worker(queue.ref, results.ref))
    queue.expectMessage(Worker.TaskRequest(worker))

    // after completing a successful task
    worker ! Worker.TaskAssignment(SimulatedTask("test-1", Success(()), 1.second, scheduler.ref))
    val success = scheduler.receiveMessage()
    success.simulation.promise.complete(success.simulation.result)
    results.expectMessageType[Completed]
    queue.expectMessage(Worker.TaskRequest(worker))

    // after completing a failed task
    worker ! Worker.TaskAssignment(SimulatedTask("test-2", Failure(new Exception("boom")), 1.second, scheduler.ref))
    val failure = scheduler.receiveMessage()
    failure.simulation.promise.complete(failure.simulation.result)
    results.expectMessageType[Failed]
    queue.expectMessage(Worker.TaskRequest(worker))
  }

  it should "stop while awaiting tasks" in new Fixture {
    val worker = testKit.spawn(Worker(queue.ref, results.ref))
    queue.expectMessage(Worker.TaskRequest(worker))
    worker ! Worker.Stop
    queue.expectTerminated(worker)
  }

  it should "stop while processing tasks" in new Fixture {
    val worker = testKit.spawn(Worker(queue.ref, results.ref))
    queue.expectMessage(Worker.TaskRequest(worker))
    worker ! Worker.TaskAssignment(SimulatedTask("test-2", Success(()), 10.second, scheduler.ref))

    scheduler.expectMessageType[Request]
    worker ! Worker.Stop

    queue.expectTerminated(worker)
  }

  it should "hang for a timed-out task" in new Fixture {
    val worker = testKit.spawn(Worker(queue.ref, results.ref))
    queue.expectMessage(Worker.TaskRequest(worker))
    worker ! Worker.TaskAssignment(SimulatedTask("test-2", Success(()), 10.second, scheduler.ref))

    scheduler.expectMessageType[Request]
    queue.expectNoMessage(5.seconds)
    worker ! Worker.Stop
    queue.expectTerminated(worker)
  }

  trait Fixture {
    val queue = testKit.createTestProbe[TaskRequest]()
    val results = testKit.createTestProbe[Result]()
    val scheduler = testKit.createTestProbe[Request]()
  }
}
