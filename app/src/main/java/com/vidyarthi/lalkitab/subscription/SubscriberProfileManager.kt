package com.vidyarthi.lalkitab.subscription

import android.content.Context

/** Premium subscriber letterhead for PDF export (name, phone, address). */
object SubscriberProfileManager {

    private const val PREFS = "subscriber_profile_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_PHONE = "phone"
    private const val KEY_ADDRESS = "address"

    data class Profile(
        val name: String,
        val phone: String,
        val address: String
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): Profile {
        val p = prefs(context)
        return Profile(
            name = p.getString(KEY_NAME, "").orEmpty(),
            phone = p.getString(KEY_PHONE, "").orEmpty(),
            address = p.getString(KEY_ADDRESS, "").orEmpty()
        )
    }

    fun save(context: Context, name: String, phone: String, address: String) {
        prefs(context).edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PHONE, phone.trim())
            .putString(KEY_ADDRESS, address.trim())
            .apply()
    }
}
