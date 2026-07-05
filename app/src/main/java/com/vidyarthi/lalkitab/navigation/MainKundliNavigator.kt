package com.vidyarthi.lalkitab.navigation

import com.vidyarthi.lalkitab.data.KundliData

interface MainKundliNavigator {
    fun onKundliOpened(data: KundliData, bottomTabId: Int? = null)
    fun switchToBottomTab(bottomTabId: Int)
}
