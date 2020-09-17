package com.ushareit.core.utils;

import com.ushareit.core.Logger;

/**
 * measure a single action's performance.
 */
public final class Timing {
    private String mTag;
    private long mStart;
    private long mLastSplit;
    private String mMsg;

    /**
     * create a Timing, note the internal timer is not started yet.
     */
    public Timing() {}

    /**
     * create a Timing with specified logger TAG, note the internal timer is not started yet.
     */
    public Timing(String tag) {
        mTag = tag;
    }

    /**
     * start timer
     */
    public Timing start() {
        mStart = System.nanoTime();
        mLastSplit = mStart;
        return this;
    }
    
    /**
     * start timer, and output a log msg.
     * @param msg log msg
     */
    public Timing start(String msg) {
        mStart = System.nanoTime();
        mLastSplit = mStart;
        mMsg = msg;
        Logger.v(mTag, "START " + msg);
        return this;
    }

    /**
     * get elapsed time since last split (or from start if this is the first split).
     */
    public long split() {
        long now = System.nanoTime();
        long elapsed = (now - mLastSplit) / 1000L / 1000L;
        mLastSplit = now;
        return elapsed;
    }

    /**
     * output a log message with elapsed time since last split (or from start if this is the first split).
     */
    public void split(String msg) {
        long now = System.nanoTime();
        long elapsed = (now - mLastSplit) / 1000L / 1000L;
        mLastSplit = now;
        Logger.v(mTag, elapsed + " " + msg);
    }
    
    /**
     * get elapsed time since start, in milliseconds.
     */
    public long delta() {
        return (System.nanoTime() - mStart) / 1000L / 1000L;
    }
    
    /**
     * same as delta(msg), but with the msg inputed by constructor.
     */
    public void end() {
        long elapsed = (System.nanoTime() - mStart) / 1000L / 1000L;
        Logger.v(mTag, "END " + elapsed + " " + mMsg);
    }

    /**
     * same as deltaIfSlow but with a "SLOW" ahead of outputed msg.
     */
    public void endIfSlow(long expectedElapsedMilliseconds) {
        endIfSlow(expectedElapsedMilliseconds, mMsg);
    }

    /**
     * same as deltaIfSlow but with a "SLOW" ahead of outputed msg.
     */
    public void endIfSlow(long expectedElapsedMilliseconds, String msg) {
        long elapsed = (System.nanoTime() - mStart) / 1000L / 1000L;
        if (elapsed > expectedElapsedMilliseconds)
            Logger.v(mTag, "SLOW " + elapsed + " " + msg);
    }

    /**
     * the nanoseconds version of Timing.
     */
    public static final class TimingNano {
        public static final long NS_IN_1_MS = (1000L * 1000L);          // nanoseconds in one millisecond
        public static final long NS_IN_1_S = (1000L * 1000L * 1000L);   // nanoseconds in one second
        public static final long MS_IN_1_S = (1000L);                   // milliseconds in one second

        private long mStart;
        private long mLastSplit;

        /**
         * constructor.
         */
        public TimingNano() {}

        /**
         * start timer
         */
        public TimingNano start() {
            mStart = System.nanoTime();
            mLastSplit = mStart;
            return this;
        }

        /**
         * get elapsed time since last split (or from start if this is the first split).
         */
        public long split() {
            long now = System.nanoTime();
            long elapsed = (now - mLastSplit);
            mLastSplit = now;
            return elapsed;
        }

        /**
         * get elapsed time since start, in nanoseconds.
         */
        public long delta() {
            return (System.nanoTime() - mStart);
        }
    }
}
