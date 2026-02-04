package com.example.vidyarthi_lalkitab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.vidyarthi_lalkitab.data.entity.KundliEntity

@Dao
interface KundliDao {

    @Insert
    suspend fun insertKundli(kundli: KundliEntity)

    @Query("SELECT * FROM kundli ORDER BY id DESC")
    suspend fun getAllKundli(): List<KundliEntity>
}
