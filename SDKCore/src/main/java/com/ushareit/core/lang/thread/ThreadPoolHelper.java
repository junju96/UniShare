package com.ushareit.core.lang.thread;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.lang.HardReference;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.stats.Stats;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;


public class ThreadPoolHelper implements IAsync{
    private static final String TAG = "TaskHelper";

    @Override
    public void exec(final TaskHelper.Task task, final long backgroundDelay, final long uiDelay) {
        Assert.notNull(task);
        Assert.isTrue(backgroundDelay >= 0 && uiDelay >= 0);

        final HardReference<TaskHelper.Task> aTask = new HardReference<TaskHelper.Task>(task);

        // fix: this will improve performance, for UI task bypass thread pool will be quicker.
        if (task instanceof TaskHelper.UITask) {
            if (task.isCancelled())
                return;

            // quick accelerate patch: if already in Main UI thread then execute immediately without use handler mechinism.
            if (uiDelay == 0 && Looper.myLooper() == Looper.getMainLooper()) {
                try {
                    task.callback(null);
                } catch (Exception e) {}
                catch (Throwable tr) {
                    Stats.onError(ObjectStore.getContext(), tr);
                    Logger.e(TAG, tr);
                }
                return;
            }

            Message msg = mHandler.obtainMessage(MSG_TYPE_CALLBACK, aTask);
            // fix: some overloaded methods pass delay in backgroundDelay.
            // (UITask don't have background job, so it's impossible that caller want background delay)
            // usually only one delay parameter is non-zero, so we add them here to make either of them effective.
            long delay = uiDelay + backgroundDelay;
            mHandler.sendMessageDelayed(msg, delay);  // note: ignore return value here
            return;
        }

        try {
            task.mFuture = ThreadPollFactory.ScheduledProvider.Scheduled.schedule(new Runnable() {
                @Override
                public void run() {
                    TaskHelper.Task task = aTask.get();

                    if (task.isCancelled())
                        return;

                    try {
                        task.execute();
                    } catch (Exception e) {
                        task.mError = e;
                        Logger.w(TAG, e.toString(), e);
                        if (Logger.isDebugVersion)
                            e.printStackTrace();
                    } catch (Throwable tr) {
                        task.mError = new RuntimeException(tr);
                        Stats.onError(ObjectStore.getContext(), tr);
                        Logger.e(TAG, tr);
                    }

                    if (task.isCancelled())
                        return;

                    Message msg = mHandler.obtainMessage(MSG_TYPE_CALLBACK, aTask);
                    mHandler.sendMessageDelayed(msg, uiDelay);  // note: ignore return value here
                }
            }, backgroundDelay, TimeUnit.MILLISECONDS);
            return;
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, e.toString());
            return;
        }
    }

    @Override
    public void exec(TaskHelper.Task task, final long delay) {
        Assert.notNull(task);

        try {
            final HardReference<TaskHelper.Task> aTask = new HardReference<TaskHelper.Task>(task);

            final ExecutorService executor = ThreadPollFactory.IOProvider.IO;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e1) {}
                    }

                    TaskHelper.Task task = aTask.get();
                    if (task.isCancelled())
                        return;

                    try {
                        task.execute();
                    } catch (Exception e) {
                        task.mError = e;
                        Logger.w(TAG, e.toString(), e);
                        if (Logger.isDebugVersion)
                            e.printStackTrace();
                    } catch (Throwable tr) {
                        task.mError = new RuntimeException(tr);
                        Stats.onError(ObjectStore.getContext(), tr);
                        Logger.e(TAG, tr);
                    }

                    if (task.isCancelled())
                        return;
                    Message msg = mHandler.obtainMessage(MSG_TYPE_CALLBACK, aTask);
                    mHandler.sendMessage(msg);  // note: ignore return value here
                }
            });
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, e.toString());
        }
    }
    @Override
    public void exec(Runnable runnable, final long delay) {
        Assert.notNull(runnable);

        try {
            ThreadPollFactory.ScheduledProvider.Scheduled.schedule(runnable, delay,TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, e.toString());
        }
    }

    @Override
    public void removeMessages(int what, Object object){
        mHandler.removeMessages(what, object);
    }

    public static final int MSG_TYPE_CALLBACK = 1;
    private static Handler mHandler = new Handler(Looper.getMainLooper()) {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_TYPE_CALLBACK) {
                super.handleMessage(msg);
                return;
            }

            HardReference<TaskHelper.Task> aTask = (HardReference<TaskHelper.Task>)msg.obj;
            TaskHelper.Task task = aTask.get();
            aTask.clear();

            if (task.isCancelled())
                return;

            try {
                task.callback(task.mError);
            } catch (Exception e) {
                // note: here we catch and ignore all exceptions
                Logger.w(TAG, e.toString(), e);
                if (Logger.isDebugVersion)
                    e.printStackTrace();
            } catch (Throwable tr) {
                Stats.onError(ObjectStore.getContext(), tr);
                Logger.e(TAG, tr);
            }
        }
    };

}
