package com.ushareit.core.scheduler;

// manages tasks, the add/remove, start/monitor/retry, etc
// and meanwhile post task status/progress events to listeners
public interface ITaskScheduler {

    /**
     * add task to waiting queue, schedule it if necessary
     * @param task
     */
    void add(Task task);

    /**
     * remove task
     * if already running, will cancel it (may fire events)
     * if not running yet, will not fire events.
     * @param task
     */
    void remove(Task task);

    /**
     * remove all tasks, cancel all running tasks.
     */
    void clear();

    /**
     * find task by its id, must have id assigned before
     * @param id task id, case insensitive, shouldn't be null or empty
     * @return return found task with specified id, return null if not found
     */
    Task find(String id);

    void addListener(ITaskSchedulerListener listener);

    void removeListener(ITaskSchedulerListener listener);
}
