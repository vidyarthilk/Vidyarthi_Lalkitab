package com.vidyarthi.lalkitab.ui.kundli

import android.content.Context
import android.content.Intent
import com.vidyarthi.lalkitab.MainActivity
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.navigation.MainKundliNavigator

object KundliFlowLauncher {

    fun open(
        context: Context,
        kundliData: KundliData,
        bottomTabId: Int = R.id.nav_panchang,
        city: String? = null,
        gender: String? = null
    ) {
        KundliHolder.sessionCity = city?.trim()?.takeIf { it.isNotEmpty() }
        KundliHolder.sessionGender = gender?.trim()?.takeIf { it.isNotEmpty() }
        val navigator = context as? MainKundliNavigator
        if (navigator != null) {
            navigator.onKundliOpened(kundliData, bottomTabId)
            return
        }
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_KUNDLI, kundliData)
                putExtra(MainActivity.EXTRA_BOTTOM_TAB, bottomTabId)
            }
        )
    }
}
