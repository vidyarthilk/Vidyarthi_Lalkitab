package com.vidyarthi.lalkitab.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.vidyarthi.lalkitab.data.entity.CityEntity

@Dao
interface CityDao {

    @Query("""
    SELECT * FROM cities WHERE city LIKE :query || '%' ORDER BY city ASC LIMIT 50 """)
    suspend fun searchCities(query: String): List<CityEntity>

    @Query("SELECT * FROM cities WHERE city = :cityName LIMIT 1")
    suspend fun getCityEntityByName(cityName: String): CityEntity?
}
