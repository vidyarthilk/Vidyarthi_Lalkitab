package com.vidyarthi.lalkitab.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "cities",
    primaryKeys = ["city", "country"]
)
data class CityEntity(

    val city: String,

    val state: String? = null,

    val country: String,

    @ColumnInfo(name = "lat")
    val latitude: Double?,

    @ColumnInfo(name = "lon")
    val longitude: Double?,

    val timezone: String?,

    val dst: Int?
)