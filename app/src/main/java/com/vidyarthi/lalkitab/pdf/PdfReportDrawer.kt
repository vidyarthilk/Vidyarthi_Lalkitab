package com.vidyarthi.lalkitab.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.kundli.GrahaRashiTableHelper

/** Premium Lal Kitab PDF styling (maroon / gold / cream). */
internal object PdfReportDrawer {

    const val PAGE_W = 595f
    const val PAGE_H = 842f
    const val MARGIN = 36f
    const val HEADER_BAND_H = 78f
    const val FOOTER_H = 36f
    const val CARD_RADIUS = 10f

    val colorMaroon = 0xFF7A1010.toInt()
    val colorGold = 0xFFE8C85C.toInt()
    val colorCream = 0xFFFFFDF8.toInt()
    val colorCreamCard = 0xFFFFF5EA.toInt()
    val colorText = 0xFF3D1810.toInt()
    val colorMuted = 0xFF9A5A4A.toInt()
    val colorSaffron = 0xFFF4A623.toInt()
    val colorRowAlt = 0xFFFFF0D0.toInt()
    val colorWhite = 0xFFFFFFFF.toInt()

    fun contentWidth(): Float = PAGE_W - MARGIN * 2f
    fun contentTop(): Float = HEADER_BAND_H + 14f
    fun contentBottom(): Float = PAGE_H - FOOTER_H - 8f

    fun drawPageBackground(c: Canvas) {
        c.drawColor(colorCream)
        val border = RectF(18f, 18f, PAGE_W - 18f, PAGE_H - 18f)
        c.drawRoundRect(border, 12f, 12f, strokePaint(colorGold, 1.5f))
    }

    fun drawHeaderBand(
        c: Canvas,
        titleLine1: String,
        titleLine2: String?,
        titleLine3: String?,
        rightCaption: String? = null,
        logo: android.graphics.Bitmap? = null
    ): Float {
        val band = RectF(0f, 0f, PAGE_W, HEADER_BAND_H)
        c.drawRect(band, fillPaint(colorMaroon))
        c.drawRect(RectF(0f, HEADER_BAND_H - 3f, PAGE_W, HEADER_BAND_H), fillPaint(colorGold))

        logo?.let { bmp ->
            val size = 44f
            val dest = RectF(MARGIN, 14f, MARGIN + size, 14f + size)
            c.drawBitmap(bmp, null, dest, null)
        }

        val textLeft = if (logo != null) MARGIN + 52f else MARGIN

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorWhite
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGold
            textSize = 10f
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEDD9A0.toInt()
            textSize = 9.5f
        }

        var y = 28f
        c.drawText(titleLine1, textLeft, y, titlePaint)
        y += 16f
        titleLine2?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, textLeft, y, subPaint)
            y += 13f
        }
        titleLine3?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, textLeft, y, metaPaint)
        }

        rightCaption?.takeIf { it.isNotBlank() }?.let { cap ->
            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorGold
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }
            val capBold = Paint(capPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11f
            }
            val lines = cap.split('\n')
            var ry = 24f
            for ((i, line) in lines.withIndex()) {
                c.drawText(line, PAGE_W - MARGIN, ry, if (i == 0) capBold else capPaint)
                ry += 14f
            }
        }
        return HEADER_BAND_H + 10f
    }

    fun drawSectionHeader(c: Canvas, title: String, y: Float): Float {
        val barH = 26f
        val rect = RectF(MARGIN, y, PAGE_W - MARGIN, y + barH)
        drawRoundRect(c, rect, 6f, colorMaroon, null, 0f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorWhite
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(title, MARGIN + 12f, y + 18f, paint)
        return y + barH + 10f
    }

    fun drawCard(
        c: Canvas,
        top: Float,
        height: Float,
        strokeGold: Boolean = true
    ): RectF {
        val rect = RectF(MARGIN, top, PAGE_W - MARGIN, top + height)
        drawRoundRect(c, rect, CARD_RADIUS, colorCreamCard, if (strokeGold) colorGold else null, 1.2f)
        return rect
    }

    fun drawLabelValueGrid(
        c: Canvas,
        rows: List<Pair<String, String>>,
        x: Float,
        y: Float,
        width: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ): Float {
        val colLabel = width * 0.34f
        var cy = y
        val rowH = 18f
        for ((label, value) in rows) {
            c.drawText(label, x, cy, labelPaint)
            drawWrapped(c, value, x + colLabel, cy - 11f, valuePaint, width - colLabel - 8f, rowH)
            cy += rowH + 6f
        }
        return cy
    }

    fun drawSubheading(c: Canvas, text: String, x: Float, y: Float, paint: Paint): Float {
        val p = Paint(paint).apply {
            color = colorMaroon
            isFakeBoldText = true
            textSize = 11f
        }
        c.drawText(text, x, y, p)
        return y + 16f
    }

    fun drawBulletLines(
        c: Canvas,
        lines: List<String>,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float,
        bullet: Boolean = true
    ): Float {
        var y = startY
        val bulletPaint = Paint(paint).apply { color = colorSaffron; textSize = paint.textSize + 1f }
        for (line in lines) {
            if (line.isBlank()) {
                y += 8f
                continue
            }
            if (bullet) {
                c.drawText("•", x, y, bulletPaint)
                y = drawWrapped(c, line, x + 12f, y - 11f, paint, maxWidth - 14f, 15f) + 4f
            } else {
                y = drawWrapped(c, line, x, y - 11f, paint, maxWidth, 15f) + 4f
            }
        }
        return y
    }

    fun drawChartFrame(c: Canvas, dest: RectF) {
        val pad = 6f
        val outer = RectF(dest.left - pad, dest.top - pad, dest.right + pad, dest.bottom + pad)
        drawRoundRect(c, outer, 8f, colorWhite, colorGold, 1.8f)
        val inner = RectF(outer.left + 3f, outer.top + 3f, outer.right - 3f, outer.bottom - 3f)
        drawRoundRect(c, inner, 6f, colorCreamCard, colorMaroon, 0.6f)
    }

    fun drawGrahaRashiTable(
        c: Canvas,
        ctx: Context,
        rows: List<GrahaRashiTableHelper.Row>,
        y: Float
    ): Float {
        val x = MARGIN
        val totalW = contentWidth()
        val colW = listOf(totalW * 0.36f, totalW * 0.36f, totalW * 0.28f)
        var cy = drawSubheading(c, ctx.getString(R.string.graha_table_title), x + 4f, y + 4f, bodyPaint())
        cy = drawTableHeader(
            c,
            listOf(
                ctx.getString(R.string.graha_table_col_graha),
                ctx.getString(R.string.graha_table_col_rashi),
                ctx.getString(R.string.graha_table_col_degree)
            ),
            x, cy + 8f, colW
        )
        val rowPaint = smallPaint()
        var alt = false
        for (row in rows) {
            cy = drawTableRow(c, listOf(row.graha, row.rashi, row.degree), x, cy, colW, rowPaint, alt)
            alt = !alt
        }
        return cy + 4f
    }

    fun drawTableHeader(c: Canvas, columns: List<String>, x: Float, y: Float, colWidths: List<Float>): Float {
        var cx = x
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorWhite
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowH = 22f
        val totalW = colWidths.sum()
        c.drawRect(RectF(x, y - 14f, x + totalW, y - 14f + rowH), fillPaint(colorMaroon))
        for (i in columns.indices) {
            c.drawText(columns[i], cx + 4f, y + 2f, headerPaint)
            cx += colWidths[i]
        }
        return y - 14f + rowH + 4f
    }

    fun drawTableRow(
        c: Canvas,
        cells: List<String>,
        x: Float,
        y: Float,
        colWidths: List<Float>,
        paint: Paint,
        alt: Boolean
    ): Float {
        val rowH = 18f
        val totalW = colWidths.sum()
        if (alt) {
            c.drawRect(RectF(x, y - 12f, x + totalW, y - 12f + rowH), fillPaint(colorRowAlt))
        }
        var cx = x
        for (i in cells.indices) {
            val cell = cells.getOrElse(i) { "" }
            drawWrapped(c, cell, cx + 4f, y - 11f, paint, colWidths[i] - 8f, rowH)
            cx += colWidths[i]
        }
        return y + rowH + 2f
    }

    fun drawTwoColumnPanels(
        c: Canvas,
        leftTitle: String,
        leftLines: List<String>,
        rightTitle: String,
        rightLines: List<String>,
        y: Float,
        bodyPaint: Paint
    ): Float {
        val gap = 10f
        val halfW = (contentWidth() - gap) / 2f
        val leftRect = RectF(MARGIN, y, MARGIN + halfW, y + 120f)
        val rightRect = RectF(MARGIN + halfW + gap, y, PAGE_W - MARGIN, y + 120f)
        drawPanel(c, leftRect, leftTitle, leftLines, bodyPaint)
        drawPanel(c, rightRect, rightTitle, rightLines, bodyPaint)
        return y + 128f
    }

    private fun drawPanel(
        c: Canvas,
        rect: RectF,
        title: String,
        lines: List<String>,
        bodyPaint: Paint
    ) {
        drawRoundRect(c, rect, CARD_RADIUS, colorCreamCard, colorGold, 1f)
        val titlePaint = Paint(bodyPaint).apply {
            color = colorMaroon
            isFakeBoldText = true
            textSize = 11f
        }
        var y = rect.top + 18f
        c.drawText(title, rect.left + 10f, y, titlePaint)
        y += 14f
        val small = Paint(bodyPaint).apply { textSize = 9.5f }
        for (line in lines) {
            y = drawWrapped(c, line, rect.left + 10f, y - 10f, small, rect.width() - 20f, 14f) + 2f
            if (y > rect.bottom - 6f) break
        }
    }

    fun drawFooter(c: Canvas, footerText: String, pageNumber: Int) {
        val y = PAGE_H - FOOTER_H
        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, strokePaint(colorGold, 1f))
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMuted
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        c.drawText(footerText, PAGE_W / 2f, y + 16f, centerPaint)
        val pagePaint = Paint(centerPaint).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(pageNumber.toString(), PAGE_W - MARGIN, y + 16f, pagePaint)
    }

    fun bodyPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorText
        textSize = 10.5f
    }

    fun labelPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorMuted
        textSize = 10f
        isFakeBoldText = true
    }

    fun smallPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorText
        textSize = 9f
    }

    fun drawWrapped(
        c: Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float
    ): Float {
        val words = text.split(' ')
        var line = StringBuilder()
        var y = startY + lineHeight
        for (word in words) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > maxWidth && line.isNotEmpty()) {
                c.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(trial)
            }
        }
        if (line.isNotEmpty()) {
            c.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun drawRoundRect(
        c: Canvas,
        rect: RectF,
        radius: Float,
        fill: Int,
        stroke: Int?,
        strokeW: Float
    ) {
        c.drawRoundRect(rect, radius, radius, fillPaint(fill))
        if (stroke != null) {
            c.drawRoundRect(rect, radius, radius, strokePaint(stroke, strokeW))
        }
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }
}
