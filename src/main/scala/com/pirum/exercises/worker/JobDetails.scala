package com.pirum.exercises.worker

import scala.concurrent.duration.FiniteDuration

final case class JobDetails (
  tasks: List[Task],
  timeout: FiniteDuration,
  workers: Int
)
