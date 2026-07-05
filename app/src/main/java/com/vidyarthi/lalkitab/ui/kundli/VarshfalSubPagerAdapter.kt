package com.vidyarthi.lalkitab.ui.kundli

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class VarshfalSubPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> VarshfalYearFragment()
        1 -> MaasKundliFragment()
        2 -> DivasKundliFragment()
        3 -> KalakKundliFragment()
        else -> VarshfalYearFragment()
    }
}
