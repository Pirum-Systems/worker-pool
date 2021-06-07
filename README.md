# Notes
- The queue and worker implementation does not assume anything about the internals of the provided tasks. You can use your own task implementation and all should still work fine. The original interface is preserved, I only added the text ID field (since the examples seem to show text IDs as well, e.g. _Task1_).
- The simulated time to completion of the task definitions is intentionally hidden away from the queue/worker impl. and the workers properly measure the execution time of each task in nanoseconds from the invocation of the execute method until the _Future_ returned by the method returns. Ordering of the results is based on those values.
- There is an actor-based task simulation impl. available, but does not have to be used.

## Open Points
- ```
  When there is no hanging task, the program should return after a time not significantly longer than the duration of the longest running task. For example, when the longest running task takes 3 seconds to execute, it should return after 3 seconds + maybe some small additional time.
  ```
  I was not quite sure about this requirement as it seems to me it depends on the amount of workers. If there's just a single worker configured, it would still process the tasks in a serial fashion, instead of parallel. The higher the parallelism, the closer the overall runtime would be to the completion time of the longest task.
- ```
  It is forbidden to use busy loops and Thread.sleep; all waits and blocks should be realized with proper methods.
  ```
  I was also not sure about this one. I used _Promises_ instead of something that "properly" creates CPU load. Hope that's still ok though.
- The task description and requirements mention setting up timeouts. However it was not specified, whether these should be global (counting down starts before the first task is executed), or whether it should be a fresh timeout for every task individually. I went for the gobal timetout.

Please let me know if any of the above need to be fixed.

# Workers

**In order to submit your work, please fork this repository into your own account instead of raising a PR against this one. Thank you.**

Implement a program that dispatches tasks to a set of workers, collects and displays their result in the right order. The workers will concurrently execute these tasks and return the summary of the execution with tasks categorized as successful, failed or timed out.

You are free to represent tasks any way you want as long as they expose the desired behaviour. Likewise, you can use any concurrency pattern you are familiar with.

For example, the following input:

```
actions=[Task3(throws after 3s), Task4(compl. after 4s), Task2(compl. after 2s), Task1(throws after 1s)]
timeout=8s
workers=4
```

Should return the following result after 4 seconds:

```
result.successful = [Task2, Task4]
result.failed = [Task1, Task3]
result.timedOut = []
```

And the following input:

```
actions=[Task3(throws after 3s), Task5(hangs), Task4(compl. after 4s), Task2(compl. after 2s), Task1(compl. after 1s)]
timeout=8s
workers=4
```

Should return following result after 8 seconds:

```
result.successful = [Task1, Task2, Task4]
result.failed = [Task3]
result.timedOut = [Task5]
```

## Requirements:

- You need to execute tasks before timeout (given as a parameter). Collective duration of all the tasks will surely exceed the timeout, so you cannot execute tasks iteratively one after another.
- A worker can only work on one task at a time and cannot be flooded with more tasks than it can handle.
- There will be something between 25 and 60 tasks to execute.
- Some of the tasks will work for some time and then finish. Other will fail and its run method will throw an unspecified exception. It can also happen that task hangs and does not return in short time. The program needs to categorize each passed task as successful, failed or timed out.
- Tasks must be returned in the order of their duration. It's guaranteed that when all tasks are started simultanoussly, each successful task will end in some distinct point in time and there should be no two tasks that finish at the same moment. Order of timed out tasks is not important.
- When there is no hanging task, the program should return after a time not significantly longer than the duration of the longest running task. For example, when the longest running task takes 3 seconds to execute, it should return after 3 seconds + maybe some small additional time.
- It is forbidden to use busy loops and `Thread.sleep`; all waits and blocks should be realized with proper methods.
