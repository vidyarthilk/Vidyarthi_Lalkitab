package com.vidyarthi.lalkitab.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.vidyarthi.lalkitab.R

object BottomNavUi {

    fun applyBottomNavStyle(customView: View?, selected: Boolean) {
        if (customView == null) return
        val tabRoot = customView.findViewById<View>(R.id.tabRoot) ?: return
        val icon = customView.findViewById<ImageView>(R.id.tabIcon) ?: return
        val text = customView.findViewById<TextView>(R.id.tabText) ?: return
        val ctx = customView.context
        val maroon = ContextCompat.getColor(ctx, R.color.deep_maroon)
        val golden = ContextCompat.getColor(ctx, R.color.golden)

        if (selected) {
            tabRoot.setBackgroundResource(R.drawable.bg_bottom_nav_pill_active)
            text.typeface = ResourcesCompat.getFont(ctx, R.font.kalam_bold) ?: text.typeface
            text.setTextColor(maroon)
            icon.alpha = 1f
            icon.setColorFilter(golden)
        } else {
            tabRoot.background = null
            text.typeface = ResourcesCompat.getFont(ctx, R.font.kalam_regular) ?: text.typeface
            text.setTextColor(maroon)
            icon.alpha = 0.78f
            icon.clearColorFilter()
        }
    }
}
