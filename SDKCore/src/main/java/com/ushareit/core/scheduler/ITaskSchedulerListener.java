package com.ushareit.core.scheduler;

public interface ITaskSchedulerListener {
    /**
     * called before execute task, return false to stop subsequent executing.
     * if have multiple listeners, one vote reject (veto)
     */
    boolean onPrepare(Task task);

    /**
     * called when task progress changed
     */
    void onProgress(Task task, long total, long completed);

    /**
     * called when task stopped due to successfully completed
     * @param task
     * @param alreadyExists true means this task completed because its data already exists
     */
    void onCompleted(Task task, int hint);

    /**
     * called when task stopped due to some error occurred
     * @return return true to retry, return false to stop executing.
     */
    boolean onError(Task task, Exception error);
}
