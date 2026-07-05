package com.vidyarthi.lalkitab.navigation

import androidx.fragment.app.Fragment

class KundliChartsTabFragment : KundliContentShellFragment() {
    override fun createContentFragment(): Fragment = KundliModuleHostFragment()
}
