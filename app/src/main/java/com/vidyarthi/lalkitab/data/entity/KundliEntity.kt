package com.vidyarthi.lalkitab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kundli")
data class KundliEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val date: String,
    val time: String,
    val city: String,
    val gender: String
)
