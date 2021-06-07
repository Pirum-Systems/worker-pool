package com.pirum.exercises.worker

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import com.pirum.exercises.worker.Result._
import com.pirum.exercises.worker.SimulationScheduler._
import com.pirum.exercises.worker.Worker._
import org.scalatest.flatspec.AnyFlatSpecLike

import scala.concurrent.duration._
import scala.util._
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.matchers.should.Matchers

class SimpleTaskQueueSpec extends ScalaTestWithActorTestKit with AnyFlatSpecLike with Matchers {
  it should "spawn workers" in new Fixture {
    val testProbe = testKit.createTestProbe[String]()
    val workerFactory = (queue: ActorRef[TaskRequest]) => Behaviors.setup[Worker.Command] { context =>
      testProbe.ref ! "worker created"
      Behaviors.empty[Worker.Command]
    }

    testKit.spawn(SimpleTaskQueue(jobDetails, workerFactory))
    testProbe.receiveMessages(5) should contain theSameElementsAs(List.fill(5)("worker created"))
  }

  it should "assign tasks" in new Fixture {
    val testProbe = testKit.createTestProbe[String]()
    val workerFactory = (queue: ActorRef[TaskRequest]) => Behaviors.setup[Worker.Command] { context =>
      queue ! TaskRequest(context.self)
      Behaviors.receiveMessagePartial {
        case TaskAssignment(task) => testProbe.ref ! task.id
        Behaviors.same
      }
    }

    testKit.spawn(SimpleTaskQueue(jobDetails, workerFactory))
    val expectedTaskIds = List("test-1", "test-2", "test-3", "test-4", "test-5")
    testProbe.receiveMessages(5) should contain theSameElementsAs(expectedTaskIds)
  }

  it should "stop workers when no tasks available" in new Fixture {
    val testProbe = testKit.createTestProbe[String]()
    val workerFactory = (queue: ActorRef[TaskRequest]) => Behaviors.setup[Worker.Command] { context =>
      queue ! TaskRequest(context.self)
      Behaviors.receiveMessagePartial {
        case Worker.Stop => testProbe.ref ! "stopped"
        Behaviors.stopped
      }
    }

    val jobDetalsNoTasks = jobDetails.copy(tasks = Nil)
    testKit.spawn(SimpleTaskQueue(jobDetalsNoTasks, workerFactory))
    testProbe.receiveMessages(5) should contain theSameElementsAs(List.fill(5)("stopped"))
  }

  trait Fixture {
    val queue = testKit.createTestProbe[TaskRequest]()
    val results = testKit.createTestProbe[Result]()
    val scheduler = testKit.createTestProbe[Request]()
    val jobDetails = JobDetails(
      tasks = List(
        SimulatedTask("test-1", Success(()), 1.second, scheduler.ref),
        SimulatedTask("test-2", Success(()), 1.second, scheduler.ref),
        SimulatedTask("test-3", Success(()), 1.second, scheduler.ref),
        SimulatedTask("test-4", Success(()), 1.second, scheduler.ref),
        SimulatedTask("test-5", Success(()), 1.second, scheduler.ref)
      ),
      timeout = 10.seconds,
      workers = 5
    )
  }
  
}
