package com.vidyarthi.lalkitab.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.firebase.FirebaseUserRegistry
import kotlinx.coroutines.tasks.await
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Email login via Firebase Authentication.
 * Legacy device-local accounts are migrated automatically on next login.
 */
object UserAccountManager {

    private const val PREFS = "user_account_prefs"
    private const val LEGACY_PREFS = "user_account_prefs_legacy"
    private const val KEY_REGISTERED_EMAIL = "registered_email"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_SALT = "password_salt"
    private const val KEY_LAST_EMAIL = "last_email"
    private const val MIN_PASSWORD_LENGTH = 6
    private const val PBKDF2_ITERATIONS = 120_000
    private const val HASH_PREFIX_V2 = "pbkdf2:"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val messageRes: Int) : AuthResult()
    }

    fun isLoggedIn(context: Context): Boolean = auth.currentUser != null

    fun registeredEmail(context: Context): String? =
        auth.currentUser?.email?.takeIf { it.isNotBlank() }

    /** Last email used on this device (for pre-fill). */
    fun lastUsedEmail(context: Context): String? =
        registeredEmail(context)
            ?: prefs(context).getString(KEY_LAST_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun hasRegisteredAccount(context: Context): Boolean =
        isLoggedIn(context) || hasLegacyLocalAccount(context)

    suspend fun register(context: Context, emailRaw: String, password: String): AuthResult {
        val email = normalizeEmail(emailRaw)
        if (!isValidEmail(email)) return AuthResult.Error(R.string.auth_error_invalid_email)
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthResult.Error(R.string.auth_error_password_short)
        }

        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            cacheLastEmail(context, email)
            clearLegacyLocalAccount(context)
            FirebaseUserRegistry.onRegister(context, email)
            AuthResult.Success
        }.getOrElse { e ->
            mapFirebaseError(e) ?: AuthResult.Error(R.string.auth_error_generic)
        }
    }

    suspend fun login(context: Context, emailRaw: String, password: String): AuthResult {
        val email = normalizeEmail(emailRaw)
        if (!isValidEmail(email)) return AuthResult.Error(R.string.auth_error_invalid_email)
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthResult.Error(R.string.auth_error_password_short)
        }

        val signIn = runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
        }

        if (signIn.isSuccess) {
            cacheLastEmail(context, email)
            clearLegacyLocalAccount(context)
            FirebaseUserRegistry.onLogin(context, email)
            return AuthResult.Success
        }

        val error = signIn.exceptionOrNull()
        if (error is FirebaseAuthInvalidUserException && matchesLegacyLocalAccount(context, email, password)) {
            return migrateLegacyAccount(context, email, password)
        }

        return mapFirebaseError(error) ?: AuthResult.Error(R.string.auth_error_wrong_credentials)
    }

    fun logout(context: Context) {
        auth.signOut()
    }

    private suspend fun migrateLegacyAccount(
        context: Context,
        email: String,
        password: String
    ): AuthResult {
        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            cacheLastEmail(context, email)
            clearLegacyLocalAccount(context)
            FirebaseUserRegistry.onRegister(context, email)
            AuthResult.Success
        }.getOrElse { e ->
            val mapped = mapFirebaseError(e)
            if (mapped is AuthResult.Error && mapped.messageRes == R.string.auth_error_already_registered) {
                runCatching {
                    auth.signInWithEmailAndPassword(email, password).await()
                    cacheLastEmail(context, email)
                    clearLegacyLocalAccount(context)
                    FirebaseUserRegistry.onLogin(context, email)
                    AuthResult.Success
                }.getOrElse { signInError ->
                    mapFirebaseError(signInError) ?: AuthResult.Error(R.string.auth_error_wrong_credentials)
                }
            } else {
                mapped ?: AuthResult.Error(R.string.auth_error_generic)
            }
        }
    }

    private fun mapFirebaseError(error: Throwable?): AuthResult.Error? {
        if (error == null) return null
        return when (error) {
            is FirebaseAuthUserCollisionException ->
                AuthResult.Error(R.string.auth_error_already_registered)
            is FirebaseAuthWeakPasswordException ->
                AuthResult.Error(R.string.auth_error_password_short)
            is FirebaseAuthInvalidCredentialsException ->
                AuthResult.Error(R.string.auth_error_wrong_credentials)
            is FirebaseAuthInvalidUserException ->
                AuthResult.Error(R.string.auth_error_wrong_credentials)
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" ->
                    AuthResult.Error(R.string.auth_error_already_registered)
                "ERROR_INVALID_EMAIL" ->
                    AuthResult.Error(R.string.auth_error_invalid_email)
                "ERROR_WEAK_PASSWORD" ->
                    AuthResult.Error(R.string.auth_error_password_short)
                "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                    AuthResult.Error(R.string.auth_error_wrong_credentials)
                "ERROR_NETWORK_REQUEST_FAILED" ->
                    AuthResult.Error(R.string.auth_error_network)
                else -> AuthResult.Error(R.string.auth_error_generic)
            }
            else -> null
        }
    }

    private fun cacheLastEmail(context: Context, email: String) {
        prefs(context).edit().putString(KEY_LAST_EMAIL, email).apply()
    }

    private fun hasLegacyLocalAccount(context: Context): Boolean =
        legacyPrefs(context).getString(KEY_REGISTERED_EMAIL, null)?.isNotBlank() == true

    private fun matchesLegacyLocalAccount(context: Context, email: String, password: String): Boolean {
        val p = legacyPrefs(context)
        val storedEmail = p.getString(KEY_REGISTERED_EMAIL, null) ?: return false
        if (normalizeEmail(storedEmail) != email) return false
        val salt = p.getString(KEY_SALT, null) ?: return false
        val storedHash = p.getString(KEY_PASSWORD_HASH, null) ?: return false
        return verifyPassword(password, salt, storedHash)
    }

    private fun clearLegacyLocalAccount(context: Context) {
        legacyPrefs(context).edit().clear().apply()
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_REGISTERED_EMAIL)
            .remove(KEY_PASSWORD_HASH)
            .remove(KEY_SALT)
            .apply()
    }

    private fun legacyPrefs(context: Context): SharedPreferences {
        val appCtx = context.applicationContext
        return try {
            createEncryptedPrefs(appCtx)
        } catch (_: Exception) {
            appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun verifyPassword(password: String, saltHex: String, storedHash: String): Boolean {
        return when {
            storedHash.startsWith(HASH_PREFIX_V2) ->
                hashPasswordV2(password, saltHex) == storedHash
            else -> hashPasswordLegacy(password, saltHex) == storedHash
        }
    }

    private fun hashPasswordV2(password: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return HASH_PREFIX_V2 + hash.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasswordLegacy(password: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0)
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
