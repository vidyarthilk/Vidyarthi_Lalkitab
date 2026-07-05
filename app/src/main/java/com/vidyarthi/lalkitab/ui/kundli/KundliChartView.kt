package com.vidyarthi.lalkitab.ui.kundli

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.utils.PlanetNames
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min

class KundliChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val kalamBold: Typeface =
        ResourcesCompat.getFont(context, R.font.kalam_bold) ?: Typeface.DEFAULT_BOLD

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val grahaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 45f
        textAlign = Paint.Align.CENTER
    }

    init {
        val brandRed = ContextCompat.getColor(context, R.color.brand_red)
        val orangeEmphasis = ContextCompat.getColor(context, R.color.orange_emphasis)
        linePaint.color = brandRed
        textPaint.color = orangeEmphasis
        textPaint.typeface = kalamBold
        grahaPaint.color = orangeEmphasis
        grahaPaint.typeface = kalamBold
        updatePaintScale(resources.getDimensionPixelSize(R.dimen.kundli_chart_min_size))
    }

    private var grahas: List<GrahaPosition> = emptyList()
    private var lagna = 1

    fun setChartData(list: List<GrahaPosition>, lagna: Int) {
        grahas = list
        this.lagna = lagna.coerceIn(1, 12)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(suggestedMinimumWidth)
        val xmlMinPx = suggestedMinimumHeight
        val defaultMinPx = resources.getDimensionPixelSize(R.dimen.kundli_chart_min_size)
        val minSizePx = if (xmlMinPx > 0) xmlMinPx else defaultMinPx
        val desiredHeight = (width * CHART_WIDTH_FRACTION).toInt().coerceAtLeast(minSizePx)

        val measuredHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }

        setMeasuredDimension(width, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePaintScale(min(w, h))
    }

    private fun updatePaintScale(viewSize: Int) {
        if (viewSize <= 0) return
        val refPx = resources.getDimension(R.dimen.kundli_chart_min_size)
        val scale = (viewSize / refPx).coerceIn(0.48f, 1.85f)
        linePaint.strokeWidth = 6f * scale
        textPaint.textSize = 32f * scale
        grahaPaint.textSize = 45f * scale
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height) * 0.9f
        val cx = width / 2f
        val cy = height / 2f
        val h = size / 2

        drawChart(canvas, cx, cy, h)
        val centers = getCenters(cx, cy, h)
        drawRashi(canvas, centers, cx, cy, h)
        drawGrahas(canvas, centers, h)
    }

    private fun drawChart(canvas: Canvas, cx: Float, cy: Float, h: Float) {
        val left = cx - h
        val top = cy - h
        val right = cx + h
        val bottom = cy + h

        canvas.drawRect(left, top, right, bottom, linePaint)

        canvas.drawLine(cx, top, right, cy, linePaint)
        canvas.drawLine(right, cy, cx, bottom, linePaint)
        canvas.drawLine(cx, bottom, left, cy, linePaint)
        canvas.drawLine(left, cy, cx, top, linePaint)

        canvas.drawLine(left, top, right, bottom, linePaint)
        canvas.drawLine(left, bottom, right, top, linePaint)
    }

    private fun getCenters(cx: Float, cy: Float, h: Float): Array<PointF> {
        val unit = h / 2
        return arrayOf(
            PointF(cx, cy - unit),
            PointF(cx - unit, cy - unit * 1.5f),
            PointF(cx - unit * 1.5f, cy - unit),
            PointF(cx - unit, cy),
            PointF(cx - unit * 1.5f, cy + unit),
            PointF(cx - unit, cy + unit * 1.5f),
            PointF(cx, cy + unit),
            PointF(cx + unit, cy + unit * 1.5f),
            PointF(cx + unit * 1.5f, cy + unit),
            PointF(cx + unit, cy),
            PointF(cx + unit * 1.5f, cy - unit),
            PointF(cx + unit, cy - unit * 1.5f)
        )
    }

    private fun drawRashi(canvas: Canvas, centers: Array<PointF>, cx: Float, cy: Float, h: Float) {
        val left = cx - h
        val top = cy - h
        val right = cx + h
        val bottom = cy + h
        for (house in 1..12) {
            val rashi = java.lang.Math.floorMod(lagna + house - 2, 12) + 1
            val p = rashiLabelPosition(house, centers, cx, cy, h)
            drawRashiNumberClamped(
                canvas = canvas,
                label = rashi.toString(),
                x = p.x,
                y = p.y,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                pad = h * 0.04f
            )
        }
    }

    private fun rashiLabelPosition(
        house: Int,
        centers: Array<PointF>,
        cx: Float,
        cy: Float,
        h: Float
    ): PointF {
        val unit = h / 2f
        val inwardT = 0.50f
        val nudge = h * 0.042f
        val centerPull = h * 0.05f
        val ptHalf = h * 0.025f
        val pt1 = h * 0.05f
        val pt1_5 = h * 0.075f
        val pt2 = h * 0.10f
        val pt3 = h * 0.15f
        val pt4 = h * 0.20f
        val pt6 = h * 0.30f
        return when (house) {
            1 -> PointF(cx, cy - unit * (1f - inwardT) + nudge + centerPull)
            4 -> PointF(cx - unit * (1f - inwardT) + nudge + centerPull, cy)
            7 -> PointF(cx, cy + unit * (1f - inwardT) - nudge - centerPull)
            10 -> PointF(cx + unit * (1f - inwardT) - nudge - centerPull, cy)
            2 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x - pt6 + pt1_5, b.y - ptHalf)
            }
            3 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x - pt6, b.y + pt3 + pt3 + pt1_5)
            }
            5 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x - pt6, b.y + pt1_5 + pt3 + pt1_5)
            }
            6 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x - pt3 - pt2 + pt1, b.y + pt2 + pt1 + ptHalf)
            }
            8 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x - pt3 - pt3 + pt1, b.y + pt2 + pt1 + ptHalf)
            }
            9, 11 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x + ptHalf + pt3, b.y + pt4 + pt1_5 + pt2)
            }
            12 -> {
                val b = outerHouseRashiBase(house, centers, cx, cy, h)
                PointF(b.x + pt6 - pt1_5, b.y - ptHalf)
            }
            else -> outerHouseRashiBase(house, centers, cx, cy, h)
        }
    }

    private fun outerHouseRashiBase(
        house: Int,
        centers: Array<PointF>,
        cx: Float,
        cy: Float,
        h: Float
    ): PointF {
        val c = centers[house - 1]
        val dx = c.x - cx
        val dy = c.y - cy
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val push = h * 0.10f
        return PointF(c.x + dx / len * push, c.y + dy / len * push)
    }

    private fun drawRashiNumberClamped(
        canvas: Canvas,
        label: String,
        x: Float,
        y: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        pad: Float
    ) {
        val fm = textPaint.fontMetrics
        val halfW = textPaint.measureText(label) / 2f
        val safeX = x.coerceIn(left + pad + halfW, right - pad - halfW)
        val minBaseline = top + pad - fm.ascent
        val maxBaseline = bottom - pad - fm.descent
        val safeY = y.coerceIn(minBaseline, maxBaseline)
        canvas.drawText(label, safeX, safeY, textPaint)
    }

    private fun drawGrahas(canvas: Canvas, centers: Array<PointF>, h: Float) {
        val grouped: Map<Int, List<GrahaPosition>> = grahas.groupBy { it.house }

        grouped.forEach { (house, list) ->
            if (house !in 1..12) return@forEach

            val c = centers[house - 1]
            val labels = list.take(8).map { shortLabel(it.name) }
            if (labels.isEmpty()) return@forEach

            val largeHouse = isLargeHouse(house)
            val cols = when {
                labels.size == 1 -> 1
                largeHouse -> 2
                labels.size <= 4 -> 1
                else -> 2
            }
            val rows = ceil(labels.size / cols.toFloat()).toInt().coerceAtLeast(1)

            val unit = h / 2f
            val houseWidth = if (largeHouse) unit * 0.92f else unit * 0.62f
            val houseHeight = if (largeHouse) unit * 0.98f else unit * 0.72f

            val cellWidth = (houseWidth / cols) * 0.9f
            val cellHeight = (houseHeight / rows) * 0.9f

            val paint = Paint(grahaPaint)
            val maxLabelWidth = labels.maxOf { paint.measureText(it) }.coerceAtLeast(1f)
            val widthBasedSize = (cellWidth / maxLabelWidth) * paint.textSize
            val heightBasedSize = cellHeight * 0.62f
            paint.textSize = min(min(widthBasedSize, heightBasedSize), grahaPaint.textSize)
                .coerceAtLeast(if (largeHouse) 14f else 10f)

            labels.forEachIndexed { i, label ->
                val row = i / cols
                val col = i % cols
                val rowStep = houseHeight / rows.toFloat()

                val x = if (cols == 1) {
                    c.x
                } else {
                    c.x + if (col == 0) -houseWidth * 0.24f else houseWidth * 0.24f
                }

                val y = c.y - (houseHeight * 0.5f) + (row + 0.5f) * rowStep

                drawCenteredText(canvas, label, x, y, paint)
            }
        }
    }

    private fun isLargeHouse(house: Int): Boolean =
        house == 1 || house == 4 || house == 7 || house == 10

    private fun drawCenteredText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        val yPos = y - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, x, yPos, paint)
    }

    private fun shortLabel(gujaratiName: String): String =
        PlanetNames.shortName(context, gujaratiName)

    companion object {
        /** ચંદ્ર / વર્ષફળ વગેરે: ખાનું ૧ = મેષ (૧) ફ્રેમ. */
        const val LAGNA_RASHI_DISPLAY_FIXED_ONE = 1
        private const val CHART_WIDTH_FRACTION = 0.94f
    }
}
