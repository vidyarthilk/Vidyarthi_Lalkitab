package com.vidyarthi.lalkitab.ui

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.vidyarthi.lalkitab.R

object TabBarUi {

    fun applySwipeTabStyle(customView: View?, selected: Boolean) {
        if (customView == null) return
        val text = customView.findViewById<TextView>(R.id.tabText) ?: return
        val underline = customView.findViewById<View>(R.id.tabUnderline) ?: return
        val ctx = customView.context
        val maroon = ContextCompat.getColor(ctx, R.color.deep_maroon)

        text.typeface = ResourcesCompat.getFont(
            ctx,
            if (selected) R.font.kalam_bold else R.font.kalam_regular
        ) ?: text.typeface

        if (selected) {
            customView.setBackgroundResource(R.drawable.bg_tab_active_gold_pill)
            text.setTextColor(maroon)
            underline.visibility = View.GONE
        } else {
            customView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            text.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            underline.visibility = View.GONE
        }
    }

    fun applyKundliDetailTabStyle(customView: View?, selected: Boolean) {
        if (customView == null) return
        val text = customView.findViewById<TextView>(R.id.tabText) ?: return
        val underline = customView.findViewById<View>(R.id.tabUnderline) ?: return
        val ctx = customView.context
        val maroon = ContextCompat.getColor(ctx, R.color.deep_maroon)

        text.typeface = ResourcesCompat.getFont(
            ctx,
            if (selected) R.font.kalam_bold else R.font.kalam_regular
        ) ?: text.typeface

        if (selected) {
            customView.setBackgroundResource(R.drawable.bg_tab_active_gold)
            text.setTextColor(maroon)
            underline.visibility = View.GONE
        } else {
            customView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            text.setTextColor(ColorUtils.setAlphaComponent(maroon, (255 * 0.65f).toInt()))
            underline.visibility = View.GONE
        }
    }

    fun applyKundliModuleTabCardStyle(customView: View?, selected: Boolean) {
        if (customView == null) return
        val text = customView.findViewById<TextView>(R.id.tabText) ?: return
        val ctx = customView.context
        val maroon = ContextCompat.getColor(ctx, R.color.deep_maroon)

        text.typeface = ResourcesCompat.getFont(ctx, R.font.kalam_bold) ?: text.typeface

        if (selected) {
            customView.setBackgroundResource(R.drawable.bg_tab_active_gold_pill)
            text.setTextColor(maroon)
        } else {
            customView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            text.setTextColor(ColorUtils.setAlphaComponent(maroon, (255 * 0.65f).toInt()))
        }
    }
}
