package com.vidyarthi.lalkitab.ui.kundli

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.utils.BudhSvabhavCalculator
import com.vidyarthi.lalkitab.utils.PlanetNames

object BudhSvabhavFormat {

    fun line(ctx: Context, chart: KundliChart): CharSequence = line(ctx, chart.grahas)

    fun line(ctx: Context, grahas: List<GrahaPosition>): CharSequence {
        val r = BudhSvabhavCalculator.compute(grahas)
        val sep = ctx.getString(R.string.mukhya_planet_sep)
        val names = r.svabhavGrahaKeys.joinToString(sep) { PlanetNames.localizedName(ctx, it) }
        val line = ctx.getString(R.string.budh_svabhav_line, names)
        val labelEnd = line.indexOf(':').takeIf { it > 0 } ?: return line
        return SpannableString(line).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
