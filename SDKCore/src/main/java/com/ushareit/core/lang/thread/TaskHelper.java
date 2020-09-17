package com.ushareit.core.lang.thread;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public final class TaskHelper {
    private static final String TAG = "TaskHelper";

    private static IAsync mAsync = new ThreadPoolHelper();

    /**
     * 创建RxJava帮助者
     */
//    public static void createRxjavaHelper(){
//        mAsync = new RxJavaHelper();
//    }
    /**
     * 执行一个任务
     * @param task 任务
     */
    public static Task execZForSDK(Task task) {
        return execZForSDK(task, 0);
    }
    /**
     * 执行一个任务
     * @param task 任务
     */
    public static Task exec(Task task) {
        return exec(task, 0, 0);
    }
    /**
     * 延迟执行一个任务
     * 若用线程池助手，使用{@link ThreadPollFactory.ScheduledProvider}线程池
     * @param task
     * @param delay 延迟时间，单位毫秒
     */
    public static Task execZForSDK(Task task, long delay) {
        mAsync.exec(task, delay);
        return task;
    }
    /**
     * 延迟执行一个任务
     * 若用线程池助手，使用{@link ThreadPollFactory.IOProvider}线程池
     * @param task
     * @param delay 延迟时间，单位毫秒
     */
    public static Task exec(Task task,long delay) {
        return exec(task, delay, 0);
    }
    /**
     * 延迟执行一个任务
     * @param task
     * @param backgroundDelay 后台线程延迟时间，单位毫秒
     * @param uiDelay 主线程延迟执行时间，单位毫秒
     */
    public static Task exec(Task task, long backgroundDelay, long uiDelay) {
        mAsync.exec(task, backgroundDelay, uiDelay);
        return task;
    }
    /**
     * 延迟执行一个runnable
     * @param runnable
     * @param delay 延迟时间，单位毫秒
     */
    public static void exec(Runnable runnable, final long delay) {
        mAsync.exec(runnable, delay);
    }
    /**
     * 执行一个Runnable
     * @param runnable
     */
    public static void exec(Runnable runnable) {
        execByIoThreadPoll(runnable);
    }
    /**
     * 执行一个Runnable
     * @param runnable
     */
    public static void execZForSDK(Runnable runnable) {
        execByIoThreadPoll(runnable);
    }
    /**
     * 执行一个带名字Runnable
     * @param runnableWithName
     */
    public static void execZForSDK(RunnableWithName runnableWithName) {
        execByIoThreadPoll(runnableWithName);
    }
    /**
     * 执行一个带名字Runnable
     * @param runnableWithName
     */
    public static void execZForUI(RunnableWithName runnableWithName) {
        execByIoThreadPoll(runnableWithName);
    }
    /**
     * 执行一个带名字Runnable，通过io线程池
     * @param runnableWithName
     */
    public static void execByIoThreadPoll(RunnableWithName runnableWithName){
        Assert.notNull(runnableWithName);
        execByIoThreadPoll(runnableWithName.getRunnable());
    }
    /**
     * 执行一个Runnable，通过io线程池
     * @param runnable
     */
    private static void execByIoThreadPoll(Runnable runnable){
        Assert.notNull(runnable);

        try {
            ThreadPollFactory.IOProvider.IO.submit(runnable);
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, e.toString());
        }
    }
    /**
     * 执行采集，通过独立的线程池
     * @param runnable
     */
    public static void execZForAnalytics(RunnableWithName runnable) {
        Assert.notNull(runnable);

        try {
            ThreadPollFactory.AnalyticsProvider.Single.execute(runnable.getRunnable());
        } catch (Exception e) {
            Logger.w(TAG, e.toString());
        }
    }

    /**
     * 带名字的Runnable
     */
    public static abstract class RunnableWithName implements Runnable{
        private String mName;

        public RunnableWithName(String name) {
            mName = name;
        }

        @Override
        public void run() {
            if (mName != null) {
                Thread.currentThread().setName(mName);
            }
            execute();
        }

        public Runnable getRunnable() {
            return this;
        }

        public abstract void execute();
    }
    /**
     * 在子线程中运行的任务
     */
    public static abstract class Task {

        public abstract void execute() throws Exception;

        public abstract void callback(Exception e);

//        public CompositeDisposable mCompositeDisposable;

        public Future<?> mFuture;
        public boolean mCancelled;
        public Exception mError;

//        private boolean isRxJavaRun;

        public Task() {
//            mCompositeDisposable = new CompositeDisposable();
//            isRxJavaRun = mAsync instanceof RxJavaHelper;
        }


        public final boolean isCancelled() {
//            if(isRxJavaRun) {
//                return mCompositeDisposable.isDisposed();
//            }else{
                return mCancelled;
//            }
        }

        public void cancel() {
//            if(isRxJavaRun) {
//                if (!isCancelled())
//                    mCompositeDisposable.dispose();
//            }else{
                mCancelled = true;
                try {
                    if (mFuture != null)
                        mFuture.cancel(true);
                } catch (Exception e) {
                    Logger.w(TAG, e.toString());
                }
                mAsync.removeMessages(ThreadPoolHelper.MSG_TYPE_CALLBACK, this);
//            }
        }
    }
    /**
     * 在主线程中运行的任务
     */
    public static abstract class UITask extends Task {
        @Override
        public void execute() {}
    }
}
