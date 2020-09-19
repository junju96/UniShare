package com.lenovo.unishare

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager

class WorkManagerInitializer: Initializer<WorkManager> {
    val TAG = WorkManagerInitializer::class.simpleName

    override fun create(context: Context): WorkManager {
        Log.d(TAG, "wangjj-log create: line in 13 ")

        val configuration = Configuration.Builder().build()
        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        Log.d(TAG, "wangjj-log dependencies: line in 21 ")
        return emptyList()
    }
}