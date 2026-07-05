package com.vidyarthi.lalkitab.ui.kundli

import android.view.View
import com.vidyarthi.lalkitab.R
import com.google.android.material.tabs.TabLayout

/** Janma / Moon toggle — chart panels only; no calculation changes. */
object LagnaChandraTabHelper {

    fun setup(
        root: View,
        tabLayoutId: Int = R.id.tabKundliSource,
        panelLagnaId: Int = R.id.panelJanmaKundli,
        panelChandraId: Int = R.id.panelChandraKundli
    ) {
        val tabLayout = root.findViewById<TabLayout>(tabLayoutId) ?: return
        val panelLagna = root.findViewById<View>(panelLagnaId) ?: return
        val panelChandra = root.findViewById<View>(panelChandraId) ?: return

        if (tabLayout.tabCount == 0) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_lagna))
            tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_chandra))
        }

        fun showLagna(showLagna: Boolean) {
            panelLagna.visibility = if (showLagna) View.VISIBLE else View.GONE
            panelChandra.visibility = if (showLagna) View.GONE else View.VISIBLE
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showLagna(tab.position == 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        tabLayout.selectTab(tabLayout.getTabAt(0))
        showLagna(true)
    }
}
