package com.vidyarthi.lalkitab.ui.kundli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.TabBarUi
import androidx.viewpager2.widget.ViewPager2

class VarshfalFlowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_varshfal_flow, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pager = view.findViewById<ViewPager2>(R.id.varshfalViewPager)
        val tabs = view.findViewById<TabLayout>(R.id.varshfalTabLayout)
        pager.adapter = VarshfalSubPagerAdapter(this)
        pager.offscreenPageLimit = 3

        val titles = listOf(
            getString(R.string.varshfal_sub_year),
            getString(R.string.varshfal_sub_month),
            getString(R.string.varshfal_sub_day),
            getString(R.string.varshfal_sub_kalak)
        )

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        for (i in titles.indices) {
            val tab = tabs.getTabAt(i)
            val customView = layoutInflater.inflate(R.layout.item_tab, tabs, false)
            customView.findViewById<TextView>(R.id.tabText).text = titles[i]
            tab?.customView = customView
            TabBarUi.applyKundliDetailTabStyle(customView, i == 0)
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                TabBarUi.applyKundliDetailTabStyle(tab.customView, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                TabBarUi.applyKundliDetailTabStyle(tab.customView, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }
}
