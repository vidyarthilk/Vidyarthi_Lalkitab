package com.vidyarthi.lalkitab

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.vidyarthi.lalkitab.ui.newkundli.NewKundliFragment
import com.vidyarthi.lalkitab.ui.savedkundli.SavedKundliFragment

class HomePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NewKundliFragment()
            else -> SavedKundliFragment()
        }
    }
}
