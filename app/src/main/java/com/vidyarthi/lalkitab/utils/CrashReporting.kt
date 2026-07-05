package com.vidyarthi.lalkitab.utils

import android.content.Context
import android.util.Log

/**
 * Local crash/error logging. Firebase Crashlytics is intentionally not bundled —
 * it requires the Crashlytics Gradle plugin + build ID or the app crashes at startup.
 */
object CrashReporting {

    private const val TAG = "CrashReporting"

    fun init(context: Context) = Unit

    fun log(message: String) {
        Log.i(TAG, message)
    }

    fun recordException(throwable: Throwable) {
        Log.e(TAG, "Recorded exception", throwable)
    }
}
