package com.vidyarthi.lalkitab.ui.settings

import android.app.AlertDialog
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.db.AppDatabase
import com.vidyarthi.lalkitab.utils.KundliBackupManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class KundliBackupUiHelper(
    private val activity: ComponentActivity,
    private val snackbarAnchor: View,
    private val onRestoreComplete: () -> Unit = {},
) {

    private var pendingRestoreJson: String? = null

    private val createBackupLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) saveBackupToUri(uri)
    }

    private val openBackupLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) readBackupFromUri(uri)
    }

    fun startBackup() {
        activity.lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(activity).kundliDao().countKundli()
            }
            if (count == 0) {
                Snackbar.make(snackbarAnchor, R.string.backup_empty, Snackbar.LENGTH_LONG).show()
                return@launch
            }
            createBackupLauncher.launch(KundliBackupManager.suggestedBackupFileName())
        }
    }

    fun startRestore() {
        openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    private fun saveBackupToUri(uri: Uri) {
        activity.lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    KundliBackupManager.exportToJson(activity)
                }
                val count = JSONObject(json).optInt("count", 0)
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot write backup file")
                }
                Snackbar.make(
                    snackbarAnchor,
                    activity.getString(R.string.backup_success, count),
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Snackbar.make(
                    snackbarAnchor,
                    activity.getString(R.string.backup_failed, e.message ?: "error"),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun readBackupFromUri(uri: Uri) {
        activity.lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    activity.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Cannot read backup file")
                }
                val root = JSONObject(json.trim())
                if (root.optString("app") != KundliBackupManager.APP_ID) {
                    Snackbar.make(snackbarAnchor, R.string.restore_invalid_file, Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                val arr = root.optJSONArray("kundlis") ?: JSONArray()
                pendingRestoreJson = json
                showRestoreDialog(arr.length())
            } catch (e: Exception) {
                Snackbar.make(
                    snackbarAnchor,
                    activity.getString(R.string.restore_failed, e.message ?: "error"),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showRestoreDialog(countInFile: Int) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.restore_dialog_title)
            .setMessage(activity.getString(R.string.restore_dialog_message, countInFile))
            .setPositiveButton(R.string.restore_merge) { _, _ ->
                performRestore(replaceExisting = false)
            }
            .setNegativeButton(R.string.restore_replace) { _, _ ->
                confirmReplaceRestore(countInFile)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ ->
                pendingRestoreJson = null
            }
            .show()
    }

    private fun confirmReplaceRestore(countInFile: Int) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.restore_replace)
            .setMessage(activity.getString(R.string.restore_replace_confirm, countInFile))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                performRestore(replaceExisting = true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                pendingRestoreJson = null
            }
            .show()
    }

    private fun performRestore(replaceExisting: Boolean) {
        val json = pendingRestoreJson ?: return
        pendingRestoreJson = null
        activity.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    KundliBackupManager.restoreFromJson(activity, json, replaceExisting)
                }
                val msg = if (replaceExisting) {
                    activity.getString(R.string.restore_replace_success, result.imported)
                } else {
                    activity.getString(
                        R.string.restore_success,
                        result.imported,
                        result.skippedDuplicates,
                        result.skippedLimit
                    )
                }
                Snackbar.make(snackbarAnchor, msg, Snackbar.LENGTH_LONG).show()
                onRestoreComplete()
            } catch (e: Exception) {
                Snackbar.make(
                    snackbarAnchor,
                    activity.getString(R.string.restore_failed, e.message ?: "error"),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}
