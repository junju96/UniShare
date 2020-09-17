//package com.ushareit.core.lang.thread;
//
//import android.os.Looper;
//
//import com.ushareit.base_common.BuildConfig;
//import com.ushareit.core.Assert;
//import com.ushareit.core.Logger;
//import com.ushareit.core.lang.ObjectStore;
//import com.ushareit.core.stats.Stats;
//
//import java.util.concurrent.TimeUnit;
//
//import io.reactivex.Observable;
//import io.reactivex.android.schedulers.AndroidSchedulers;
//import io.reactivex.functions.Consumer;
//import io.reactivex.schedulers.Schedulers;
//
//public class RxJavaHelper implements IAsync{
//    private static final String TAG = "TaskHelper";
//
//    /**
//     * 延迟执行一个Runnable，通过RxJava
//     * @param runnable 可运行的
//     * @param delay 延迟时间，单位毫秒
//     */
//    @Override
//    public void exec(final Runnable runnable, final long delay){
//        Assert.notNull(runnable);
//        Schedulers
//            .from(ThreadPollFactory.IOProvider.IO)
//            .scheduleDirect(new Runnable() {
//                @Override
//                public void run() {
//                    if(BuildConfig.DEBUG) {
//                        Logger.d(TAG, "exec runnable = " + Thread.currentThread().getName());
//                    }
//                    try {
//                        runnable.run();
//                    }catch (Throwable throwable){
//                        Stats.onError(ObjectStore.getContext(), throwable);
//                    }
//                }
//        }, delay, TimeUnit.MILLISECONDS);
//    }
//
//    @Override
//    public void exec(TaskHelper.Task task, long delay) {
//        exec(task, delay, 0);
//    }
//
//    @Override
//    public void exec(final TaskHelper.Task task, final long backgroundDelay, final long uiDelay){
//        Assert.notNull(task);
//        Assert.isTrue(backgroundDelay >= 0 && uiDelay >= 0);
//
//        long delay = uiDelay + backgroundDelay;
//
//        if(task instanceof TaskHelper.UITask){
//            if (uiDelay == 0 && Looper.myLooper() == Looper.getMainLooper()) {
//                if(BuildConfig.DEBUG) {
//                    Logger.d(TAG, " exec UITask in main thread");
//                }
//                callTaskCallback(task, null);
//            }else {
//                task.mCompositeDisposable.add(
//                    AndroidSchedulers.mainThread()
//                        .scheduleDirect(new Runnable() {
//                            @Override
//                            public void run() {
//                                if(BuildConfig.DEBUG) {
//                                    Logger.d(TAG, " exec UITask in sub thread begin");
//                                }
//                                if (task.isCancelled())
//                                    return;
//                                callTaskCallback(task, null);
//                                if(BuildConfig.DEBUG) {
//                                    Logger.d(TAG, " exec UITask in sub thread end");
//                                }
//                            }
//                        }, delay, TimeUnit.MILLISECONDS));
//            }
//        }else {
//            task.mCompositeDisposable.add(
//                Observable
//                    .timer(delay, TimeUnit.MILLISECONDS, Schedulers.from(ThreadPollFactory.IOProvider.IO))
//                    .doOnNext(new Consumer<Long>() {
//                        @Override
//                        public void accept(Long aLong) throws Exception {
//                            if(BuildConfig.DEBUG) {
//                                Logger.d(TAG, "exec task accept = " + Thread.currentThread().getName());
//                            }
//                            task.execute();
//                        }
//                    })
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Consumer<Long>() {
//                            @Override
//                            public void accept(Long aLong) throws Exception {
//                                if(BuildConfig.DEBUG) {
//                                    Logger.d(TAG, "exec task onSuccess isMain = " + (Looper.myLooper() == Looper.getMainLooper()));
//                                }
//                                task.callback(null);
//                            }
//                        }, new Consumer<Throwable>() {
//                            @Override
//                            public void accept(Throwable tr) throws Exception {
//                                if(BuildConfig.DEBUG) {
//                                    Logger.e(TAG, "exec task onError isMain = " + (Looper.myLooper() == Looper.getMainLooper()));
//                                }
//                                task.callback(new RuntimeException(tr));
//                                Stats.onError(ObjectStore.getContext(), tr);
//                        }
//                    })
//            );
//        }
//    }
//
//    @Override
//    public void removeMessages(int what, Object object) {
//
//    }
//
//    private void callTaskCallback(TaskHelper.Task task, Throwable throwable){
//        try {
//            RuntimeException exception = throwable == null ? null : new RuntimeException(throwable);
//            task.callback(exception);
//        }catch (Throwable tr) {
//            Stats.onError(ObjectStore.getContext(), tr);
//            Logger.e(TAG, tr);
//        }
//    }
//}
