package com.vidyarthi.lalkitab

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.utils.KundliHolder

/** Legacy entry — forwards to [MainActivity] bottom navigation. */
class KundliFlowActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val k: KundliData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_KUNDLI, KundliData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_KUNDLI) as? KundliData
        } ?: KundliHolder.kundliData

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                k?.let { putExtra(MainActivity.EXTRA_KUNDLI, it) }
                putExtra(MainActivity.EXTRA_BOTTOM_TAB, R.id.nav_panchang)
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_KUNDLI = "extra_kundli"
    }
}
