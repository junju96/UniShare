package com.ushareit.core.lang;

import android.content.Context;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;

public class Singleton {
    /**
     * Singleton Pattern, see XXXSingletonImpl for details.
     * @param <Clazz> the actual class name that have this Singleton semantics.
     */
    public interface ISingletonCreator<Clazz> {
        /**
         * create the singleton instance, this will be called when first client attached.
         * @param context an Context passed to Singleton constructor, many class need a Context object to initialize.
         * @return the singleton instance created and initialized.
         */
        Clazz createSingletonInstance(Context context);
        /**
         * destroy the singleton instance, this will be called when last client detached.
         * @param instance the singleton instance to be destroyed.
         */
        void destroySingletonInstance(Clazz instance);
    }

    /**
     * Singleton Pattern, combined with ISingletonCreator to implement a singleton feature for classes.
     *
     * @param <Clazz> the actual class name that have this Singleton semantics.
     *
     * Implementor Usage:
     *
       Class CLAZZ {
            static class Creator implements ISingletonCreator<CLAZZ> {
                @Override
                public CLAZZ createSingletonInstance(Context context) {
                    CommandEngine instance = new CLAZZ(context);
                    // NOTE initialize instance here
                    return instance;
                }

                @Override
                public void destroySingletonInstance(CLAZZ instance) {
                    // NOTE destroy instance here
                }
            }

            public static final SimpleSingletonImpl<CLAZZ> Singleton = new SimpleSingletonImpl<CLAZZ>(new Creator());

        ...
        }
     *
     * Caller Usage:
     *
         CLAZZ instance = CLAZZ.Singleton.get(clientId);
         instance.callMethods();
     */
    public static final class SimpleSingletonImpl<CLAZZ> {
        private static final String TAG = "Singleton";

        private ISingletonCreator<CLAZZ> mCreator;
        private CLAZZ sInstance = null;
        private long mAccessCount = 0;

        public SimpleSingletonImpl(ISingletonCreator<CLAZZ> creator) {
            Assert.notNull(creator,"creator can't be null");
            mCreator = creator;

            Logger.d(TAG, "Creator Registered: " + mCreator.getClass().getName());
        }

        /**
         * get a reference of the singleton instance.
         */
        public synchronized CLAZZ get() {
            return get("internal");
        }

        /**
         * get a reference of the singleton instance.
         * @param clientId an arbitrary string to track clients (caller)
         */
        public synchronized CLAZZ get(String clientId) {
            if (sInstance == null) {
                Logger.d(TAG, "Instance Creating: " + mCreator.getClass().getName() + ", ClientId = " + clientId);
                // note: following may throw exception
                sInstance = mCreator.createSingletonInstance(ObjectStore.getContext());
                Assert.notNull(sInstance, "singleton creator can't create instance: " + mCreator.getClass().getName());
            }

            mAccessCount++;
            Logger.d(TAG, "Client Attached: Creator = " + mCreator.getClass().getName() + ", ClientId = " + clientId + ", AccessCount = " + mAccessCount);
            return sInstance;
        }
    }
}
