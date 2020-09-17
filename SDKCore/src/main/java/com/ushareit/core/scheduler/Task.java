package com.ushareit.core.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class Task {
    public static final int HINT_ALREADY_COMPLETED = 1;

    protected String mId = null;        // task's unique id, (usually) shouldn't be null or empty, case insensitive
    protected Object mCookie = null;    // task's custom data (used by subclasses only)

    protected long mTotalLength = 0;    // the total amount of job
    protected long mCompletedLength = 0;// the amount of job completed so far

    protected boolean mIsSyncTask = true;		// sign the task is sync or async

    protected int mRetryCount = 0;

    protected final Object mCancelledMonitor = new Object();
    protected final AtomicBoolean mCancelled = new AtomicBoolean(false);

    public Task() {}

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public boolean isSyncTask() {
        return mIsSyncTask;
    }

    public void setIsSyncTask(boolean isSyncTask) {
        mIsSyncTask = isSyncTask;
    }

    public Object getCookie() {
        return mCookie;
    }

    public void setCookie(Object cookie) {
        mCookie = cookie;
    }

    public long getTotalLength() {
        return mTotalLength;
    }

    public void setTotalLength(long length) {
        mTotalLength = length;
    }

    public long getCompletedLength() {
        return mCompletedLength;
    }

    public void setCompletedLength(long length) {
        mCompletedLength = length;
    }

    public int getRetryCount() {
        return mRetryCount;
    }

    public void addRetryCount() {
        mRetryCount++;
    }

    public void cleanRetryCount() {
        mRetryCount = 0;
    }

    public boolean isCancelled() {
        return mCancelled.get();
    }

    public void cancel() {
        mCancelled.set(true);
        synchronized (mCancelledMonitor) {
            mCancelledMonitor.notifyAll();
        }
    }

    public void active() {
        mCancelled.set(false);
    }

    // sleep for a while (if cancelled, wake up and return immediately)
    public void sleep(long interval) {
        if (interval <= 0)
            return;
        try {
            synchronized (mCancelledMonitor) {
                mCancelledMonitor.wait(interval);
            }
        } catch (InterruptedException ie) {}
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[id = " + mId + ", length = " + mCompletedLength + "/" + mTotalLength + ", retry = " + mRetryCount + ", cancelled = " + mCancelled.get() + "]");
        return sb.toString();
    }
}
