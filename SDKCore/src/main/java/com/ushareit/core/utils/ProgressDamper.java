package com.ushareit.core.utils;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.i18n.LocaleUtils;

/**
 * an helper class used to decide when to report progress to upper layer (UI),
 * to avoid too many progress report cause UI not smoothly responsive.
 */
public final class ProgressDamper {
    private static final String TAG = "ProgressDamper";

    // minimum time (in ms) between progress report, don't report too frequently
    private static final long MIN_SILENCE_TIME = 50;
    private final long mMinSilenceTime;
    // maximum time (in ms) between progress report if progress changed
    // don't report too infrequently
    // note: if no any actual progress, don't report any progress
    private static final long MAX_SILENCE_TIME = 200;
    private final long mMaxSilenceTime;

    // total length of this task, usually it's in bytes but shouldn't always be.
    private final long mTotalLength;
    // this task's suggested minimum length to report if progress changed too frequently
    private long mSuggestedMinLengthToReport;

    private long mLastReportLength;
    private long mLastReportTime;

    private final long mStartTime;
    private final long mStartCompleted;

    /**
     * construct an ProgressDamper instance for an "task" with initial status.
     *
     * @param totalLength     the total length (in bytes) of this task
     * @param completedLength the already completed length (in bytes) of this task.
     */
    public ProgressDamper(long totalLength, long completedLength, long minSilenceTime, long maxSilenceTime) {
        mTotalLength = totalLength;
        mMinSilenceTime = minSilenceTime;
        mMaxSilenceTime = maxSilenceTime;
        mLastReportLength = mStartCompleted = completedLength;
        mLastReportTime = mStartTime = System.currentTimeMillis();

        init();
    }

    /**
     * construct an ProgressDamper instance for an "task" with initial status.
     *
     * @param totalLength     the total length (in bytes) of this task
     * @param completedLength the already completed length (in bytes) of this task.
     */
    public ProgressDamper(long totalLength, long completedLength) {
        mTotalLength = totalLength;
        mMinSilenceTime = MIN_SILENCE_TIME;
        mMaxSilenceTime = MAX_SILENCE_TIME;
        mLastReportLength = mStartCompleted = completedLength;
        mLastReportTime = mStartTime = System.currentTimeMillis();

        init();
    }

    private void init() {
        int parts = 1;
        if (mTotalLength >= 500)
            parts = 500;
        else if (mTotalLength >= 100)
            parts = 100;
        else if (mTotalLength >= 10)
            parts = 10;
        mSuggestedMinLengthToReport = mTotalLength / parts;

        if (mLastReportLength > 0)
            notifyReported(mLastReportLength);
    }

    /**
     * tell the helper current completed length and ask should report progress or not now.
     *
     * @param completed the current completed length (in bytes)
     * @return true if should report progress, false otherwise.
     * note: how to report progress and whether report or not is caller's responsibility.
     */
    public final boolean shouldReport(long completed) {
        long now = System.currentTimeMillis();
        long elapsedTime = now - mLastReportTime;
        long elapsedLength = completed - mLastReportLength;

        return (completed > 0 && mLastReportLength == 0) ||
                (completed == mTotalLength) ||
                (elapsedTime > mMaxSilenceTime && elapsedLength > 0) ||
                (elapsedTime > mMinSilenceTime && elapsedLength >= mSuggestedMinLengthToReport);
    }

    /**
     * called immediately after progress reported to upper layer (by caller).
     * this is a must.
     *
     * @param completed the extactly "reported" progress (may less than current actuall progress).
     */
    public final void notifyReported(long completed) {
        if (Logger.isDebugVersion)
            Logger.v(TAG, "report progress: time elasped = " + (System.currentTimeMillis() - mLastReportTime) + ", bytes elapsed = " + (completed - mLastReportLength));
        mLastReportLength = completed;
        mLastReportTime = System.currentTimeMillis();
    }

    /**
     * get average speed of this task.
     *
     * @param completed the current completed length (in bytes).
     * @return average speed (bytes per second), from task constrcuted till now.
     */
    public final long getAverageSpeed(long completed) {
        double seconds = (System.currentTimeMillis() - mStartTime) / 1000.0;
        long elapsedLength = completed - mStartCompleted;
        double speed = elapsedLength / seconds;
        Logger.d(TAG, LocaleUtils.formatStringIgnoreLocale("Total:%d bytes, Seconds:%.3f, AVG: %.1f bytes/s", elapsedLength, seconds, speed));
        return Math.round(speed);
    }
}
