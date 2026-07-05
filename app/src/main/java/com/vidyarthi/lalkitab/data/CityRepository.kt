package com.vidyarthi.lalkitab.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.vidyarthi.lalkitab.data.entity.CityEntity
import com.vidyarthi.lalkitab.utils.CityUtils

class CityRepository(context: Context) {

    private val db: SQLiteDatabase
    private val hasStateColumn: Boolean

    init {
        val appCtx = context.applicationContext
        CityUtils.copyDatabase(appCtx)
        val dbPath = appCtx.getDatabasePath("cities.db")
        check(dbPath.isFile && CityUtils.isDatabaseReady(appCtx)) {
            "City database missing. Reinstall the app or ensure assets/database/cities.db is bundled."
        }
        db = SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        hasStateColumn = detectStateColumn()
    }

    private fun detectStateColumn(): Boolean {
        db.rawQuery("PRAGMA table_info(cities)", null).use { c ->
            while (c.moveToNext()) {
                val col = c.getString(1) ?: continue
                if (col.equals("state", ignoreCase = true)) return true
            }
        }
        return false
    }

    /** Prefix then contains match on city name (case-insensitive); India rows first. */
    fun searchCityPicks(query: String, limit: Int = 40): List<CityPick> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val prefix = "$q%"
        val contains = "%$q%"
        val list = linkedSetOf<CityPick>()
        if (hasStateColumn) {
            db.rawQuery(
                """
                SELECT city, state, country, lat, lon, timezone, dst
                FROM cities
                WHERE city LIKE ? COLLATE NOCASE
                ORDER BY CASE WHEN LOWER(country) = 'india' THEN 0 ELSE 1 END,
                         city ASC, country ASC, state ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf(prefix, limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext() && list.size < limit) {
                    list.add(readPickWithState(cursor))
                }
            }
            if (list.size < limit) {
                db.rawQuery(
                    """
                    SELECT city, state, country, lat, lon, timezone, dst
                    FROM cities
                    WHERE city LIKE ? COLLATE NOCASE
                      AND city NOT LIKE ? COLLATE NOCASE
                    ORDER BY CASE WHEN LOWER(country) = 'india' THEN 0 ELSE 1 END,
                             city ASC, country ASC, state ASC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(contains, prefix, (limit - list.size).toString())
                ).use { cursor ->
                    while (cursor.moveToNext() && list.size < limit) {
                        list.add(readPickWithState(cursor))
                    }
                }
            }
        } else {
            db.rawQuery(
                """
                SELECT city, country, lat, lon, timezone, dst
                FROM cities
                WHERE city LIKE ? COLLATE NOCASE
                ORDER BY CASE WHEN LOWER(country) = 'india' THEN 0 ELSE 1 END,
                         city ASC, country ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf(prefix, limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext() && list.size < limit) {
                    list.add(readPickNoState(cursor))
                }
            }
            if (list.size < limit) {
                db.rawQuery(
                    """
                    SELECT city, country, lat, lon, timezone, dst
                    FROM cities
                    WHERE city LIKE ? COLLATE NOCASE
                      AND city NOT LIKE ? COLLATE NOCASE
                    ORDER BY CASE WHEN LOWER(country) = 'india' THEN 0 ELSE 1 END,
                             city ASC, country ASC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(contains, prefix, (limit - list.size).toString())
                ).use { cursor ->
                    while (cursor.moveToNext() && list.size < limit) {
                        list.add(readPickNoState(cursor))
                    }
                }
            }
        }
        return list.toList()
    }

    /** Resolve a line saved from [CityPick.toString] (or typed identically). */
    fun getCityPickByDisplay(display: String): CityPick? {
        val line = display.trim()
        if (line.isEmpty()) return null
        val (city, state, country) = CityRepository.parseDisplayLine(line)
        if (city.isEmpty() || country.isEmpty()) return null

        if (hasStateColumn) {
            val stParsed = state?.trim().orEmpty()

            if (stParsed.isNotEmpty()) {
                db.rawQuery(
                    """
                    SELECT city, state, country, lat, lon, timezone, dst
                    FROM cities
                    WHERE city = ? COLLATE NOCASE AND country = ? COLLATE NOCASE
                      AND TRIM(IFNULL(state, '')) = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(city, country, stParsed)
                ).use { if (it.moveToFirst()) return readPickWithState(it) }
            } else {
                db.rawQuery(
                    """
                    SELECT city, state, country, lat, lon, timezone, dst
                    FROM cities
                    WHERE city = ? COLLATE NOCASE AND country = ? COLLATE NOCASE
                      AND (state IS NULL OR TRIM(state) = '')
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(city, country)
                ).use { if (it.moveToFirst()) return readPickWithState(it) }
            }

            db.rawQuery(
                """
                SELECT city, state, country, lat, lon, timezone, dst
                FROM cities
                WHERE city = ? COLLATE NOCASE AND country = ? COLLATE NOCASE
                LIMIT 1
                """.trimIndent(),
                arrayOf(city, country)
            ).use {
                if (it.moveToFirst()) return readPickWithState(it)
            }
        } else {
            db.rawQuery(
                """
                SELECT city, country, lat, lon, timezone, dst
                FROM cities
                WHERE city = ? COLLATE NOCASE AND country = ? COLLATE NOCASE
                LIMIT 1
                """.trimIndent(),
                arrayOf(city, country)
            ).use {
                if (it.moveToFirst()) return readPickNoState(it)
            }
        }
        return null
    }

    /** Saved kundli city: "city, state, country" / "city, country", or legacy city name only. */
    fun resolveCityForKundli(stored: String): CityInfo? {
        val line = stored.trim()
        if (line.isEmpty()) return null
        if (line.contains(',')) {
            getCityPickByDisplay(line)?.toCityInfo()?.let { return it }
        }
        return lookupByCityColumnOnly(line)
    }

    fun resolveCityPickForKundli(stored: String): CityPick? {
        val line = stored.trim()
        if (line.isEmpty()) return null
        if (line.contains(',')) {
            getCityPickByDisplay(line)?.let { return it }
        }
        return lookupCityPickByNameOnly(line)
    }

    fun getCityByName(cityName: String): CityInfo? = resolveCityForKundli(cityName)

    private fun lookupByCityColumnOnly(name: String): CityInfo? {
        val cursor = db.rawQuery(
            if (hasStateColumn) {
                "SELECT city, state, country, lat, lon, timezone, dst FROM cities WHERE city=? COLLATE NOCASE LIMIT 1"
            } else {
                "SELECT city, country, lat, lon, timezone, dst FROM cities WHERE city=? COLLATE NOCASE LIMIT 1"
            },
            arrayOf(name)
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            val pick = if (hasStateColumn) readPickWithState(it) else readPickNoState(it)
            return pick.toCityInfo()
        }
    }

    private fun lookupCityPickByNameOnly(name: String): CityPick? {
        val cursor = db.rawQuery(
            if (hasStateColumn) {
                "SELECT city, state, country, lat, lon, timezone, dst FROM cities WHERE city=? COLLATE NOCASE LIMIT 1"
            } else {
                "SELECT city, country, lat, lon, timezone, dst FROM cities WHERE city=? COLLATE NOCASE LIMIT 1"
            },
            arrayOf(name)
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            return if (hasStateColumn) readPickWithState(it) else readPickNoState(it)
        }
    }

    fun searchCities(query: String): List<CityEntity> =
        searchCityPicks(query).map { p ->
            CityEntity(
                city = p.city,
                state = p.state,
                country = p.country,
                latitude = p.latitude,
                longitude = p.longitude,
                timezone = p.timezoneId,
                dst = p.dst
            )
        }

    private fun tzHoursFromId(tzId: String?): Double {
        if (tzId.isNullOrBlank()) return 5.5
        return java.util.TimeZone.getTimeZone(tzId).rawOffset / 3_600_000.0
    }

    private fun CityPick.toCityInfo(): CityInfo = CityInfo(
        city = city,
        state = state,
        country = country,
        latitude = latitude,
        longitude = longitude,
        timezone = tzHoursFromId(timezoneId),
        timezoneId = timezoneId?.takeIf { it.isNotBlank() } ?: "Asia/Kolkata"
    )

    private fun readPickWithState(cursor: Cursor): CityPick {
        val tz = cursor.getString(5)
        val dst = if (cursor.isNull(6)) 0 else cursor.getInt(6)
        return CityPick(
            city = cursor.getString(0),
            state = if (cursor.isNull(1)) null else cursor.getString(1),
            country = cursor.getString(2),
            latitude = cursor.getDouble(3),
            longitude = cursor.getDouble(4),
            timezoneId = tz,
            dst = dst
        )
    }

    private fun readPickNoState(cursor: Cursor): CityPick {
        val tz = cursor.getString(4)
        val dst = if (cursor.isNull(5)) 0 else cursor.getInt(5)
        return CityPick(
            city = cursor.getString(0),
            state = null,
            country = cursor.getString(1),
            latitude = cursor.getDouble(2),
            longitude = cursor.getDouble(3),
            timezoneId = tz,
            dst = dst
        )
    }

    companion object {
        /**
         * Split display built by [CityPick.toString].
         * Last segment = country; if three+ segments, second-last = state; rest = city (may contain commas).
         */
        fun parseDisplayLine(display: String): Triple<String, String?, String> {
            val parts = display.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return Triple("", null, "")
            if (parts.size == 1) return Triple(parts[0], null, "")
            val country = parts.last()
            if (parts.size == 2) return Triple(parts[0], null, country)
            val state = parts[parts.lastIndex - 1]
            val city = parts.dropLast(2).joinToString(", ")
            return Triple(city, state, country)
        }
    }
}
