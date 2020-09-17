package com.ushareit.core.lang.thread;

import com.ushareit.core.lang.thread.TaskHelper;

public interface IAsync {
    void exec(Runnable runnable, final long delay);

    void exec(TaskHelper.Task task, long delay);

    void exec(TaskHelper.Task task, long backgroundDelay, long uiDelay);

    void removeMessages(int what, Object object);
}
