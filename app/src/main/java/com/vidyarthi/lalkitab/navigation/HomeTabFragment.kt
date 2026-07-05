package com.vidyarthi.lalkitab.navigation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.vidyarthi.lalkitab.HomePagerAdapter
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.TabBarUi
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeTabFragment : Fragment(R.layout.fragment_kundli_tab) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabTitles = listOf(
            getString(R.string.tab_new_kundli),
            getString(R.string.tab_saved)
        )

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerKundliHome)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutKundliHome)

        viewPager.adapter = HomePagerAdapter(requireActivity())
        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        for (i in tabTitles.indices) {
            val tab = tabLayout.getTabAt(i)
            val customView = layoutInflater.inflate(R.layout.item_tab, tabLayout, false)
            customView.findViewById<TextView>(R.id.tabText).text = tabTitles[i]
            tab?.customView = customView
            TabBarUi.applySwipeTabStyle(customView, i == 0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                TabBarUi.applySwipeTabStyle(tab.customView, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                TabBarUi.applySwipeTabStyle(tab.customView, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }
}
