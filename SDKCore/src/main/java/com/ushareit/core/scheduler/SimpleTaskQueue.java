package com.ushareit.core.scheduler;

import com.ushareit.core.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

// a simple task queue that only allow fixed count tasks running at one time
public final class SimpleTaskQueue implements ITaskQueue {
    private static final String TAG = "Task.Queue";

    protected final LinkedList<Task> mWaitingQueue = new LinkedList<Task>();
    protected final LinkedList<Task> mRunningQueue = new LinkedList<Task>();
    private int mMaxTaskCount;

    public SimpleTaskQueue() {
        this(1);
    }

    public SimpleTaskQueue(int maxTaskCount) {
        mMaxTaskCount = maxTaskCount;
    }

    @Override
    public void addWaitingTask(Task task) {
        synchronized (mWaitingQueue) {
            mWaitingQueue.add(task);
        }
    }

    public void addFirstWaitingTask(Task task) {
        synchronized (mWaitingQueue) {
            mWaitingQueue.addFirst(task);
        }
    }

    @Override
    public void removeWaitingTask(Task task) {
        synchronized (mWaitingQueue) {
            mWaitingQueue.remove(task);
        }
    }

    @Override
    public void removeRunningTask(Task task) {
        synchronized (mRunningQueue) {
            mRunningQueue.remove(task);
        }
    }

    @Override
    public void clearAllTasks() {
        synchronized (mWaitingQueue) {
            mWaitingQueue.clear();
        }
        synchronized (mRunningQueue) {
            for (Task task : mRunningQueue)
                task.cancel();
            mRunningQueue.clear();
        }
    }

    @Override
    public boolean shouldSchedule(Task changedTask) {
        // we don't try schedule on any task progress changed events
        // we only try schedule when task added / removed.
        return false;
    }

    @Override
    public Collection<Task> scheduleTasks() {
        Collection<Task> tasks = new ArrayList<Task>();

        synchronized (mWaitingQueue) {
            synchronized (mRunningQueue) {
                if (mWaitingQueue.size() == 0) {
                    Logger.v(TAG, "pick tasks return empty: no waiting tasks");
                    return null;
                }

                if (mRunningQueue.size() >= mMaxTaskCount) {
                    Logger.v(TAG, "pick tasks return empty: has running task");
                    return null;
                }

                Task task = mWaitingQueue.remove();
                tasks.add(task);

                mRunningQueue.addAll(tasks);
            }
        }

        return tasks;
    }

    @Override
    public Task findTask(String id) {
        if (id == null)
            return null;

        synchronized (mWaitingQueue) {
            for (Task task : mWaitingQueue) {
                if (id.equalsIgnoreCase(task.getId()))
                    return task;
            }
        }

        synchronized (mRunningQueue) {
            for (Task task : mRunningQueue) {
                if (id.equalsIgnoreCase(task.getId()))
                    return task;
            }
        }

        return null;
    }
}
