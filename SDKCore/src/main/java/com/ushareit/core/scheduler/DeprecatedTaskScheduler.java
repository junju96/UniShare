package com.ushareit.core.scheduler;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.lang.thread.TaskHelper.RunnableWithName;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Deprecated
public class DeprecatedTaskScheduler implements ITaskScheduler {
    private static final String TAG = "Task.Scheduler";

    protected ITaskExecutor mTaskExecutor = null;
    protected ITaskQueue mTaskQueue = null;

    protected final List<ITaskSchedulerListener> mListeners = new CopyOnWriteArrayList<ITaskSchedulerListener>();

    protected String mSchedulerName;

    public DeprecatedTaskScheduler(String name) {
        mSchedulerName = name;
    }

    protected final void setTaskExecutor(ITaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    protected final void setTaskQueue(ITaskQueue taskQueue) {
        mTaskQueue = taskQueue;
    }

    @Override
    public final void add(Task task) {
        Assert.isFalse(task.isCancelled());
        Logger.d(TAG, "task added: " + task.toString());

        mTaskQueue.addWaitingTask(task);
        schedule();
    }

    @Override
    public final void remove(Task task) {
        Logger.d(TAG, "task removed: " + task.toString());

        task.cancel();
        mTaskQueue.removeWaitingTask(task);
        schedule();
    }

    @Override
    public final Task find(String id) {
        return mTaskQueue.findTask(id);
    }
    
    @Override
    public final void clear() {
        Logger.d(TAG, "tasks cleared");
        mTaskQueue.clearAllTasks();
    }

    // this method will be called from multiple threads
    protected final void schedule() {
        Collection<Task> tasks = mTaskQueue.scheduleTasks();
        if (tasks == null || tasks.isEmpty())
            return;

        Logger.d(TAG, "scheduling " + tasks.size() + " tasks");

        for (final Task task : tasks) {
            TaskHelper.execByIoThreadPoll(new RunnableWithName(mSchedulerName) {
                @Override
                public void execute() {
                    boolean retry = false;
                    try {
                        retry = executeTask(task);
                    } finally {
                        if (retry) {
                            task.mRetryCount++;
                            DeprecatedTaskScheduler.this.add(task);
                        }
                        schedule();
                    }
                }
            });
        }
    }

    // return true if want retry, otherwise return false
    protected final boolean executeTask(Task task) {
        boolean alreadyCompleted = false;
        boolean hasError = false;
        try {
            boolean accepted = fireOnPrepare(task);
            if (!accepted) {
                Logger.d(TAG, "prepare task failed: " + task.toString());
                hasError = true;
                return false;
            }

            Assert.isTrue(task.getTotalLength() >= 0);
            Assert.isTrue(task.getCompletedLength() <= task.getTotalLength());

            // should process in normal when file size is 0. otherwise, receive file is not exist and sent side do not show progress.
            alreadyCompleted = (task.getCompletedLength() == task.getTotalLength() && task.getTotalLength() != 0);
            int hint = Task.HINT_ALREADY_COMPLETED;
            if (!alreadyCompleted) {
                Logger.d(TAG, "executing task: " + task.toString());
                hint = 0;
                mTaskExecutor.execute(task);
                Logger.d(TAG, "task completed: " + task.toString());
                if (task.isSyncTask())
                    alreadyCompleted = true;
            }

            if (alreadyCompleted)
                fireOnCompleted(task, hint);
            return false;
        } catch (Exception e) {
            hasError = true;
            boolean retry = fireOnError(task, e);
            Logger.w(TAG, "task execute failed: retry = " + retry + ", error = " + e.toString() + ", task = " + task.toString());
            return retry;
        } finally {
            // Only one case don't remove running task,
            // the task should be async task and the has no error and not completed when execute.
            if (alreadyCompleted || hasError)
                mTaskQueue.removeRunningTask(task);
        }
    }

    @Override
    public final void addListener(ITaskSchedulerListener listener) {
        mListeners.add(listener);
    }

    @Override
    public final void removeListener(ITaskSchedulerListener listener) {
        mListeners.remove(listener);
    }

    // private implementations
    
    protected boolean fireOnPrepare(Task task) {
        for (ITaskSchedulerListener listener : mListeners) {
            boolean accepted = false;
            try {
                accepted = listener.onPrepare(task);
            } catch (Exception e) {
                Logger.w(TAG, e);
            }
            if (!accepted)
                return false;
        }
        return true;
    }

    protected void fireOnProgress(Task task, long total, long completed) {
        for (ITaskSchedulerListener listener : mListeners) {
            try {
                listener.onProgress(task, total, completed);
            } catch (Exception e) {
                Logger.w(TAG, e);
            }
        }

        // check if should schedule when task progress changed (for advanced schedule policy)
        if (mTaskQueue.shouldSchedule(task))
            schedule();
    }

    protected void fireOnCompleted(Task task, int hint) {
        for (ITaskSchedulerListener listener : mListeners) {
            try {
                listener.onCompleted(task, hint);
            } catch (Exception e) {
                Logger.w(TAG, e);
            }
        }
    }

    protected boolean fireOnError(Task task, Exception t) {
        boolean retry = false;
        for (ITaskSchedulerListener listener : mListeners) {
            try {
                if (listener.onError(task, t))
                    retry = true;
            } catch (Exception e) {
                Logger.w(TAG, e);
            }
        }
        return retry;
    }
}
