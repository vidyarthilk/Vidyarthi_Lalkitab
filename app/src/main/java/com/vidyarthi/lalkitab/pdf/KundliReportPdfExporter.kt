package com.vidyarthi.lalkitab.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment
import com.vidyarthi.lalkitab.subscription.SubscriberProfileManager
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import com.vidyarthi.lalkitab.ui.kundli.BudhSvabhavFormat
import com.vidyarthi.lalkitab.ui.kundli.KundliChartView
import com.vidyarthi.lalkitab.utils.BirthPlanetsHelper
import com.vidyarthi.lalkitab.utils.DhokaNaGrahHelper
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.LalKitabDashaDates
import com.vidyarthi.lalkitab.utils.LalKitabDashaExpand
import com.vidyarthi.lalkitab.utils.PanchangLabelResolver
import com.vidyarthi.lalkitab.utils.PanchangUiState
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.VarshfalRajaVazirHelper
import com.vidyarthi.lalkitab.utils.VarshfalTable
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object KundliReportPdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN_H = 40f
    private const val HEADER_H = 72f
    private const val FOOTER_H = 28f

    fun export(
        context: Context,
        k: KundliData,
        city: String?,
        gender: String?
    ): File {
        VarshfalTable.load(context)
        val panchang = PanchangReportLoader.load(k)
        val subscribed = SubscriptionManager.isSubscribed(context)
        val profile = if (subscribed) SubscriberProfileManager.load(context) else null

        val doc = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }

        addBirthDetailsPage(doc, context, k, city, gender, subscribed, profile, titlePaint, bodyPaint)
        addPanchangPage(doc, context, panchang, subscribed, profile, titlePaint, bodyPaint)
        addChartPage(
            doc, context, k,
            context.getString(R.string.pdf_page_janma_chart),
            janma = true,
            subscribed, profile, titlePaint, bodyPaint, smallPaint
        )
        addChartPage(
            doc, context, k,
            context.getString(R.string.pdf_page_chandra_chart),
            janma = false,
            subscribed, profile, titlePaint, bodyPaint, smallPaint
        )
        addVarshfalPage(doc, context, k, subscribed, profile, titlePaint, bodyPaint, smallPaint)
        addCurrentDashaPage(doc, context, k, subscribed, profile, titlePaint, bodyPaint)
        addAntardashaPage(doc, context, k, subscribed, profile, titlePaint, bodyPaint)
        addRajaVazirDhokaPage(doc, context, k, subscribed, profile, titlePaint, bodyPaint)

        val safeName = k.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "kundli" }
        val out = File(context.cacheDir, "kundli_report_$safeName.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }

    private fun addBirthDetailsPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        city: String?,
        gender: String?,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint
    ) {
        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 12f
        y = drawSectionTitle(c, ctx.getString(R.string.pdf_page_birth_details), MARGIN_H, y, titlePaint)
        val dob = String.format("%02d/%02d/%04d", k.day, k.month, k.year)
        val tob = String.format("%02d:%02d", k.hour, k.minute)
        val lines = buildList {
            add(ctx.getString(R.string.pdf_label_name, k.name))
            add(ctx.getString(R.string.pdf_label_dob, dob))
            add(ctx.getString(R.string.pdf_label_tob, tob))
            if (!city.isNullOrBlank()) add(ctx.getString(R.string.pdf_label_city, city))
            if (!gender.isNullOrBlank()) add(ctx.getString(R.string.pdf_label_gender, gender))
        }
        y = drawLines(c, lines, MARGIN_H, y, bodyPaint, contentWidth())
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addPanchangPage(
        doc: PdfDocument,
        ctx: Context,
        state: PanchangUiState,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint
    ) {
        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 12f
        y = drawSectionTitle(c, ctx.getString(R.string.panchang_title), MARGIN_H, y, titlePaint)
        val lines = mutableListOf<String>()
        state.dayInfo?.let { d ->
            lines.add("${d.weekday}")
            lines.add(ctx.getString(R.string.pdf_sunrise_sunset, d.sunrise, d.sunset))
            d.moonrise?.let { mr ->
                lines.add(ctx.getString(R.string.pdf_moonrise_set, mr, d.moonset.orEmpty()))
            }
            lines.add(ctx.getString(R.string.pdf_day_night, d.dayLength, d.nightLength))
        }
        state.tithiInfo?.let { t ->
            lines.add(ctx.getString(R.string.pdf_tithi, t.janmaTithi, t.endTime))
        }
        state.nakshatraInfo?.let { n ->
            val name = PanchangLabelResolver.nakshatra(ctx, n.nakshatraIndex)
            lines.add(ctx.getString(R.string.pdf_nakshatra, name, n.pada, n.endTime))
        }
        state.pakshaInfo?.let { p ->
            lines.add(ctx.getString(R.string.pdf_paksha, p.paksha, p.tithi))
        }
        state.vikramSamvat?.let { vs ->
            lines.add(ctx.getString(R.string.pdf_vikram_samvat, vs))
        }
        state.birthdayPlanetKey?.let { key ->
            lines.add(ctx.getString(R.string.panchang_birth_day_planet_line, PlanetNames.localizedName(ctx, key)))
        }
        state.samayGrahaDashaName?.let { key ->
            val house = state.samayGrahaJanmaHouse ?: 1
            lines.add(ctx.getString(R.string.panchang_birth_time_planet_line, PlanetNames.localizedName(ctx, key)))
            lines.add(ctx.getString(R.string.pdf_samay_house, PlanetNames.localizedName(ctx, key), house))
        }
        y = drawLines(c, lines, MARGIN_H, y, bodyPaint, contentWidth())
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addChartPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        pageTitle: String,
        janma: Boolean,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint,
        smallPaint: Paint
    ) {
        val chart = if (janma) KundliEngine.calculate(k) else KundliEngine.calculateChandraKundli(k)
        val lagna = if (janma) chart.lagnaHouse else KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE
        val bitmap = KundliChartBitmap.render(ctx, chart.grahas, lagna, chartWidthPx())
        val budh = BudhSvabhavFormat.line(ctx, chart.grahas)

        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 8f
        y = drawSectionTitle(c, pageTitle, MARGIN_H, y, titlePaint)

        val maxW = contentWidth()
        val scale = maxW / bitmap.width.toFloat()
        val imgH = bitmap.height * scale
        val dest = RectF(MARGIN_H, y, MARGIN_H + maxW, y + imgH)
        c.drawBitmap(bitmap, null, dest, null)
        y = dest.bottom + 10f
        drawLines(c, listOf(budh.toString()), MARGIN_H, y, smallPaint, maxW)
        bitmap.recycle()
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addVarshfalPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint,
        smallPaint: Paint
    ) {
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        val lifeYear = KundliEngine.getCurrentVarshfalYear(dob)
        val janmaChart = KundliEngine.calculate(k)
        val chandraBase = KundliEngine.calculateChandraKundli(k).grahas
        val janmaVarshfal = KundliEngine.applyLalKitabVarshfal(ctx, janmaChart.grahas, lifeYear)
        val chandraVarshfal = KundliEngine.applyLalKitabVarshfal(ctx, chandraBase, lifeYear)
        val (start, end) = KundliEngine.getVarshfalDateRange(dob, lifeYear)
        val range = KundliEngine.getFormattedRange(start, end)

        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 8f
        y = drawSectionTitle(c, ctx.getString(R.string.pdf_page_varshfal), MARGIN_H, y, titlePaint)
        y = drawLines(
            c,
            listOf(ctx.getString(R.string.pdf_varshfal_year, lifeYear, range)),
            MARGIN_H, y, bodyPaint, contentWidth()
        ) + 6f

        val halfW = (contentWidth() - 8f) / 2f
        val janmaBmp = KundliChartBitmap.render(
            ctx, janmaVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE, halfW.toInt()
        )
        val chandraBmp = KundliChartBitmap.render(
            ctx, chandraVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE, halfW.toInt()
        )
        val scaleJ = halfW / janmaBmp.width
        val scaleC = halfW / chandraBmp.width
        val hJ = janmaBmp.height * scaleJ
        val hC = chandraBmp.height * scaleC
        c.drawText(ctx.getString(R.string.pdf_janma), MARGIN_H, y + 12f, smallPaint)
        c.drawText(ctx.getString(R.string.pdf_chandra), MARGIN_H + halfW + 8f, y + 12f, smallPaint)
        y += 16f
        c.drawBitmap(janmaBmp, null, RectF(MARGIN_H, y, MARGIN_H + halfW, y + hJ), null)
        c.drawBitmap(chandraBmp, null, RectF(MARGIN_H + halfW + 8f, y, PAGE_W - MARGIN_H, y + hC), null)
        janmaBmp.recycle()
        chandraBmp.recycle()
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addCurrentDashaPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k)
        val samay = resolveSamayGraha(k)
        val result = LalKitabDashaCalculator.compute(k, samayGrahaName = samay) ?: return
        val seg = result.segments.firstOrNull { lifeYear in it.startYear..it.endYear } ?: return
        val (d0, d1) = LalKitabDashaDates.formatSegmentRange(k, seg)

        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 12f
        y = drawSectionTitle(c, ctx.getString(R.string.lalkitab_dasha_screen_title), MARGIN_H, y, titlePaint)
        val lines = listOf(
            ctx.getString(R.string.pdf_current_life_year, lifeYear),
            ctx.getString(R.string.pdf_samay_graha, PlanetNames.localizedName(ctx, samay)),
            ctx.getString(
                R.string.pdf_mahadasha_line,
                PlanetNames.localizedName(ctx, seg.planetName),
                seg.startYear,
                seg.endYear,
                d0,
                d1
            )
        )
        drawLines(c, lines, MARGIN_H, y, bodyPaint, contentWidth())
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addAntardashaPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k).toDouble()
        val samay = resolveSamayGraha(k)
        val result = LalKitabDashaCalculator.compute(k, samayGrahaName = samay) ?: return
        val seg = result.segments.firstOrNull { lifeYear.toInt() in it.startYear..it.endYear } ?: return
        val antars = LalKitabDashaExpand.antardashasForSegment(seg)
        if (antars.isEmpty()) return

        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 12f
        y = drawSectionTitle(c, ctx.getString(R.string.pdf_page_antardasha), MARGIN_H, y, titlePaint)
        val lines = antars.map { antar ->
            val (a, b) = LalKitabDashaDates.formatAntarRange(k, antar.startFrac, antar.endFrac)
            val current = lifeYear in antar.startFrac..antar.endFrac
            val tag = if (current) " *" else ""
            ctx.getString(
                R.string.pdf_antar_line,
                PlanetNames.localizedName(ctx, antar.antarPlanet),
                PlanetNames.localizedName(ctx, antar.mahadashaPlanet),
                a,
                b,
                tag
            )
        }
        drawLines(c, lines, MARGIN_H, y, bodyPaint, contentWidth())
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun addRajaVazirDhokaPage(
        doc: PdfDocument,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        titlePaint: Paint,
        bodyPaint: Paint
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k)
        val janmaChart = runCatching { KundliEngine.calculate(k) }.getOrNull()
        val chandraChart = runCatching { KundliEngine.calculateChandraKundli(k) }.getOrNull()
        val samay = resolveSamayGraha(k)
        val segJ = LalKitabDashaCalculator.compute(k, samay, useChandraChartForHouse = false)
            ?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }
        val segC = LalKitabDashaCalculator.compute(k, samay, useChandraChartForHouse = true)
            ?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }

        val page = startPage(doc)
        val c = page.canvas
        var y = drawBrandingHeader(c, ctx, subscribed, profile, bodyPaint) + 12f
        y = drawSectionTitle(c, ctx.getString(R.string.raja_vajir_title), MARGIN_H, y, titlePaint)
        val lines = mutableListOf<String>()
        lines.add(yearInfoLine(ctx, k, lifeYear))
        lines.add("")
        lines.add(ctx.getString(R.string.pdf_janma_column))
        lines.add(ctx.getString(R.string.mukhya_section_raja) + ": " + rajaLine(ctx, segJ, janmaChart, lifeYear))
        lines.add(ctx.getString(R.string.mukhya_section_vazir) + ": " + vazirLine(ctx, k, lifeYear, false, janmaChart))
        lines.add(ctx.getString(R.string.mukhya_section_dhoka) + ": " + dhokaLine(ctx, janmaChart, lifeYear))
        lines.add("")
        lines.add(ctx.getString(R.string.pdf_chandra_column))
        lines.add(ctx.getString(R.string.mukhya_section_raja) + ": " + rajaLine(ctx, segC, chandraChart, lifeYear))
        lines.add(ctx.getString(R.string.mukhya_section_vazir) + ": " + vazirLine(ctx, k, lifeYear, true, chandraChart))
        lines.add(ctx.getString(R.string.mukhya_section_dhoka) + ": " + dhokaLine(ctx, chandraChart, lifeYear))
        drawLines(c, lines, MARGIN_H, y, bodyPaint, contentWidth())
        drawFooter(c, ctx, bodyPaint)
        doc.finishPage(page)
    }

    private fun resolveSamayGraha(k: KundliData): String {
        val horaKey = runCatching { BirthPlanetsHelper.vaarAndHoraPlanetKeys(k).second }
            .getOrDefault(LalKitabDashaCalculator.MOON_NAME)
        val janma = runCatching { KundliEngine.calculate(k) }.getOrNull()
        return if (janma?.grahas?.any { it.name == horaKey } == true) {
            horaKey
        } else {
            KundliHolder.dashaSamayGrahaName.ifBlank { LalKitabDashaCalculator.MOON_NAME }
        }
    }

    private fun yearInfoLine(ctx: Context, k: KundliData, lifeYear: Int): String {
        val fmt = DateTimeFormatter.ofPattern("d/M/yyyy")
        val safeDob = runCatching {
            LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        }.getOrElse { LocalDateTime.now() }
        val start = safeDob.plusYears((lifeYear - 1).toLong())
        val end = safeDob.plusYears(lifeYear.toLong())
        val range = ctx.getString(R.string.mukhya_between_dates, start.format(fmt), end.format(fmt))
        val currentYear = runCatching { VarshfalRajaVazirHelper.computeCurrentLifeYear(k) }.getOrDefault(1)
        val tag = if (lifeYear == currentYear) ctx.getString(R.string.mukhya_current_varshfal_tag) else ""
        return ctx.getString(R.string.mukhya_varshfal_year_info, lifeYear, range, tag)
    }

    private fun rajaLine(
        ctx: Context,
        seg: LalKitabDashaSegment?,
        chart: com.vidyarthi.lalkitab.data.KundliChart?,
        lifeYear: Int
    ): String {
        if (chart == null || seg == null) return ctx.getString(R.string.mukhya_grah_missing)
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chart.grahas, lifeYear)
        val house = varshfalGrahas.firstOrNull { it.name == seg.planetName }?.house ?: return ctx.getString(R.string.mukhya_grah_missing)
        return ctx.getString(
            R.string.mukhya_grah_khana_line,
            PlanetNames.localizedName(ctx, seg.planetName),
            house
        )
    }

    private fun vazirLine(
        ctx: Context,
        k: KundliData,
        lifeYear: Int,
        fromChandra: Boolean,
        chart: com.vidyarthi.lalkitab.data.KundliChart?
    ): String {
        val v = runCatching { VarshfalRajaVazirHelper.computeVazir(k, lifeYear, fromChandra) }.getOrNull()
            ?: return ctx.getString(R.string.mukhya_grah_missing)
        val sep = ctx.getString(R.string.mukhya_planet_sep)
        val labels = if (v.grahaKeys.isEmpty()) ctx.getString(R.string.mukhya_grah_missing)
        else v.grahaKeys.joinToString(sep) { PlanetNames.localizedName(ctx, it) }
        if (labels == ctx.getString(R.string.mukhya_grah_missing)) return labels
        val varshfalGrahas = chart?.let { KundliEngine.applyLalKitabVarshfal(ctx, it.grahas, lifeYear) } ?: emptyList()
        val house = v.grahaKeys.firstNotNullOfOrNull { key ->
            varshfalGrahas.firstOrNull { it.name == key }?.house
        } ?: v.khana
        return ctx.getString(R.string.mukhya_grah_khana_line, labels, house)
    }

    private fun dhokaLine(
        ctx: Context,
        chart: com.vidyarthi.lalkitab.data.KundliChart?,
        lifeYear: Int
    ): String {
        if (chart == null) return ctx.getString(R.string.mukhya_grah_missing)
        val yearResult = DhokaNaGrahHelper.computeForLifeYear(chart, lifeYear)
        val keys = yearResult.grahaKeys
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chart.grahas, lifeYear)
        val house = keys.firstNotNullOfOrNull { key ->
            varshfalGrahas.firstOrNull { it.name == key }?.house
        } ?: yearResult.firstYearAnchorHouse
        val sep = ctx.getString(R.string.mukhya_planet_sep)
        val labels = if (keys.isEmpty()) ctx.getString(R.string.mukhya_grah_missing)
        else keys.joinToString(sep) { PlanetNames.localizedName(ctx, it) }
        return ctx.getString(R.string.mukhya_grah_khana_line, labels, house)
    }

    private fun startPage(doc: PdfDocument): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create()
        return doc.startPage(info)
    }

    private fun drawBrandingHeader(
        c: Canvas,
        ctx: Context,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        paint: Paint
    ): Float {
        var y = HEADER_H - 48f
        val bold = Paint(paint).apply { isFakeBoldText = true; textSize = 12f }
        val normal = Paint(paint).apply { textSize = 10f }
        if (subscribed) {
            val p = profile ?: SubscriberProfileManager.Profile("", "", "")
            if (p.name.isNotBlank()) {
                c.drawText(p.name, MARGIN_H, y, bold)
                y += 14f
            }
            if (p.phone.isNotBlank()) {
                c.drawText(p.phone, MARGIN_H, y, normal)
                y += 12f
            }
            if (p.address.isNotBlank()) {
                y = drawWrapped(c, p.address, MARGIN_H, y, normal, contentWidth()) + 2f
            }
            if (p.name.isBlank() && p.phone.isBlank() && p.address.isBlank()) {
                c.drawText(ctx.getString(R.string.pdf_profile_empty_hint), MARGIN_H, y, normal)
                y += 12f
            }
        } else {
            c.drawText(ctx.getString(R.string.app_name), MARGIN_H, y, bold)
            y += 14f
            c.drawText(ctx.getString(R.string.pdf_website), MARGIN_H, y, normal)
            y += 12f
            c.drawText(ctx.getString(R.string.pdf_facebook), MARGIN_H, y, normal)
            y += 12f
        }
        c.drawLine(MARGIN_H, HEADER_H - 6f, PAGE_W - MARGIN_H, HEADER_H - 6f, Paint().apply { strokeWidth = 1f })
        return HEADER_H
    }

    private fun drawFooter(c: Canvas, ctx: Context, paint: Paint) {
        val footerPaint = Paint(paint).apply {
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val text = ctx.getString(R.string.pdf_footer_app)
        c.drawText(text, PAGE_W / 2f, PAGE_H - FOOTER_H, footerPaint)
    }

    private fun drawSectionTitle(c: Canvas, title: String, x: Float, y: Float, paint: Paint): Float {
        c.drawText(title, x, y, paint)
        return y + 20f
    }

    private fun drawLines(
        c: Canvas,
        lines: List<String>,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float
    ): Float {
        var y = startY
        for (line in lines) {
            y = if (line.isEmpty()) y + 8f else drawWrapped(c, line, x, y, paint, maxWidth) + 4f
        }
        return y
    }

    private fun drawWrapped(
        c: Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float
    ): Float {
        val words = text.split(' ')
        var line = StringBuilder()
        var y = startY
        for (word in words) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > maxWidth && line.isNotEmpty()) {
                c.drawText(line.toString(), x, y, paint)
                y += 14f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(trial)
            }
        }
        if (line.isNotEmpty()) {
            c.drawText(line.toString(), x, y, paint)
            y += 14f
        }
        return y
    }

    private fun contentWidth(): Float = PAGE_W - MARGIN_H * 2
    private fun chartWidthPx(): Int = (contentWidth() * 1.2f).toInt()
}
