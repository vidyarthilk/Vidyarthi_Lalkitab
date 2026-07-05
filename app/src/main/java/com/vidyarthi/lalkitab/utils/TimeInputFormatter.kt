package com.vidyarthi.lalkitab.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Birth time display (12-hour AM/PM) and parsing for kundli calculations. */
object TimeInputFormatter {

    private val amPmDisplay = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val amPmDisplayAlt = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val legacy24h = SimpleDateFormat("HH:mm", Locale.US)

    fun formatFromPicker(hourOfDay: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return amPmDisplay.format(cal.time)
    }

    /** Returns 24-hour hour and minute, or null if invalid. Accepts old HH:mm saves too. */
    fun parseTo24Hour(time: String): Pair<Int, Int>? {
        val t = time.trim()
        if (t.isEmpty()) return null

        parseWith(amPmDisplay, t)?.let { return it }
        parseWith(amPmDisplayAlt, t)?.let { return it }

        val parts = t.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    /** Show saved kundli time in AM/PM when stored as 24-hour. */
    fun toDisplayTime(stored: String): String {
        val (hour, minute) = parseTo24Hour(stored) ?: return stored.trim()
        return formatFromPicker(hour, minute)
    }

    private fun parseWith(format: SimpleDateFormat, text: String): Pair<Int, Int>? {
        return try {
            val date = format.parse(text) ?: return null
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        } catch (_: ParseException) {
            null
        }
    }
}
