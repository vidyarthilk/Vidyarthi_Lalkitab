package com.vidyarthi.lalkitab

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.vidyarthi.lalkitab.ui.kundli.KundliChartsFragment
import com.vidyarthi.lalkitab.ui.kundli.LalKitabDashaFlowFragment
import com.vidyarthi.lalkitab.ui.kundli.VarshfalFlowFragment
import com.vidyarthi.lalkitab.ui.lalkitab.LalKitabMukhyaGrahFlowFragment
import com.vidyarthi.lalkitab.ui.panchang.PanchangFragment

class KundliFlowPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> PanchangFragment()
        1 -> KundliChartsFragment()
        2 -> VarshfalFlowFragment()
        3 -> LalKitabDashaFlowFragment()
        4 -> LalKitabMukhyaGrahFlowFragment()
        else -> PanchangFragment()
    }
}
