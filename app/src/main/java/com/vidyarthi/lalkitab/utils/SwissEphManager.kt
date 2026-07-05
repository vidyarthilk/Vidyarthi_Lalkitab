package com.vidyarthi.lalkitab.utils

import android.content.Context
import swisseph.SwissEph
import swisseph.SweConst
import java.io.File
import java.io.FileOutputStream

object SwissEphManager {

    private var eph: SwissEph? = null
    private var lastInitError: Throwable? = null

    /** [KundliEngine] માટે — [get] સમાન. */
    val sw: SwissEph get() = get()

    fun isInitialized(): Boolean = eph != null

    fun lastInitError(): Throwable? = lastInitError

    /** Initializes ephemeris; returns false instead of crashing the app process. */
    fun initSafe(context: Context): Boolean {
        return try {
            init(context)
            lastInitError = null
            true
        } catch (e: Exception) {
            eph = null
            lastInitError = e
            false
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (eph != null) return

        val appContext = context.applicationContext
        val epheDir = File(appContext.filesDir, "ephe")
        if (epheDir.exists()) {
            epheDir.listFiles()?.forEach { f ->
                if (f.isFile && (f.name.endsWith(".c", ignoreCase = true) ||
                            f.name.endsWith(".h", ignoreCase = true))
                ) {
                    f.delete()
                }
            }
        }

        if (!epheDir.exists() || !ephemerisCopyPresent(epheDir)) {
            if (!epheDir.exists() && !epheDir.mkdirs() && !epheDir.isDirectory) {
                throw IllegalStateException("ephe directory not creatable: ${epheDir.absolutePath}")
            }
            if (!epheDir.isDirectory) {
                throw IllegalStateException("ephe is not a directory: ${epheDir.absolutePath}")
            }
            copyEphemerisFromAssets(appContext, epheDir)
        }

        if (!ephemerisCopyPresent(epheDir)) {
            throw IllegalStateException("ephemeris files missing after copy from assets")
        }

        val instance = SwissEph()
        instance.swe_set_ephe_path(epheDir.absolutePath)
        instance.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        instance.swe_set_topo(0.0, 0.0, 0.0)
        eph = instance
    }

    private fun ephemerisCopyPresent(epheDir: File): Boolean {
        if (!epheDir.isDirectory) return false
        val marker = File(epheDir, "sefstars.txt")
        return marker.isFile && marker.length() > 0L
    }

    private fun copyEphemerisFromAssets(context: Context, epheDir: File) {
        val assetNames = context.assets.list("ephe")
            ?: throw IllegalStateException("assets/ephe folder missing from APK")
        if (assetNames.isEmpty()) {
            throw IllegalStateException("assets/ephe contains no ephemeris files")
        }
        assetNames.forEach { fileName ->
            if (fileName.endsWith(".c", ignoreCase = true) ||
                fileName.endsWith(".h", ignoreCase = true)
            ) {
                return@forEach
            }
            val outFile = File(epheDir, fileName)
            if (outFile.isFile && outFile.length() > 0L) return@forEach
            context.assets.open("ephe/$fileName").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun get(): SwissEph =
        eph ?: throw IllegalStateException("SwissEph not initialized — call SwissEphManager.init(context) first")
}
