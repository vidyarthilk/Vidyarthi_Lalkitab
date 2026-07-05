package com.vidyarthi.lalkitab.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vidyarthi.lalkitab.firebase.FirebaseUserRegistry
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Device-local email account for higher kundli save limit.
 * Credentials stored in encrypted prefs; password as salted PBKDF2 hash.
 */
object UserAccountManager {

    private const val PREFS = "user_account_prefs"
    private const val LEGACY_PREFS = "user_account_prefs_legacy"
    private const val KEY_REGISTERED_EMAIL = "registered_email"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_SALT = "password_salt"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val MIN_PASSWORD_LENGTH = 6
    private const val PBKDF2_ITERATIONS = 120_000
    private const val HASH_PREFIX_V2 = "pbkdf2:"

    private fun prefs(context: Context): SharedPreferences {
        val appCtx = context.applicationContext
        val encrypted = try {
            createEncryptedPrefs(appCtx)
        } catch (_: Exception) {
            appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
        migrateLegacyPrefsIfNeeded(appCtx, encrypted)
        return encrypted
    }

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

    private fun migrateLegacyPrefsIfNeeded(context: Context, encrypted: SharedPreferences) {
        if (encrypted.contains(KEY_REGISTERED_EMAIL)) return
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val plain = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val source = when {
            legacy.contains(KEY_REGISTERED_EMAIL) -> legacy
            plain.contains(KEY_REGISTERED_EMAIL) -> plain
            else -> return
        }
        encrypted.edit()
            .putString(KEY_REGISTERED_EMAIL, source.getString(KEY_REGISTERED_EMAIL, null))
            .putString(KEY_PASSWORD_HASH, source.getString(KEY_PASSWORD_HASH, null))
            .putString(KEY_SALT, source.getString(KEY_SALT, null))
            .putBoolean(KEY_LOGGED_IN, source.getBoolean(KEY_LOGGED_IN, false))
            .apply()
        source.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun registeredEmail(context: Context): String? =
        prefs(context).getString(KEY_REGISTERED_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun hasRegisteredAccount(context: Context): Boolean =
        registeredEmail(context) != null

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val messageRes: Int) : AuthResult()
    }

    fun register(context: Context, emailRaw: String, password: String): AuthResult {
        val email = normalizeEmail(emailRaw)
        if (!isValidEmail(email)) return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_invalid_email)
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_password_short)
        }
        if (hasRegisteredAccount(context)) {
            return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_already_registered)
        }

        val salt = randomSaltHex()
        prefs(context).edit()
            .putString(KEY_REGISTERED_EMAIL, email)
            .putString(KEY_SALT, salt)
            .putString(KEY_PASSWORD_HASH, hashPasswordV2(password, salt))
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        FirebaseUserRegistry.onRegister(context, email)
        return AuthResult.Success
    }

    fun login(context: Context, emailRaw: String, password: String): AuthResult {
        val email = normalizeEmail(emailRaw)
        val storedEmail = registeredEmail(context)
            ?: return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_no_account)
        if (email != storedEmail) {
            return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_wrong_credentials)
        }

        val p = prefs(context)
        val salt = p.getString(KEY_SALT, null) ?: return AuthResult.Error(
            com.vidyarthi.lalkitab.R.string.auth_error_wrong_credentials
        )
        val storedHash = p.getString(KEY_PASSWORD_HASH, null) ?: return AuthResult.Error(
            com.vidyarthi.lalkitab.R.string.auth_error_wrong_credentials
        )
        if (!verifyPassword(password, salt, storedHash)) {
            return AuthResult.Error(com.vidyarthi.lalkitab.R.string.auth_error_wrong_credentials)
        }

        val editor = p.edit().putBoolean(KEY_LOGGED_IN, true)
        if (!storedHash.startsWith(HASH_PREFIX_V2)) {
            editor.putString(KEY_PASSWORD_HASH, hashPasswordV2(password, salt))
        }
        editor.apply()
        FirebaseUserRegistry.onLogin(context, email)
        return AuthResult.Success
    }

    fun logout(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun randomSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

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

    /** Legacy SHA-256 for accounts created before v5.4. */
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
