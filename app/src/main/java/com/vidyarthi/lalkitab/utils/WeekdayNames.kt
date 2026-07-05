package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.R

/** Panchang / UI: stable Gujarati weekday keys → localized strings. */
object WeekdayNames {

    private val gujToRes: Map<String, Int> = mapOf(
        "રવિવાર" to R.string.weekday_sunday,
        "સોમવાર" to R.string.weekday_monday,
        "મંગળવાર" to R.string.weekday_tuesday,
        "બુધવાર" to R.string.weekday_wednesday,
        "ગુરુવાર" to R.string.weekday_thursday,
        "શુક્રવાર" to R.string.weekday_friday,
        "શનિવાર" to R.string.weekday_saturday,
    )

    fun localizedFromGujarati(context: Context, gujaratiWeekday: String): String {
        val id = gujToRes[gujaratiWeekday.trim()] ?: return gujaratiWeekday
        return context.getString(id)
    }
}
