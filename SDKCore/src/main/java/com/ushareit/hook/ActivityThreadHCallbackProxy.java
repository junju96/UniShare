package com.ushareit.hook;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.ushareit.core.Logger;

public class ActivityThreadHCallbackProxy implements Handler.Callback {

    public static final String TAG = "CallbackProxy";

    public static final int PAUSE_ACTIVITY = 101;
    public static final int PAUSE_ACTIVITY_FINISHING = 102;
    public static final int STOP_ACTIVITY_SHOW = 103;
    public static final int STOP_ACTIVITY_HIDE = 104;
    //    public static final int DESTROY_ACTIVITY = 109;
//    public static final int RECEIVER = 113;
//    public static final int CREATE_SERVICE = 114;
    public static final int SERVICE_ARGS = 115;
    public static final int STOP_SERVICE = 116;
    public static final int SLEEPING = 137;


    private Handler mRawHandler;

    public ActivityThreadHCallbackProxy(Handler handler) {
        mRawHandler = handler;
    }

    @Override
    public boolean handleMessage(Message message) {
        try {
            switch (message.what) {
                case STOP_ACTIVITY_HIDE:
                case STOP_ACTIVITY_SHOW:
                    Logger.d(TAG, "stop Activity");
                    //stop activity
                    beforeWaitToFinished();
                    break;
//                case DESTROY_ACTIVITY:
//                    Logger.d(TAG, "destroy Activity");
//                    //destroy activity
//                    beforeWaitToFinished();
//                    break;
//                case RECEIVER:
//                    Logger.d(TAG, "receiver");
//                    //receiver
//                    beforeWaitToFinished();
//                    break;
//                case CREATE_SERVICE:
//                    Logger.d(TAG, "create service");
//                    //SERVICE ARGS
//                    beforeWaitToFinished();
//                    break;
                case SERVICE_ARGS:
                    Logger.d(TAG, "service args");
                    //SERVICE ARGS
                    beforeWaitToFinished();
                    break;
                case STOP_SERVICE:
                    Logger.d(TAG, "stop service");
                    //STOP SERVICE
                    beforeWaitToFinished();
                    break;

                case SLEEPING:
                    Logger.d(TAG, "sleeping");
                    //SLEEPING
                    beforeWaitToFinished();
                    break;
                case PAUSE_ACTIVITY:
                case PAUSE_ACTIVITY_FINISHING:
                    Logger.d(TAG, "pause activity");
                    //pause activity
                    beforeWaitToFinished();
                    break;
                default:
                    break;
            }
            if (mRawHandler != null) {
                mRawHandler.handleMessage(message);
            }
        } catch (Throwable e) {
            String className = e.getClass().getName();
            if (!TextUtils.isEmpty(className)) {
                if (className.contains("RemoteServiceException")
                        || className.contains("SecurityException")
                        || className.contains("DeadSystemException")) {
                    e.printStackTrace();
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
        return true;
    }

    private void beforeWaitToFinished() {
        QueuedWorkProxy.cleanAll();
    }
}
