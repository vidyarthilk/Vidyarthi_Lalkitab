package com.vidyarthi.lalkitab.navigation

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.TabBarUi
import com.vidyarthi.lalkitab.ui.kundli.KundliChandraChartFragment
import com.vidyarthi.lalkitab.ui.kundli.KundliJanmaChartFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class KundliModuleHostFragment : Fragment(R.layout.fragment_kundli_janma_chandra_host) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titles = listOf(
            getString(R.string.tab_kundli_janma),
            getString(R.string.tab_kundli_moon)
        )

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutModule)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerModule)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> KundliJanmaChartFragment()
                else -> KundliChandraChartFragment()
            }
        }
        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        for (i in titles.indices) {
            val tab = tabLayout.getTabAt(i)
            val customView = layoutInflater.inflate(R.layout.item_kundli_module_tab_card, tabLayout, false)
            customView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            customView.findViewById<TextView>(R.id.tabText).text = titles[i]
            tab?.customView = customView
            TabBarUi.applyKundliModuleTabCardStyle(customView, i == 0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                TabBarUi.applyKundliModuleTabCardStyle(tab.customView, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                TabBarUi.applyKundliModuleTabCardStyle(tab.customView, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }
}
