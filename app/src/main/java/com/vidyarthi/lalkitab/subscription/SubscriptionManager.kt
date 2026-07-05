package com.vidyarthi.lalkitab.subscription

import android.content.Context
import android.provider.Settings
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.auth.UserAccountManager

/**
 * Guest: max [GUEST_KUNDLI_LIMIT] lifetime saves per device.
 * Logged in (email): max [LOGGED_IN_KUNDLI_LIMIT] lifetime saves.
 * Delete does not reduce lifetime count — only new saves increment it.
 * Premium (Play Billing): unlimited.
 */
object SubscriptionManager {

    const val GUEST_KUNDLI_LIMIT = 10
    const val LOGGED_IN_KUNDLI_LIMIT = 25

    /** @deprecated Use [freeKundliLimit] — kept for backup restore messages. */
    const val FREE_KUNDLI_LIMIT = GUEST_KUNDLI_LIMIT

    private const val PREFS = "subscription_prefs"
    private const val KEY_PREMIUM = "premium_active"
    private const val KEY_BOUND_DEVICE_ID = "bound_device_id"
    private const val KEY_LIFETIME_SAVE_COUNT = "lifetime_save_count"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSubscribed(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PREMIUM, false)
    }

    fun setSubscribed(context: Context, subscribed: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_PREMIUM, subscribed)
            .apply()
    }

    fun freeKundliLimit(context: Context): Int {
        return if (UserAccountManager.isLoggedIn(context)) {
            LOGGED_IN_KUNDLI_LIMIT
        } else {
            GUEST_KUNDLI_LIMIT
        }
    }

    fun maxSavedKundli(context: Context): Int? {
        return if (isSubscribed(context)) null else freeKundliLimit(context)
    }

    /**
     * @param isUpdatingExisting true when editing an existing row (no new lifetime slot).
     */
    fun canSaveAnother(context: Context, currentSavedCount: Int, isUpdatingExisting: Boolean): Boolean {
        if (isUpdatingExisting) return true
        if (isSubscribed(context)) return true
        val limit = freeKundliLimit(context)
        if (currentSavedCount >= limit) return false
        return lifetimeSaveCount(context, currentSavedCount) < limit
    }

    /** Undo after delete — restores a row without consuming another lifetime slot. */
    fun canRestoreDeleted(context: Context, currentSavedCount: Int): Boolean {
        if (isSubscribed(context)) return true
        return currentSavedCount < freeKundliLimit(context)
    }

    /** Call once after a successful new insert (not update / undo restore). */
    fun recordLifetimeSave(context: Context) {
        if (isSubscribed(context)) return
        val limit = freeKundliLimit(context)
        val p = prefs(context)
        ensureDeviceBinding(context)
        val next = p.getInt(KEY_LIFETIME_SAVE_COUNT, 0) + 1
        p.edit()
            .putInt(KEY_LIFETIME_SAVE_COUNT, next.coerceAtMost(limit))
            .apply()
    }

    fun lifetimeSaveCount(context: Context, currentSavedCount: Int = 0): Int {
        if (isSubscribed(context)) return 0
        ensureDeviceBinding(context)
        val p = prefs(context)
        val stored = p.getInt(KEY_LIFETIME_SAVE_COUNT, 0)
        if (currentSavedCount > stored) {
            p.edit().putInt(KEY_LIFETIME_SAVE_COUNT, currentSavedCount).apply()
            return currentSavedCount
        }
        return stored
    }

    fun storageStatusText(context: Context, savedCount: Int): String {
        if (isSubscribed(context)) {
            return context.getString(R.string.subscription_storage_unlimited, savedCount)
        }
        val lifetime = lifetimeSaveCount(context, savedCount)
        val limit = freeKundliLimit(context)
        return context.getString(
            R.string.subscription_storage_limited,
            savedCount,
            lifetime,
            limit
        )
    }

    fun limitReachedMessage(context: Context): String {
        if (isSubscribed(context)) return ""
        return if (UserAccountManager.isLoggedIn(context)) {
            context.getString(R.string.toast_kundli_limit_logged_in, LOGGED_IN_KUNDLI_LIMIT)
        } else {
            context.getString(
                R.string.toast_kundli_limit_guest,
                GUEST_KUNDLI_LIMIT,
                LOGGED_IN_KUNDLI_LIMIT
            )
        }
    }

    fun undoLimitReachedMessage(context: Context): String {
        if (isSubscribed(context)) return ""
        val limit = freeKundliLimit(context)
        return context.getString(R.string.toast_undo_limit_reached, limit)
    }

    private fun ensureDeviceBinding(context: Context) {
        val p = prefs(context)
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
        val boundId = p.getString(KEY_BOUND_DEVICE_ID, null)
        when {
            boundId == null -> {
                p.edit().putString(KEY_BOUND_DEVICE_ID, deviceId).apply()
            }
            boundId != deviceId -> {
                p.edit()
                    .putString(KEY_BOUND_DEVICE_ID, deviceId)
                    .putInt(KEY_LIFETIME_SAVE_COUNT, 0)
                    .apply()
            }
        }
    }
}
