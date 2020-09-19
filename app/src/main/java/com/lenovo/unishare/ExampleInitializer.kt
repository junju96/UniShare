package com.lenovo.unishare

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.WorkManager

class ExampleInitializer : Initializer<ExampleLogger> {
    val TAG = ExampleInitializer::class.java.simpleName

    override fun create(context: Context): ExampleLogger {
        Log.d(TAG, "wangjj-log create: line in 12 ")
        return ExampleLogger(WorkManager.getInstance(context))
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        Log.d(TAG, "wangjj-log dependencies: line in 17 ")
//        return emptyList()
        return listOf(WorkManagerInitializer::class.java)
    }
}