package com.vidyarthi.lalkitab

import android.app.Application
import android.content.Context
import com.vidyarthi.lalkitab.auth.UserAccountManager
import com.vidyarthi.lalkitab.firebase.FirebaseUserRegistry
import com.vidyarthi.lalkitab.subscription.SubscriptionBilling
import com.vidyarthi.lalkitab.utils.CrashReporting
import com.vidyarthi.lalkitab.utils.LocaleHelper
import com.vidyarthi.lalkitab.utils.SwissEphManager

class VidyarthiLalkitabApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        LocaleHelper.syncApplicationLocales(this)

        if (!SwissEphManager.initSafe(this)) {
            SwissEphManager.lastInitError()?.let { error ->
                CrashReporting.log("SwissEph init failed at startup")
                CrashReporting.recordException(error)
            }
        }

        try {
            SubscriptionBilling.syncFromPlayIfPossible(this)
        } catch (e: Exception) {
            CrashReporting.recordException(e)
        }

        if (UserAccountManager.isLoggedIn(this)) {
            UserAccountManager.registeredEmail(this)?.let { email ->
                FirebaseUserRegistry.syncLoggedInUser(this, email)
            }
        }
    }
}
