package com.ushareit.core.lang.thread.provider;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class IOExecutor extends ThreadPoolExecutor {
    //cpu个数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心线程数
    public static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 5)) * 2 + 1;
    //最大线程数
    private static final int MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;
    //线程空闲后回收时间
    private static final int KEEP_ALIVE_SECONDS = 60;

    public IOExecutor() {
        super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new DefaultThreadFactory("IO"));
    }
}
