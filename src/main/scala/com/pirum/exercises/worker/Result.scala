package com.pirum.exercises.worker

object Result {
  
  sealed trait Result

  case class Completed(task: Task, durationNanos: Long) extends Result
  case class Failed(task: Task, reason: Throwable, durationNanos: Long) extends Result

}
