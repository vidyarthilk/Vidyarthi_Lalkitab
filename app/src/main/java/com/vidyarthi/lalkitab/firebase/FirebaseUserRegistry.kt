package com.vidyarthi.lalkitab.firebase

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.vidyarthi.lalkitab.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Saves app-registered user emails to Firebase when they login/register.
 * Cannot read device Gmail automatically — only email the user enters in the app.
 */
object FirebaseUserRegistry {

    private const val TAG = "FirebaseUserRegistry"
    private const val COLLECTION = "registered_users"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onRegister(context: Context, email: String) {
        record(context, email, isNewRegistration = true)
    }

    fun onLogin(context: Context, email: String) {
        record(context, email, isNewRegistration = false)
    }

    /** Call on app start when user is already logged in — updates last seen. */
    fun syncLoggedInUser(context: Context, email: String) {
        record(context, email, isNewRegistration = false)
    }

    private fun record(context: Context, emailRaw: String, isNewRegistration: Boolean) {
        val email = emailRaw.trim().lowercase()
        if (email.isBlank() || !email.contains('@')) return

        scope.launch {
            runCatching {
                val appCtx = context.applicationContext
                val docId = docIdForEmail(email)
                val data = hashMapOf(
                    "email" to email,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "versionCode" to BuildConfig.VERSION_CODE,
                    "deviceModel" to Build.MODEL,
                    "androidRelease" to Build.VERSION.RELEASE.orEmpty(),
                    "installId" to installId(appCtx)
                )
                if (isNewRegistration) {
                    data["registeredAt"] = FieldValue.serverTimestamp()
                }

                Firebase.firestore.collection(COLLECTION)
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()

                val analytics = Firebase.analytics
                analytics.setUserId(docId)
                analytics.setUserProperty("app_account", "registered")
                if (isNewRegistration) {
                    analytics.logEvent("app_register", null)
                } else {
                    analytics.logEvent("app_login", null)
                }
            }.onFailure { e ->
                Log.w(TAG, "Firestore user sync failed", e)
            }
        }
    }

    private fun docIdForEmail(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(email.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    private fun installId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }
}
