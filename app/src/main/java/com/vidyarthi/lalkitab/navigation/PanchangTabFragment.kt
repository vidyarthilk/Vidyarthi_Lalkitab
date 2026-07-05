package com.vidyarthi.lalkitab.navigation

import androidx.fragment.app.Fragment
import com.vidyarthi.lalkitab.ui.panchang.PanchangFragment

class PanchangTabFragment : KundliContentShellFragment() {
    override fun createContentFragment(): Fragment = PanchangFragment()
}
