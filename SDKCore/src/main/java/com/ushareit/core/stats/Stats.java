package com.ushareit.core.stats;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.ushareit.core.Logger;
import com.ushareit.core.Settings;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.lang.thread.TaskHelper.RunnableWithName;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * the statistics facade.
 */
public final class Stats {
    private static final String TAG = "Stats";

    private static Context sContext = null;
    private static IAnalyticsCollectorFactory sCollectorFactory = null;

    private static Stats sInstance = null;

    private List<BaseAnalyticsCollector> mAnalyticsCollectors = null;
    private static final int DEFAULT_RANDOM_RATE = 100;
    public static final int DEFAULT_HIGH_RANDOM_RATE = 10;

    private static Map<String, Integer> mConfigEvents = new HashMap<>();

    public static void init(Context context, IAnalyticsCollectorFactory factory) {
        sContext = context;
        sCollectorFactory = factory;
    }

    public static void readStatsConfigEvents(String jconfig){
        if (TextUtils.isEmpty(jconfig))
            return;
        try {
            JSONObject json = new JSONObject(jconfig);
            Iterator<String> names = json.keys();
            while (names.hasNext()) {
                String name = names.next();
                mConfigEvents.put(name, json.getInt(name));
            }

        } catch (Exception e) {}
        //todo: Adapt andrid p webview bug
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HashMap<String, String> params = new HashMap<>();
            Stats.onEvent(sContext, "test_user_webview", params);
        }
    }

    private static Stats get() {
        // double checked singleton design pattern
        if (sInstance == null) {
            synchronized(Stats.class) {
                if (sInstance == null) {
                    Logger.v(TAG, "Stats inited");
                    List<BaseAnalyticsCollector> collectors = sCollectorFactory.createCollectors(sContext);
                    sInstance = new Stats(collectors);
                }
            }
        }
        return sInstance;
    }

    private Stats(List<BaseAnalyticsCollector> collectors) {
        mAnalyticsCollectors = collectors;
    }

    public static void dispatch(Context context, String portal) {
        if(sCollectorFactory == null){
            return;
        }
        for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors)
            collector.dispatch(context, portal);
    }

    public static boolean syncDispatch(Context context, Class<?> clazz, String portal) {
        if(sCollectorFactory == null){
            return false;
        }
        for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
            if (clazz.isInstance(collector))
                return collector.syncDispatch(context, portal);
        }
        return false;
    }

    public static boolean dispatchSpecial(Context context, Class<?> clazz, String portal) {
        if(sCollectorFactory == null){
            return false;
        }
        for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
            if (clazz.isInstance(collector)) {
                int c = collector.countEvent(context);
                Logger.v(TAG, "dispatchLenovo: event count = " + c);
                if (c > 0) {
                    collector.dispatch(context, portal);
                    return (collector.countEvent(context) > 0);
                }
            }
        }
        return false;
    }

    public static void onAppDestroy() {
        if(sCollectorFactory == null){
            return;
        }
        for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
            if (collector.isCollectEvent())
                collector.onAppDestroy();
        }
    }

    public static void onAppBackend() {
        if(sCollectorFactory == null){
            return;
        }
        for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
            if (collector.isCollectEvent())
                collector.onAppBackend();
        }
    }

    public static boolean isRandomCollect(int rateDenominator) {
        return isRandomCollect(1, rateDenominator);
    }

    public static boolean isRandomCollect(int rateNumerator, int rateDenominator) {
        Random random = new Random();
        return (random.nextInt(rateDenominator) < rateNumerator);
    }

    // following methods are all scheduled to be called in separate thread asynchrously

    public static void onResume(Context context, String extra) {
        if (context == null)
            return;
        onResume(context.getClass().getName(), extra);
    }
    public static void onResume(final String className, final String extra) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectPageView())
                        collector.onResume(className, extra);
                }
            }
        });
    }

    public static void onPause(Context context, String extra) {
        if (context == null)
            return;
        onPause(context.getClass().getName(), extra);
    }

    public static void onPause(final String className, final String extra) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectPageView())
                        collector.onPause(className, extra);
                }
            }
        });
    }

    public static void onEvent(final Context context, final String eventId) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent())
                        collector.onEvent(context, eventId);
                }
                Logger.d(TAG, "onEvent(): " + eventId);
            }
        });
    }

    public void onRandomEvent(Context context, String eventId) {
        onRandomEvent(context, eventId, DEFAULT_RANDOM_RATE);
    }

    // rateDenominator is random collect rate denominator, if the rateDenominator is m, then the event collect rate is 1/m.
    public static void onRandomEvent(final Context context, final String eventId, int rateDenominator) {
        if(sCollectorFactory == null){
            return;
        }
        if (isRandomCollect(rateDenominator)) {
            TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
                @Override
                public void execute() {
                    for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                        if (collector.isCollectEvent())
                            collector.onEvent(context, eventId);
                    }
                    Logger.d(TAG, "onRandomEvent(): " + eventId);
                }
            });
        }
    }

    public static void onEvent(final Context context, final String eventId, final String label) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent())
                        collector.onEvent(context, eventId, label);
                }
                Logger.d(TAG, "onEvent(): " + eventId + ", label = " + label);
            }
        });
    }

    public static void onOnceEvent(final Context context, final String eventId, final String label) {
        if(sCollectorFactory == null){
            return;
        }
        final Settings settings = new Settings(context);
        final String key = "Analytics" + eventId;
        boolean hasReport = settings.getBoolean(key, false);
        if (!hasReport) {
            TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
                @Override
                public void execute() {
                    for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                        if (collector.isCollectEvent())
                            collector.onEvent(context, eventId, label);
                    }
                    settings.setBoolean(key, true);
                    Logger.d(TAG, "onOnceEvent(): " + eventId + ", label = " + label);
                }
            });
        }
    }
    
    public static void onOnceEvent(final Context context, final String eventId, final HashMap<String, String> map) {
        if(sCollectorFactory == null){
            return;
        }
        final Settings settings = new Settings(context);
        final String key = "Analytics" + eventId;
        boolean hasReport = settings.getBoolean(key, false);
        if (!hasReport) {
            TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
                @Override
                public void execute() {
                    for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                        if (collector.isCollectEvent())
                            collector.onEvent(context, eventId, map);
                    }
                    settings.setBoolean(key, true);
                    Logger.d(TAG, "onOnceEvent(): " + eventId + ", info = " + map.toString());
                }
            });
        }
    }

    public static boolean isOnceEventReported(Context context, String eventId){
        final Settings settings = new Settings(context);
        final String key = "Analytics" + eventId;
        return settings.getBoolean(key, false);
    }

    public static void onRandomEvent(Context context, String eventId, String label) {
        onRandomEvent(context, eventId, label, DEFAULT_RANDOM_RATE);
    }

    public static void onHighRandomEvent(Context context, String eventId, String label) {
        onRandomEvent(context, eventId, label, DEFAULT_HIGH_RANDOM_RATE);
    }

    // rateDenominator is random collect rate denominator, if the rateDenominator is m, then the event collect rate is 1/m.
    public static void onRandomEvent(final Context context, final String eventId, final String label, int rateDenominator) {
        if(sCollectorFactory == null){
            return;
        }
        if (isRandomCollect(rateDenominator)) {
            TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
                @Override
                public void execute() {
                    for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                        if (collector.isCollectEvent())
                            collector.onEvent(context, eventId, label);
                    }
                    Logger.d(TAG, "onRandomEvent(): " + eventId + ", label = " + label);
                }
            });            
        }
    }

    public static void onEvent(final Context context, final String eventId, final HashMap<String, String> params) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent())
                        collector.onEvent(context, eventId, params);
                }
                Logger.d(TAG, "onEvent(): " + eventId + ", info = " + params.toString());
            }
        });            
    }

    public static void onEvent(final Context context, final String eventId, final HashMap<String, String> params, final int value) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent())
                        collector.onEvent(context, eventId, params, value);
                }
                Logger.d(TAG, "onEvent(): " + eventId + ", info = " + params.toString() + ", value = " + value);
            }
        });
    }

    public static void onEvent(final Context context, final String eventId, final HashMap<String, String> params, final int value, int rateDenominator) {
        if (isRandomCollect(rateDenominator)) {
            onEvent(context, eventId, params, value);
        }
    }

    public static void onSpecialEvent(final Context context, final String eventId, final HashMap<String, String> params, final Class<?> clazz) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;
                if(clazz == null)
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent() && clazz.isInstance(collector))
                        collector.onEvent(context, eventId, params);
                }
                Logger.d(TAG, "onSpecialEvent(): " + eventId + ", info = " + params.toString());
            }
        });
    }

    public static void onSpecialEvent(final Context context, final String eventId, final HashMap<String, String> params, final String collectorName) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                if (ignoreByConfig(eventId))
                    return;
                if(collectorName == null)
                    return;

                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent() && collectorName.equals(collector.getCollectorName()))
                        collector.onEvent(context, eventId, params);
                }
                Logger.d(TAG, "onSpecialEvent(): " + eventId + ", info = " + params.toString() + ", collector = " + collectorName);
            }
        });
    }

    public static void onRandomEvent(Context context, String eventId, HashMap<String, String> map) {
        onRandomEvent(context, eventId, map, DEFAULT_RANDOM_RATE);
    }

    public static void onSpecialRandomEvent(Context context, String eventId, HashMap<String, String> map, Class<?> clazz) {
        if (!isRandomCollect(1, DEFAULT_RANDOM_RATE))
            return;

        onSpecialEvent(context, eventId, map, clazz);
    }

    public static void onHighRandomEvent(Context context, String eventId, HashMap<String, String> map) {
        onRandomEvent(context, eventId, map, DEFAULT_HIGH_RANDOM_RATE);
    }

    public static void onSpecialHighRandomEvent(Context context, String eventId, HashMap<String, String> map, Class<?> clazz) {
        if (!isRandomCollect(1, DEFAULT_HIGH_RANDOM_RATE))
            return;

        onSpecialEvent(context, eventId, map, clazz);
    }

    // rateDenominator is random collect rate denominator, if the rateDenominator is m, then the event collect rate is 1/m.
    public static void onRandomEvent(final Context context, final String eventId, final HashMap<String, String> params, int rateDenominator) {
        onRandomEvent(context, eventId, params, 1, rateDenominator);
    }

    // Rate is random collect rate, if the rate is m, then the event collect rate is 1/m.
    public static void onRandomEvent(final Context context, final String eventId, final HashMap<String, String> params, int rateNumerator, int rateDenominator) {
        if(sCollectorFactory == null){
            return;
        }
        if (isRandomCollect(rateNumerator, rateDenominator)) {
            TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
                @Override
                public void execute() {
                    for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                        if (collector.isCollectEvent())
                            collector.onEvent(context, eventId, params);
                    }
                    Logger.d(TAG, "onRandomEvent(): " + eventId + ", info = " + params.toString());
                }
            });
        }
    }

    //Only UmengCollector support this method
    public static void onError(final Context context, final String error) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent() || collector.isCollectPageView())
                        collector.onError(context, error);
                }
                Logger.d(TAG, "onError(): error = " + error);
            }
        });
    }

    //Only UmengCollector support this method
    public static void onError(final Context context, final Throwable e) {
        if(sCollectorFactory == null){
            return;
        }
        TaskHelper.execZForAnalytics(new RunnableWithName("Stats") {
            @Override
            public void execute() {
                for (BaseAnalyticsCollector collector : get().mAnalyticsCollectors) {
                    if (collector.isCollectEvent() || collector.isCollectPageView())
                        collector.onError(context, e);
                }
                Logger.d(TAG, "onError(): error = " + e.getClass().getSimpleName());
            }
        });
    }

    private static boolean ignoreByConfig(String eventId) {
        if (!mConfigEvents.containsKey(eventId))
            return false;

        int number = mConfigEvents.get(eventId);
        return !isRandomCollect(number, DEFAULT_RANDOM_RATE);
    }
}
