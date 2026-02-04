package com.example.vidyarthi_lalkitab.ui.kundli

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.vidyarthi_lalkitab.data.KundliChart
import kotlin.math.min

class KundliChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private var chart: KundliChart? = null

    fun setChart(chart: KundliChart) {
        this.chart = chart
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height)
        val cx = width / 2f
        val cy = height / 2f
        val half = size / 2f

        // Outer diamond
        val path = Path()
        path.moveTo(cx, cy - half)
        path.lineTo(cx + half, cy)
        path.lineTo(cx, cy + half)
        path.lineTo(cx - half, cy)
        path.close()
        canvas.drawPath(path, linePaint)

        // Cross lines
        canvas.drawLine(cx, cy - half, cx, cy + half, linePaint)
        canvas.drawLine(cx - half, cy, cx + half, cy, linePaint)

        // Diagonal lines
        canvas.drawLine(cx - half, cy, cx, cy - half, linePaint)
        canvas.drawLine(cx, cy - half, cx + half, cy, linePaint)
        canvas.drawLine(cx + half, cy, cx, cy + half, linePaint)
        canvas.drawLine(cx, cy + half, cx - half, cy, linePaint)

        drawGrahas(canvas, cx, cy, half)
    }

    private fun drawGrahas(canvas: Canvas, cx: Float, cy: Float, h: Float) {
        val houseCenters = arrayOf(
            Pair(cx, cy - h * 0.75f),        // 1
            Pair(cx + h * 0.35f, cy - h * 0.35f),
            Pair(cx + h * 0.75f, cy),
            Pair(cx + h * 0.35f, cy + h * 0.35f),
            Pair(cx, cy + h * 0.75f),
            Pair(cx - h * 0.35f, cy + h * 0.35f),
            Pair(cx - h * 0.75f, cy),
            Pair(cx - h * 0.35f, cy - h * 0.35f),
            Pair(cx, cy - h * 0.25f),
            Pair(cx + h * 0.25f, cy),
            Pair(cx, cy + h * 0.25f),
            Pair(cx - h * 0.25f, cy)
        )

        chart?.grahas?.groupBy { it.house }?.forEach { (house, list) ->
            val (x, y) = houseCenters[house - 1]
            val text = list.joinToString("\n") { it.name }
            canvas.drawText(text, x, y, textPaint)
        }
    }
}
