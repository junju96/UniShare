package com.ushareit.core.scheduler;

import java.util.Collection;

/**
 * task queue, implement task queue management and schedule policy.
 * note: all methods must be multiple-thread safe
 */
public interface ITaskQueue {
    void addWaitingTask(Task task);

    void removeWaitingTask(Task task);

    void removeRunningTask(Task task);

    void clearAllTasks();

    Task findTask(String taskId);

    // called after one task progress changed
    // return true if should try schedule new tasks, otherwise return false
    // note: this will only control if should schedule when task progress changed.
    //       if any task was added/removed, a schedule attempt will always be forced.
    boolean shouldSchedule(Task changedTask);

    // schedule tasks to running queue. (move from waiting queue to running queue, in an atomic operation)
    // return the tasks just added to running queue (don't include those already in running queue before call this method)
    // the schedule policy may depends on: Waiting Queue, Running Queue
    Collection<Task> scheduleTasks();
}
