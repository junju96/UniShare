package com.ushareit.core.lang.thread;

import android.os.HandlerThread;
import android.os.Looper;
import com.ushareit.core.lang.thread.provider.IOExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public final class ThreadPollFactory {
    /**
     * 全局线程池提供者
     * 核心线程与cpu相关，不限制最大线程数
     */
    public static final class IOProvider {
        public static final ThreadPoolExecutor IO;
        static {
            IO = new IOExecutor();
        }
    }
    /**
     * 可调度的线程池提供者
     * 核心线程5个
     */
    public static final class ScheduledProvider {
        public static final ScheduledExecutorService Scheduled;
        static {
            Scheduled = Executors.newScheduledThreadPool(5);
        }
    }
    /**
     * 为采集提供线程池
     * 核心线程1个
     */
    public static final class AnalyticsProvider{
        public static final Executor Single;
        static {
            Single = Executors.newSingleThreadExecutor();
        }
    }

    public static final class ThreadLooperProvider{
        public static final Looper ThreadLooper;
        static {
            HandlerThread handlerThread = new HandlerThread("ThreadLooperProvider");
            handlerThread.start();
            ThreadLooper = handlerThread.getLooper();
        }
    }
}
