package com.ushareit.core.services;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobServiceEngine;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ushareit.core.Logger;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class BackgroundService extends Service {
    static final String TAG = "BackgroundService";

    CompatJobEngine mJobImpl;
    WorkEnqueuer mCompatWorkEnqueuer;
    CommandProcessor mCurProcessor;
    boolean mInterruptIfStopped = false;
    boolean mStopped = false;
    boolean mDestroyed = false;

    final ArrayList<CompatWorkItem> mCompatQueue;

    static final Object sLock = new Object();
    static final HashMap<ComponentName, WorkEnqueuer> sClassWorkEnqueuer = new HashMap<ComponentName, WorkEnqueuer>();

    /**
     * Base class for the target service we can deliver work to and the implementation of
     * how to deliver that work.
     */
    abstract static class WorkEnqueuer {
        final ComponentName mComponentName;

        boolean mHasJobId;
        int mJobId;

        WorkEnqueuer(Context context, ComponentName cn) {
            mComponentName = cn;
        }

        void ensureJobId(int jobId) {
            if (!mHasJobId) {
                mHasJobId = true;
                mJobId = jobId;
            } else if (mJobId != jobId) {
//                throw new IllegalArgumentException("Given job ID " + jobId
//                        + " is different than previous " + mJobId);
            }
        }

        abstract void enqueueWork(Intent work);

        public void serviceStartReceived() {
        }

        public void serviceProcessingStarted() {
        }

        public void serviceProcessingFinished() {
        }
    }

    /**
     * Get rid of lint warnings about API levels.
     */
    interface CompatJobEngine {
        IBinder compatGetBinder();
        GenericWorkItem dequeueWork();
    }

    /**
     * An implementation of WorkEnqueuer that works for pre-O (raw Service-based).
     */
    static final class CompatWorkEnqueuer extends WorkEnqueuer {
        private final Context mContext;
        private final PowerManager.WakeLock mLaunchWakeLock;
        private final PowerManager.WakeLock mRunWakeLock;
        boolean mLaunchingService;
        boolean mServiceProcessing;

        CompatWorkEnqueuer(Context context, ComponentName cn) {
            super(context, cn);
            mContext = context.getApplicationContext();
            // Make wake locks.  We need two, because the launch wake lock wants to have
            // a timeout, and the system does not do the right thing if you mix timeout and
            // non timeout (or even changing the timeout duration) in one wake lock.
            PowerManager pm = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            mLaunchWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    cn.getClassName() + ":launch");
            mLaunchWakeLock.setReferenceCounted(false);
            mRunWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    cn.getClassName() + ":run");
            mRunWakeLock.setReferenceCounted(false);
        }

        @Override
        void enqueueWork(Intent work) {
            try {
                Intent intent = new Intent(work);
                intent.setComponent(mComponentName);
                Logger.d(TAG, "Starting service for work: " + work);
                if (mContext.startService(intent) != null) {
                    synchronized (this) {
                        if (!mLaunchingService) {
                            mLaunchingService = true;
                            if (!mServiceProcessing) {
                                // If the service is not already holding the wake lock for
                                // itself, acquire it now to keep the system running until
                                // we get this work dispatched.  We use a timeout here to
                                // protect against whatever problem may cause it to not get
                                // the work.
                                try {
                                    mLaunchWakeLock.acquire(60 * 1000);
                                } catch (Exception e) {

                                }
                            }
                        }
                    }
                }
            }catch (Throwable t){

            }
        }

        @Override
        public void serviceStartReceived() {
            synchronized (this) {
                // Once we have started processing work, we can count whatever last
                // enqueueWork() that happened as handled.
                mLaunchingService = false;
            }
        }

        @Override
        public void serviceProcessingStarted() {
            synchronized (this) {
                // We hold the wake lock as long as the service is processing commands.
                if (!mServiceProcessing) {
                    mServiceProcessing = true;
                    // Keep the device awake, but only for at most 10 minutes at a time
                    // (Similar to JobScheduler.)
                    try {
                        mRunWakeLock.acquire(10 * 60 * 1000L);
                        mLaunchWakeLock.release();
                    }catch (Exception e){

                    }
                }
            }
        }

        @Override
        public void serviceProcessingFinished() {
            synchronized (this) {
                try {
                    if (mServiceProcessing) {
                        // If we are transitioning back to a wakelock with a timeout, do the same
                        // as if we had enqueued work without the service running.
                        if (mLaunchingService) {
                            mLaunchWakeLock.acquire(60 * 1000);
                        }
                        mServiceProcessing = false;
                        mRunWakeLock.release();
                    }
                }catch (Throwable t){

                }
            }
        }
    }

    /**
     * Implementation of a JobServiceEngine for interaction with JobIntentService.
     */
    @RequiresApi(26)
    static final class JobServiceEngineImpl extends JobServiceEngine
            implements CompatJobEngine {
        static final String TAG = "JobServiceEngineImpl";

        final BackgroundService mService;
        final Object mLock = new Object();
        JobParameters mParams;

        final class WrapperWorkItem implements GenericWorkItem {
            final JobWorkItem mJobWork;

            WrapperWorkItem(JobWorkItem jobWork) {
                mJobWork = jobWork;
            }

            @Override
            public Intent getIntent() {
                return mJobWork.getIntent();
            }

            @Override
            public void complete() {
                try {
                    synchronized (mLock) {
                        if (mParams != null) {
                            mParams.completeWork(mJobWork);
                        }
                    }
                }catch (Exception e){
                    Logger.d(TAG," complete E = " + e.toString());
                }
            }
        }

        JobServiceEngineImpl(BackgroundService service) {
            super(service);
            mService = service;
        }

        @Override
        public IBinder compatGetBinder() {
            return getBinder();
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            Logger.d(TAG, "onStartJob: " + params);
            mParams = params;
            // We can now start dequeuing work!
            mService.ensureProcessorRunningLocked(false);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Logger.d(TAG, "onStopJob: " + params);
            boolean result = mService.doStopCurrentWork();
            synchronized (mLock) {
                // Once we return, the job is stopped, so its JobParameters are no
                // longer valid and we should not be doing anything with them.
                mParams = null;
            }
            return result;
        }

        /**
         * Dequeue some work.
         */
        @Override
        public GenericWorkItem dequeueWork() {
            JobWorkItem work;
            try {
                synchronized (mLock) {
                    if (mParams == null) {
                        return null;
                    }
                    work = mParams.dequeueWork();
                }
                if (work != null) {
                    work.getIntent().setExtrasClassLoader(mService.getClassLoader());
                    return new JobServiceEngineImpl.WrapperWorkItem(work);
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

    @RequiresApi(26)
    static final class JobWorkEnqueuer extends WorkEnqueuer {
        private final JobInfo mJobInfo;
        private final JobScheduler mJobScheduler;

        JobWorkEnqueuer(Context context, ComponentName cn, int jobId) {
            super(context, cn);
            ensureJobId(jobId);
            JobInfo.Builder b = new JobInfo.Builder(jobId, mComponentName);
            mJobInfo = b.setOverrideDeadline(0).build();
            mJobScheduler = (JobScheduler) context.getApplicationContext().getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
        }

        @Override
        void enqueueWork(Intent work) {
            Logger.d(TAG, "Enqueueing work: " + work);
            try {
                mJobScheduler.enqueue(mJobInfo, new JobWorkItem(work));
            }catch (Exception e){

            }
        }
    }

    /**
     * Abstract definition of an item of work that is being dispatched.
     */
    interface GenericWorkItem {
        Intent getIntent();
        void complete();
    }

    /**
     * An implementation of GenericWorkItem that dispatches work for pre-O platforms: intents
     * received through a raw service's onStartCommand.
     */
    final class CompatWorkItem implements GenericWorkItem {
        final Intent mIntent;
        final int mStartId;

        CompatWorkItem(Intent intent, int startId) {
            mIntent = intent;
            mStartId = startId;
        }

        @Override
        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public void complete() {
            Logger.d(TAG, "Stopping self: #" + mStartId);
            stopSelf(mStartId);
        }
    }

    /**
     * This is a task to dequeue and process work in the background.
     */
    final class CommandProcessor extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            GenericWorkItem work;

            Logger.d(TAG, "Starting to dequeue work...");

            while ((work = dequeueWork()) != null) {
                Logger.d(TAG, "Processing next work: " + work);
                onHandleWork(work.getIntent());
                Logger.d(TAG, "Completing work: " + work);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long waitTime = 0;
                    while (waitTime < getMaxWaitTime() && !isWorkComplete()) {
                        try {
                            Thread.sleep(2000);
                            waitTime += 2000;
                        }catch (Exception e) {
                            break;
                        }
                    }
                    work.complete();
                    Logger.d(TAG, "should complete the cache service, wait time:" + waitTime);
                }
            }

            Logger.d(TAG, "Done processing work!");

            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            processorFinished();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            processorFinished();
        }
    }

    /**
     * Default empty constructor.
     */
    public BackgroundService() {
        if (Build.VERSION.SDK_INT >= 26) {
            mCompatQueue = null;
        } else {
            mCompatQueue = new ArrayList<CompatWorkItem>();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "CREATING: " + this);
        if (Build.VERSION.SDK_INT >= 26) {
            mJobImpl = new JobServiceEngineImpl(this);
            mCompatWorkEnqueuer = null;
        } else {
            mJobImpl = null;
            ComponentName cn = new ComponentName(this, this.getClass());
            synchronized (sClassWorkEnqueuer) {
                try {
                    mCompatWorkEnqueuer = getWorkEnqueuer(this, cn, false, 0);
                }catch (Exception e){

                }
            }
        }
    }

    /**
     * Processes start commands when running as a pre-O service, enqueueing them to be
     * later dispatched in {@link #onHandleWork(Intent)}.
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (mCompatQueue != null && mCompatWorkEnqueuer != null) {
            mCompatWorkEnqueuer.serviceStartReceived();
            Logger.d(TAG, "Received compat start command #" + startId + ": " + intent);
            synchronized (mCompatQueue) {
                mCompatQueue.add(new CompatWorkItem(intent != null ? intent : new Intent(),
                        startId));
                ensureProcessorRunningLocked(true);
            }
            return START_REDELIVER_INTENT;
        } else {
            Logger.d(TAG, "Ignoring start command: " + intent);
            return START_NOT_STICKY;
        }
    }

    /**
     * Returns the IBinder for the {@link JobServiceEngine} when
     * running as a JobService on O and later platforms.
     */
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        if (mJobImpl != null) {
            IBinder engine = mJobImpl.compatGetBinder();
            Logger.d(TAG, "Returning engine: " + engine);
            return engine;
        } else {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompatQueue != null && mCompatWorkEnqueuer != null) {
            synchronized (mCompatQueue) {
                mDestroyed = true;
                mCompatWorkEnqueuer.serviceProcessingFinished();
            }
        }
    }

    /**
     * Call this to enqueue work for your subclass of {@link BackgroundService}.  This will
     * either directly start the service (when running on pre-O platforms) or enqueue work
     * for it as a job (when running on O and later).  In either case, a wake lock will be
     * held for you to ensure you continue running.  The work you enqueue will ultimately
     * appear at {@link #onHandleWork(Intent)}.
     *
     * @param context Context this is being called from.
     * @param cls The concrete class the work should be dispatched to (this is the class that
     * is published in your manifest).
     * @param jobId A unique job ID for scheduling; must be the same value for all work
     * enqueued for the same class.
     * @param work The Intent of work to enqueue.
     */
    public static void enqueueWork(@NonNull Context context, @NonNull Class cls, int jobId,
                                   @NonNull Intent work) {
        enqueueWork(context, new ComponentName(context, cls), jobId, work);
    }

    /**
     * Like {@link #enqueueWork(Context, Class, int, Intent)}, but supplies a ComponentName
     * for the service to interact with instead of its class.
     *
     * @param context Context this is being called from.
     * @param component The published ComponentName of the class this work should be
     * dispatched to.
     * @param jobId A unique job ID for scheduling; must be the same value for all work
     * enqueued for the same class.
     * @param work The Intent of work to enqueue.
     */
    private static void enqueueWork(@NonNull Context context, @NonNull ComponentName component,
                                   int jobId, @NonNull Intent work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        synchronized (sLock) {
            WorkEnqueuer we = getWorkEnqueuer(context, component, true, jobId);
            we.ensureJobId(jobId);
            we.enqueueWork(work);
        }
    }

    static WorkEnqueuer getWorkEnqueuer(Context context, ComponentName cn, boolean hasJobId,
                                                                                int jobId) {
        WorkEnqueuer we = sClassWorkEnqueuer.get(cn);
        if (we == null) {
            if (Build.VERSION.SDK_INT >= 26) {
                if (!hasJobId) {
                    throw new IllegalArgumentException("Can't be here without a job id");
                }
                we = new JobWorkEnqueuer(context, cn, jobId);
            } else {
                we = new CompatWorkEnqueuer(context, cn);
            }
            sClassWorkEnqueuer.put(cn, we);
        }
        return we;
    }

    /**
     * Called serially for each work dispatched to and processed by the service.  This
     * method is called on a background thread, so you can do long blocking operations
     * here.  Upon returning, that work will be considered complete and either the next
     * pending work dispatched here or the overall service destroyed now that it has
     * nothing else to do.
     *
     * <p>Be aware that when running as a job, you are limited by the maximum job execution
     * time and any single or total sequential items of work that exceeds that limit will
     * cause the service to be stopped while in progress and later restarted with the
     * last unfinished work.  (There is currently no limit on execution duration when
     * running as a pre-O plain Service.)</p>
     *
     * @param intent The intent describing the work to now be processed.
     */
    protected abstract void onHandleWork(@NonNull Intent intent);

    @RequiresApi(26)
    protected abstract boolean isWorkComplete();

    @RequiresApi(26)
    protected abstract long getMaxWaitTime();

    /**
     * Control whether code executing in {@link #onHandleWork(Intent)} will be interrupted
     * if the job is stopped.  By default this is false.  If called and set to true, any
     * time {@link #onStopCurrentWork()} is called, the class will first call
     * {@link AsyncTask#cancel(boolean) AsyncTask.cancel(true)} to interrupt the running
     * task.
     *
     * @param interruptIfStopped Set to true to allow the system to interrupt actively
     * running work.
     */
    public void setInterruptIfStopped(boolean interruptIfStopped) {
        mInterruptIfStopped = interruptIfStopped;
    }

    /**
     * Returns true if {@link #onStopCurrentWork()} has been called.  You can use this,
     * while executing your work, to see if it should be stopped.
     */
    public boolean isStopped() {
        return mStopped;
    }

    /**
     * This will be called if the JobScheduler has decided to stop this job.  The job for
     * this service does not have any constraints specified, so this will only generally happen
     * if the service exceeds the job's maximum execution time.
     *
     * @return True to indicate to the JobManager whether you'd like to reschedule this work,
     * false to drop this and all following work. Regardless of the value returned, your service
     * must stop executing or the system will ultimately kill it.  The default implementation
     * returns true, and that is most likely what you want to return as well (so no work gets
     * lost).
     */
    public boolean onStopCurrentWork() {
        return true;
    }

    boolean doStopCurrentWork() {
        if (mCurProcessor != null) {
            mCurProcessor.cancel(mInterruptIfStopped);
        }
        mStopped = true;
        return onStopCurrentWork();
    }

    void ensureProcessorRunningLocked(boolean reportStarted) {
        if (mCurProcessor == null) {
            mCurProcessor = new CommandProcessor();
            if (mCompatWorkEnqueuer != null && reportStarted) {
                mCompatWorkEnqueuer.serviceProcessingStarted();
            }
            Logger.d(TAG, "Starting processor: " + mCurProcessor);
            mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void processorFinished() {
        if (mCompatQueue != null) {
            synchronized (mCompatQueue) {
                mCurProcessor = null;
                // The async task has finished, but we may have gotten more work scheduled in the
                // meantime.  If so, we need to restart the new processor to execute it.  If there
                // is no more work at this point, either the service is in the process of being
                // destroyed (because we called stopSelf on the last intent started for it), or
                // someone has already called startService with a new Intent that will be
                // arriving shortly.  In either case, we want to just leave the service
                // waiting -- either to get destroyed, or get a new onStartCommand() callback
                // which will then kick off a new processor.
                if (mCompatQueue != null && mCompatQueue.size() > 0) {
                    ensureProcessorRunningLocked(false);
                } else if (!mDestroyed) {
                    mCompatWorkEnqueuer.serviceProcessingFinished();
                }
            }
        }
    }

    GenericWorkItem dequeueWork() {
        if (mJobImpl != null) {
            return mJobImpl.dequeueWork();
        } else {
            synchronized (mCompatQueue) {
                if (mCompatQueue.size() > 0) {
                    return mCompatQueue.remove(0);
                } else {
                    return null;
                }
            }
        }
    }
}

