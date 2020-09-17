package com.ushareit.core.net;

public class ServerTimeManager {

    private static ServerTimeManager instance;
    private long mLastServiceTime;
    private long mTimeDelta;

    private ServerTimeManager() {
    }

    public static ServerTimeManager getInstance() {
        if (instance == null) {
            synchronized (ServerTimeManager.class) {
                if (instance == null) {
                    instance = new ServerTimeManager();
                }
            }
        }
        return instance;
    }

    /**
     * @return the service current time(unit millis) and
     * return the local time when there is no server time.
     * return last server time when local time error
     */
    public synchronized long getServerTimestamp() {
        if (mLastServiceTime <= 0) {
            // return local time when there is no server time
            return System.currentTimeMillis();
        }
        long nowServerTime = System.currentTimeMillis() + mTimeDelta;
        if (nowServerTime < mLastServiceTime) {
            return mLastServiceTime;
        }
        return nowServerTime;
    }

    /**
     * update server time
     * @param serverTime now server utc time (unit millis）
     */
    public synchronized void updateServerTime(long serverTime) {
        if (serverTime > 0) {
            mLastServiceTime = serverTime;
            mTimeDelta = mLastServiceTime - System.currentTimeMillis();
        }
    }
}
