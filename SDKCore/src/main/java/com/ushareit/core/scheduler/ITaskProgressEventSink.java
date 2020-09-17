package com.ushareit.core.scheduler;

/**
 * implemented by task scheduler, called by task executor to report task progress to its scheduler.
 */
public interface ITaskProgressEventSink {
    /**
     * called when task progress changed
     */
    void onTaskProgressMade(Task task, long total, long completed);
}
