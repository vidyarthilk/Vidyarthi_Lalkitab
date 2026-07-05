package com.vidyarthi.lalkitab.update

import android.app.Activity
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.vidyarthi.lalkitab.R

/**
 * Play In-App Updates: prompt on launch, then download in the background (flexible update).
 * Works only for builds installed from Google Play (not sideload/debug APK).
 */
class PlayStoreUpdateHelper(
    private val activity: AppCompatActivity
) : DefaultLifecycleObserver {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var installListener: InstallStateUpdatedListener? = null
    private var updatePromptShownThisSession = false
    private var availableDialog: AlertDialog? = null
    private var readyDialog: AlertDialog? = null

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                unregisterInstallListener()
            }
        }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        resumeInProgressOrCompletedUpdate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        unregisterInstallListener()
        availableDialog?.dismiss()
        readyDialog?.dismiss()
        availableDialog = null
        readyDialog = null
    }

    fun checkForUpdate() {
        if (!isInstalledFromPlayStore()) return

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        showUpdateReadyDialog()
                    }
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        !updatePromptShownThisSession -> {
                        showUpdateAvailableDialog(info)
                    }
                }
            }
    }

    private fun resumeInProgressOrCompletedUpdate() {
        if (!isInstalledFromPlayStore()) return

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        showUpdateReadyDialog()
                    }
                    info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        startFlexibleUpdate(info)
                    }
                }
            }
    }

    private fun showUpdateAvailableDialog(info: AppUpdateInfo) {
        if (availableDialog?.isShowing == true) return
        updatePromptShownThisSession = true

        availableDialog = MaterialAlertDialogBuilder(activity, R.style.Theme_Vidyarthi_Dialog)
            .setTitle(R.string.update_available_title)
            .setMessage(R.string.update_available_message)
            .setPositiveButton(R.string.update_now) { _, _ ->
                startFlexibleUpdate(info)
            }
            .setNegativeButton(R.string.update_later, null)
            .setCancelable(false)
            .show()
    }

    private fun startFlexibleUpdate(info: AppUpdateInfo) {
        if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startImmediateUpdate(info)
            }
            return
        }

        registerInstallListener()

        appUpdateManager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    private fun startImmediateUpdate(info: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    private fun registerInstallListener() {
        if (installListener != null) return
        installListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> showUpdateReadyDialog()
                InstallStatus.FAILED, InstallStatus.CANCELED -> unregisterInstallListener()
                else -> Unit
            }
        }
        installListener?.let { appUpdateManager.registerListener(it) }
    }

    private fun unregisterInstallListener() {
        installListener?.let { appUpdateManager.unregisterListener(it) }
        installListener = null
    }

    private fun showUpdateReadyDialog() {
        if (readyDialog?.isShowing == true) return

        readyDialog = MaterialAlertDialogBuilder(activity, R.style.Theme_Vidyarthi_Dialog)
            .setTitle(R.string.update_download_complete_title)
            .setMessage(R.string.update_download_complete_message)
            .setPositiveButton(R.string.update_restart_now) { _, _ ->
                appUpdateManager.completeUpdate()
            }
            .setCancelable(false)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun isInstalledFromPlayStore(): Boolean {
        val pm = activity.packageManager
        val packageName = activity.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val source = pm.getInstallSourceInfo(packageName)
                source.installingPackageName == PLAY_STORE_PACKAGE ||
                    source.initiatingPackageName == PLAY_STORE_PACKAGE
            }.getOrDefault(false)
        } else {
            val installer = pm.getInstallerPackageName(packageName)
            installer == PLAY_STORE_PACKAGE || installer == "com.google.android.feedback"
        }
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
