package com.ushareit.core.stats;

import android.content.Context;

import com.ushareit.core.stats.BaseAnalyticsCollector;

import java.util.List;

public interface IAnalyticsCollectorFactory {
    List<BaseAnalyticsCollector> createCollectors(Context context);
}
