package com.vidyarthi.lalkitab.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.content.ContextCompat
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.ui.kundli.KundliChartView

object KundliChartBitmap {

    /** Must run on the main thread — [KundliChartView] loads fonts and theme colors. */
    fun render(context: Context, grahas: List<GrahaPosition>, lagnaHouse: Int, widthPx: Int): Bitmap {
        val themed = ContextThemeWrapper(context, R.style.Theme_Vidyarthi_Lalkitab)
        val chart = KundliChartView(themed)
        chart.setChartData(grahas, lagnaHouse)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx.coerceAtLeast(200), View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        chart.measure(widthSpec, heightSpec)
        val w = chart.measuredWidth.coerceAtLeast(1)
        val h = chart.measuredHeight.coerceAtLeast(1)
        chart.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(themed, R.color.cream_white))
        chart.draw(canvas)
        return bitmap
    }
}
