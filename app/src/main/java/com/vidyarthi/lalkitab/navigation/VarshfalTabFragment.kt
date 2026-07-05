package com.vidyarthi.lalkitab.navigation

import androidx.fragment.app.Fragment
import com.vidyarthi.lalkitab.ui.kundli.VarshfalFlowFragment

class VarshfalTabFragment : KundliContentShellFragment() {
    override fun createContentFragment(): Fragment = VarshfalFlowFragment()
}
