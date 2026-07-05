package com.vidyarthi.lalkitab.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.vidyarthi.lalkitab.data.CityInfo
import com.vidyarthi.lalkitab.data.CityRepository
import com.vidyarthi.lalkitab.data.entity.CityEntity
import com.vidyarthi.lalkitab.utils.CrashReporting
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * Reads city data from assets/cities.json
 * Accurate for Kundli & Panchang calculations.
 */
object CityUtils {

    private const val DB_NAME = "cities.db"
    /** Assets DB બદલાય ત્યારે વધારો — જૂની કૉપી ડિવાઇસ પરથી દૂર થઈ નવી copy થાય. */
    private const val DB_VERSION = 2
    private const val PREFS = "city_db_prefs"
    private const val KEY_DB_VERSION = "cities_db_version"

    /** Returns true when cities.db is present and readable. */
    fun isDatabaseReady(context: Context): Boolean {
        val dbFile = context.applicationContext.getDatabasePath(DB_NAME)
        return dbFile.isFile && dbFile.length() > 0L
    }

    /** [CityRepository] માટે: assets `database/cities.db` → app database. */
    fun copyDatabase(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt(KEY_DB_VERSION, 0)
        val dbFile = context.getDatabasePath(DB_NAME)
        val needsCopy = !dbFile.isFile || storedVersion < DB_VERSION
        if (needsCopy) {
            dbFile.parentFile?.mkdirs()
            if (dbFile.isFile && !dbFile.delete()) {
                Log.w("CITY_DB", "could not remove old cities.db")
            }
            try {
                context.assets.open("database/cities.db").use { input ->
                    FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                }
                prefs.edit().putInt(KEY_DB_VERSION, DB_VERSION).apply()
            } catch (e: Exception) {
                Log.e("CITY_DB", "copy failed: ${e.message}")
                CrashReporting.recordException(e)
                return false
            }
        }
        if (!isDatabaseReady(context)) return false
        applyKnownCityCorrections(dbFile)
        ensureStateColumn(dbFile)
        return true
    }

    /** જૂની DB માં [state] કૉલમ ન હોય તો ઉમેરે છે — [CityRepository] રાજ્ય લાઇન દેખાડી શકે. */
    private fun ensureStateColumn(dbFile: File) {
        if (!dbFile.isFile) return
        try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                if (columnExists(db, "cities", "state")) return
                db.execSQL("ALTER TABLE cities ADD COLUMN state TEXT")
                Log.d("CITY_DB", "cities.state column added")
            }
        } catch (e: Exception) {
            Log.w("CITY_DB", "ensureStateColumn: ${e.message}")
        }
    }

    private fun columnExists(db: SQLiteDatabase, table: String, column: String): Boolean {
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.equals(column, ignoreCase = true)) return true
            }
        }
        return false
    }

    /**
     * જાણીતી ખોટી લાટ/લોન પંક્તિઓ સુધારે છે (પહેલેથી કૉપી થયેલી DB પર પણ ચાલે).
     * "Ganganagar, India" પહેલાં ટ્રિપુરા નજીકના કો-ઓર્ડિનેટ હતા; જ્યોતિષમાં સામાન્ય રીતે શ્રી ગંગાનગર (રાજસ્થાન) લેવાય છે.
     */
    private fun applyKnownCityCorrections(dbFile: File) {
        if (!dbFile.isFile) return
        try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                        UPDATE cities SET lat = 29.92009, lon = 73.87496
                        WHERE city = 'Ganganagar' AND LOWER(country) = 'india'
                          AND ABS(lat - 24.60769) < 0.02 AND ABS(lon - 92.96395) < 0.02
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        UPDATE cities SET lat = 26.9124, lon = 75.7873
                        WHERE city = 'Jaipur' AND LOWER(country) = 'india' AND lon > 85
                        """.trimIndent()
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            Log.w("CITY_DB", "city corrections skipped: ${e.message}")
        }
    }

    data class City(
        val city: String,
        val country: String,
        val latitude: Double,
        val longitude: Double,
        val timezone: String,
        val dst: Boolean
    )

    /**
     * Load and parse full city list from assets.
     */
    fun loadCityData(context: Context): List<City> {
        val jsonString = readJsonFromAssets(context, "cities.json")
        val jsonArray = JSONArray(jsonString)

        val cities = mutableListOf<City>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            cities.add(
                City(
                    city = obj.getString("city"),
                    country = obj.getString("country"),
                    latitude = obj.getDouble("lat"),
                    longitude = obj.getDouble("lng"),
                    timezone = obj.getString("timezone"),
                    dst = obj.getBoolean("dst")
                )
            )
        }

        return cities
    }

    /**
     * For AutoCompleteTextView (city names only)
     */
    fun loadCities(context: Context): List<String> {
        return loadCityData(context).map { it.city }
    }

    /**
     * Get full city object by name
     */
    fun getCityByName(context: Context, cityName: String): CityInfo? =
        CityRepository(context.applicationContext).resolveCityForKundli(cityName)

    fun searchCities(context: Context, query: String): List<CityEntity> =
        CityRepository(context.applicationContext).searchCities(query)

    /** Legacy JSON city list — prefer [searchCities] / SQLite DB. */
    fun getCityByNameFromJson(context: Context, cityName: String): City? {
        return loadCityData(context).find { it.city == cityName }
    }

    /**
     * Internal helper to read JSON file
     */
    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val builder = StringBuilder()

        reader.forEachLine { line ->
            builder.append(line)
        }

        reader.close()
        return builder.toString()
    }
}
