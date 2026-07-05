package com.vidyarthi.lalkitab.navigation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.TabBarUi
import com.vidyarthi.lalkitab.ui.kundli.LalKitabDashaFlowFragment
import com.vidyarthi.lalkitab.ui.lalkitab.LalKitabMukhyaGrahFlowFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Dasha35ModuleHostFragment : Fragment(R.layout.fragment_module_tabs) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titles = listOf(
            getString(R.string.tab_35_varsh_chakkar),
            getString(R.string.tab_raja_vazir_dhoka)
        )

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutModule)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerModule)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> LalKitabDashaFlowFragment()
                else -> LalKitabMukhyaGrahFlowFragment()
            }
        }
        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        for (i in titles.indices) {
            val tab = tabLayout.getTabAt(i)
            val customView = layoutInflater.inflate(R.layout.item_tab, tabLayout, false)
            customView.findViewById<TextView>(R.id.tabText).text = titles[i]
            tab?.customView = customView
            TabBarUi.applyKundliDetailTabStyle(customView, i == 0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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
