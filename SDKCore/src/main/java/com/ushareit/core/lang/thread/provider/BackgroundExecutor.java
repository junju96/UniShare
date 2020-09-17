package com.ushareit.core.lang.thread.provider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor extends ThreadPoolExecutor {
    //cpu个数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心线程数
    public static final int CORE_POOL_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    //最大线程数
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 2;
    //线程空闲后回收时间
    private static final int KEEP_ALIVE_SECONDS = 30;
    //阻塞队列
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(128);
    //异常处理机制
    private static final RejectedExecutionHandler sHandler = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Executors.newCachedThreadPool().execute(r);
        }
    };


    public BackgroundExecutor() {
        super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, sPoolWorkQueue,new DefaultThreadFactory("Background"), sHandler);
        allowCoreThreadTimeOut(true);
    }
}
