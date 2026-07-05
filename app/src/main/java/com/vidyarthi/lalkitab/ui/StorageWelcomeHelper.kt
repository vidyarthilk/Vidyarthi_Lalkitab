package com.vidyarthi.lalkitab.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.auth.LoginActivity
import com.vidyarthi.lalkitab.subscription.SubscriptionManager

/** One-time info after install — guest / login / premium save limits. */
object StorageWelcomeHelper {

    private const val PREFS = "app_onboarding_prefs"
    private const val KEY_STORAGE_INFO_SHOWN = "storage_info_shown"

    fun showIfFirstLaunch(activity: AppCompatActivity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_STORAGE_INFO_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_STORAGE_INFO_SHOWN, true).apply()

        val message = activity.getString(
            R.string.welcome_storage_message,
            SubscriptionManager.GUEST_KUNDLI_LIMIT,
            SubscriptionManager.LOGGED_IN_KUNDLI_LIMIT
        )

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.welcome_storage_title)
            .setMessage(message)
            .setPositiveButton(R.string.welcome_storage_ok, null)
            .setNeutralButton(R.string.auth_btn_login_settings) { _, _ ->
                activity.startActivity(Intent(activity, LoginActivity::class.java))
            }
            .show()
    }
}
