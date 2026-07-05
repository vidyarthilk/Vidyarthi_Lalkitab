package com.vidyarthi.lalkitab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.vidyarthi.lalkitab.utils.CrashReporting
import com.vidyarthi.lalkitab.utils.SwissEphManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown when Swiss Ephemeris data cannot be loaded — kundli/panchang would not work.
 */
class EphemerisErrorActivity : BaseActivity() {

    override fun isPinchZoomEnabled(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ephemeris_error)

        findViewById<TextView>(R.id.tvEphemerisErrorDetail).text =
            getString(R.string.ephemeris_error_detail)

        val progress = findViewById<ProgressBar>(R.id.progressEphemerisRetry)
        val btnRetry = findViewById<MaterialButton>(R.id.btnEphemerisRetry)
        val btnClose = findViewById<MaterialButton>(R.id.btnEphemerisClose)

        btnRetry.setOnClickListener {
            progress.visibility = View.VISIBLE
            btnRetry.isEnabled = false
            btnClose.isEnabled = false
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    SwissEphManager.initSafe(applicationContext)
                }
                progress.visibility = View.GONE
                btnRetry.isEnabled = true
                btnClose.isEnabled = true
                if (ok) {
                    startActivity(
                        Intent(this@EphemerisErrorActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                } else {
                    SwissEphManager.lastInitError()?.let { error ->
                        CrashReporting.recordException(error)
                    }
                }
            }
        }

        btnClose.setOnClickListener {
            finishAffinity()
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, EphemerisErrorActivity::class.java)
    }
}
