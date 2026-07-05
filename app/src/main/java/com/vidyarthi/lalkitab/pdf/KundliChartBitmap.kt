package com.vidyarthi.lalkitab.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.ui.kundli.KundliChartView

object KundliChartBitmap {

    fun render(context: Context, grahas: List<GrahaPosition>, lagnaHouse: Int, widthPx: Int): Bitmap {
        val chart = KundliChartView(context)
        chart.setChartData(grahas, lagnaHouse)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        chart.measure(widthSpec, heightSpec)
        chart.layout(0, 0, chart.measuredWidth, chart.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            chart.measuredWidth.coerceAtLeast(1),
            chart.measuredHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        chart.draw(Canvas(bitmap))
        return bitmap
    }
}
