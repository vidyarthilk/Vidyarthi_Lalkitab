package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.data.db.AppDatabase
import com.vidyarthi.lalkitab.data.entity.KundliEntity
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Saved kundli JSON backup — Drive / WhatsApp થી નવા phone પર restore.
 * Free plan: restore respects guest/login lifetime limits. Premium: unlimited.
 */
object KundliBackupManager {

    const val APP_ID = "Vidyarthi_Lalkitab"
    const val FORMAT_VERSION = 1
    const val BACKUP_FILE_PREFIX = "vidyarthi_lalkitab_backup"

    data class RestoreResult(
        val imported: Int,
        val skippedDuplicates: Int,
        val skippedLimit: Int,
        val totalInFile: Int,
    )

    suspend fun exportToJson(context: Context): String {
        val list = AppDatabase.getDatabase(context.applicationContext).kundliDao().getAllKundli()
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", Instant.now().toString())
        root.put("count", list.size)
        val arr = JSONArray()
        for (k in list) {
            arr.put(kundliToJson(k))
        }
        root.put("kundlis", arr)
        return root.toString(2)
    }

    suspend fun restoreFromJson(
        context: Context,
        jsonText: String,
        replaceExisting: Boolean,
    ): RestoreResult {
        val root = parseAndValidate(jsonText.trim())
        val arr = root.getJSONArray("kundlis")
        val appCtx = context.applicationContext
        val dao = AppDatabase.getDatabase(appCtx).kundliDao()

        if (replaceExisting) {
            dao.deleteAllKundli()
        }

        var imported = 0
        var skippedDuplicates = 0
        var skippedLimit = 0

        for (i in 0 until arr.length()) {
            val entity = jsonToKundli(arr.getJSONObject(i))

            if (!replaceExisting) {
                val dup = dao.checkDuplicate(entity.name, entity.date, entity.time, entity.city)
                if (dup > 0) {
                    skippedDuplicates++
                    continue
                }
            }

            if (!SubscriptionManager.isSubscribed(appCtx)) {
                val count = dao.countKundli()
                if (!SubscriptionManager.canSaveAnother(appCtx, count, isUpdatingExisting = false)) {
                    skippedLimit++
                    continue
                }
            }

            dao.insertKundli(entity)
            if (!SubscriptionManager.isSubscribed(appCtx)) {
                SubscriptionManager.recordLifetimeSave(appCtx)
            }
            imported++
        }

        return RestoreResult(
            imported = imported,
            skippedDuplicates = skippedDuplicates,
            skippedLimit = skippedLimit,
            totalInFile = arr.length(),
        )
    }

    fun suggestedBackupFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "${BACKUP_FILE_PREFIX}_$stamp.json"
    }

    private fun kundliToJson(k: KundliEntity): JSONObject =
        JSONObject().apply {
            put("name", k.name)
            put("date", k.date)
            put("time", k.time)
            put("city", k.city)
            put("gender", k.gender)
        }

    private fun jsonToKundli(o: JSONObject): KundliEntity {
        val name = o.optString("name", "").trim()
        val date = o.optString("date", "").trim()
        val time = o.optString("time", "").trim()
        val city = o.optString("city", "").trim()
        if (name.isEmpty() || date.isEmpty() || time.isEmpty() || city.isEmpty()) {
            throw IllegalArgumentException("Backup entry missing name, date, time, or city")
        }
        return KundliEntity(
            name = name,
            date = date,
            time = time,
            city = city,
            gender = o.optString("gender", "").trim(),
        )
    }

    private fun parseAndValidate(jsonText: String): JSONObject {
        if (jsonText.isEmpty()) throw IllegalArgumentException("Backup file is empty")
        val root = JSONObject(jsonText)
        val app = root.optString("app", "").trim()
        if (app != APP_ID) {
            throw IllegalArgumentException("Not a Vidyarthi Lalkitab backup file")
        }
        if (!root.has("kundlis") || root.get("kundlis") !is JSONArray) {
            throw IllegalArgumentException("Invalid backup format")
        }
        return root
    }
}
