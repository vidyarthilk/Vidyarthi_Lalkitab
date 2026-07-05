package com.vidyarthi.lalkitab.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "vidyarthi_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val LANG_EN = "en"
    const val LANG_HI = "hi"

    fun persistLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_EN) ?: LANG_EN
    }

    fun syncApplicationLocales(context: Context) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(getLanguage(context))
        )
    }

    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
