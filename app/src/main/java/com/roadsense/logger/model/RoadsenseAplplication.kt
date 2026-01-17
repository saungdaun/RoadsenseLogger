package com.roadsense.logger

import android.app.Application
import android.util.Log
import timber.log.Timber

class RoadsenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant Timber tree for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("🚀 Roadsense Logger Application Started - DEBUG MODE")
        } else {
            // In production, use ReleaseTree
            Timber.plant(ReleaseTree())
            Timber.i("🚀 Roadsense Logger Application Started - RELEASE MODE")
        }
    }

    // Custom tree for release builds
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // In release, only log warnings and errors to Logcat
            if (priority == Log.WARN || priority == Log.ERROR || priority == Log.ASSERT) {
                // Log to Logcat
                when (priority) {
                    Log.VERBOSE, Log.DEBUG -> return // Skip verbose/debug in release
                    Log.INFO -> Log.i(tag ?: "Roadsense", message)
                    Log.WARN -> Log.w(tag ?: "Roadsense", message)
                    Log.ERROR -> {
                        Log.e(tag ?: "Roadsense", message)
                        t?.let {
                            Log.e(tag ?: "Roadsense", "Exception", it)
                        }
                    }
                }

                // TODO: Here you can also send to Crashlytics, Firebase Crashlytics, etc.
                // Example with Firebase Crashlytics (if you add it later):
                // if (priority == Log.ERROR && t != null) {
                //     FirebaseCrashlytics.getInstance().recordException(t)
                // }
            }
        }

        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // Only log WARN and ERROR in release
            return priority >= Log.WARN
        }
    }
}