package com.example.vidyarthi_lalkitab.utils

import android.content.Context
import swisseph.SwissEph
import java.io.File

object SwissEphManager {

    lateinit var sw: SwissEph
        private set

    fun init(context: Context) {
        val epheDir = File(context.filesDir, "ephe")
        if (!epheDir.exists()) epheDir.mkdirs()
        sw = SwissEph(epheDir.absolutePath)
    }
}
