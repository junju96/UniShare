package com.ushareit.core.scheduler;

// task executor: how to execute a task
public interface ITaskExecutor {
    /**
     * execute task synchronously
     * @param task
     * @throws any exception if failed, don't throw exception if cancelled
     */
    void execute(Task task) throws Exception;
}
