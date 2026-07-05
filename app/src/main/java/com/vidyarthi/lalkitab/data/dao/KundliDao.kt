package com.vidyarthi.lalkitab.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vidyarthi.lalkitab.data.entity.KundliEntity

@Dao
interface KundliDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKundli(kundli: KundliEntity): Long

    @Update
    suspend fun updateKundli(kundli: KundliEntity)

    @Delete
    suspend fun deleteKundli(kundli: KundliEntity)

    @Query("SELECT * FROM kundli ORDER BY id DESC")
    suspend fun getAllKundli(): List<KundliEntity>

    @Query("SELECT COUNT(*) FROM kundli")
    suspend fun countKundli(): Int

    @Query("DELETE FROM kundli")
    suspend fun deleteAllKundli()

    @Query("""
        SELECT COUNT(*) FROM kundli
        WHERE name = :name AND date = :date AND time = :time AND city = :city
    """)
    suspend fun checkDuplicate(
        name: String,
        date: String,
        time: String,
        city: String,
    ): Int
}
