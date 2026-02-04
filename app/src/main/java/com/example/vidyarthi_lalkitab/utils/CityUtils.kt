package com.example.vidyarthi_lalkitab.utils

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads city data from assets/cities.json
 * Accurate for Kundli & Panchang calculations.
 */
object CityUtils {

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
    fun getCityByName(context: Context, cityName: String): City? {
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
