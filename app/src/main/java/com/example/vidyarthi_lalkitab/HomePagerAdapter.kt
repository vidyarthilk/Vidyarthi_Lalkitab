package com.example.vidyarthi_lalkitab

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.vidyarthi_lalkitab.ui.newkundli.NewKundliFragment
import com.example.vidyarthi_lalkitab.ui.savedkundli.SavedKundliFragment

class HomePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NewKundliFragment()
            else -> SavedKundliFragment()
        }
    }
}
