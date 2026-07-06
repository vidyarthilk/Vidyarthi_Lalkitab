package com.vidyarthi.lalkitab.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment
import com.vidyarthi.lalkitab.subscription.SubscriberProfileManager
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import com.vidyarthi.lalkitab.ui.kundli.BudhSvabhavFormat
import com.vidyarthi.lalkitab.ui.kundli.GrahaRashiTableHelper
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KundliReportPdfExporter {

    private data class PreparedCharts(
        val janmaChart: KundliChart,
        val chandraChart: KundliChart,
        val janma: Bitmap,
        val chandra: Bitmap,
        val janmaVarshfal: Bitmap,
        val chandraVarshfal: Bitmap
    )

    private class PageBuilder(private val doc: PdfDocument) {
        var pageNumber = 0

        fun newPage(): PdfDocument.Page {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(
                PdfReportDrawer.PAGE_W.toInt(),
                PdfReportDrawer.PAGE_H.toInt(),
                pageNumber
            ).create()
            val page = doc.startPage(info)
            PdfReportDrawer.drawPageBackground(page.canvas)
            return page
        }

        fun finish(page: PdfDocument.Page) = doc.finishPage(page)
    }

    suspend fun export(
        context: Context,
        k: KundliData,
        city: String?,
        gender: String?
    ): File = withContext(Dispatchers.IO) {
        VarshfalTable.load(context)
        val panchang = PanchangReportLoader.load(k)
        val subscribed = SubscriptionManager.isSubscribed(context)
        val profile = if (subscribed) SubscriberProfileManager.load(context) else null
        val charts = prepareCharts(context, k)

        val doc = PdfDocument()
        val pages = PageBuilder(doc)
        val footer = context.getString(R.string.pdf_footer_app)
        val logo = loadLogo(context)

        try {
            addBirthDetailsPage(doc, pages, context, k, city, gender, subscribed, profile, footer, logo)
            addPanchangPage(doc, pages, context, k, panchang, subscribed, profile, footer, logo)
            addChartPage(
                doc, pages, context, k,
                context.getString(R.string.pdf_page_janma_chart),
                charts.janma,
                charts.janmaChart.grahas,
                tableChart = charts.janmaChart,
                subscribed, profile, footer, logo
            )
            addChartPage(
                doc, pages, context, k,
                context.getString(R.string.pdf_page_chandra_chart),
                charts.chandra,
                charts.chandraChart.grahas,
                tableChart = null,
                subscribed, profile, footer, logo
            )
            addVarshfalPage(
                doc, pages, context, k, charts.janmaVarshfal, charts.chandraVarshfal,
                subscribed, profile, footer, logo
            )
            addCurrentDashaPage(doc, pages, context, k, subscribed, profile, footer, logo)
            addAntardashaPage(doc, pages, context, k, subscribed, profile, footer, logo)
            addRajaVazirDhokaPage(doc, pages, context, k, subscribed, profile, footer, logo)

            val safeName = k.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "kundli" }
            val out = File(context.cacheDir, "kundli_report_$safeName.pdf")
            FileOutputStream(out).use { doc.writeTo(it) }
            out
        } finally {
            logo?.recycle()
            charts.janma.recycle()
            charts.chandra.recycle()
            charts.janmaVarshfal.recycle()
            charts.chandraVarshfal.recycle()
            doc.close()
        }
    }

    private suspend fun prepareCharts(ctx: Context, k: KundliData): PreparedCharts =
        withContext(Dispatchers.Main) {
            val janmaChart = KundliEngine.calculate(k)
            val chandraChart = KundliEngine.calculateChandraKundli(k)
            val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
            val lifeYear = KundliEngine.getCurrentVarshfalYear(dob)
            val janmaVarshfal = KundliEngine.applyLalKitabVarshfal(ctx, janmaChart.grahas, lifeYear)
            val chandraVarshfal = KundliEngine.applyLalKitabVarshfal(ctx, chandraChart.grahas, lifeYear)
            val fullW = chartWidthPx()
            val halfW = (PdfReportDrawer.contentWidth() / 2f).toInt().coerceAtLeast(200)
            PreparedCharts(
                janmaChart = janmaChart,
                chandraChart = chandraChart,
                janma = KundliChartBitmap.render(
                    ctx, janmaChart.grahas, janmaChart.lagnaHouse, fullW
                ),
                chandra = KundliChartBitmap.render(
                    ctx, chandraChart.grahas, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE, fullW
                ),
                janmaVarshfal = KundliChartBitmap.render(
                    ctx, janmaVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE, halfW
                ),
                chandraVarshfal = KundliChartBitmap.render(
                    ctx, chandraVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE, halfW
                )
            )
        }

    private fun headerLines(
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?
    ): Triple<String, String?, String?> {
        return if (subscribed) {
            val p = profile ?: SubscriberProfileManager.Profile("", "", "")
            val line1 = p.name.ifBlank { ctx.getString(R.string.app_name) }
            val line2 = p.phone.takeIf { it.isNotBlank() }
            val line3 = p.address.takeIf { it.isNotBlank() }
                ?: if (p.name.isBlank() && p.phone.isBlank()) ctx.getString(R.string.pdf_profile_empty_hint) else null
            Triple(line1, line2, line3)
        } else {
            Triple(
                ctx.getString(R.string.app_name),
                ctx.getString(R.string.pdf_website),
                ctx.getString(R.string.pdf_facebook)
            )
        }
    }

    private fun drawPageHeader(
        c: Canvas,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        logo: Bitmap?
    ): Float {
        val (h1, h2, h3) = headerLines(ctx, k, subscribed, profile)
        return PdfReportDrawer.drawHeaderBand(c, h1, h2, h3, kundliCaption(k), logo)
    }

    private fun kundliCaption(k: KundliData): String {
        val dob = String.format("%02d/%02d/%04d", k.day, k.month, k.year)
        val tob = String.format("%02d:%02d", k.hour, k.minute)
        return "${k.name}\n$dob · $tob"
    }

    private fun loadLogo(ctx: Context): Bitmap? = runCatching {
        val d = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher) ?: return null
        val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        Canvas(bmp).also { canvas ->
            d.setBounds(0, 0, 128, 128)
            d.draw(canvas)
        }
        bmp
    }.getOrNull()

    private fun addBirthDetailsPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        city: String?,
        gender: String?,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.pdf_page_birth_details), y)

        val dob = String.format("%02d/%02d/%04d", k.day, k.month, k.year)
        val tob = String.format("%02d:%02d", k.hour, k.minute)
        val rows = buildList {
            add(ctx.getString(R.string.pdf_label_name, "") to k.name)
            add(ctx.getString(R.string.pdf_label_dob, "") to dob)
            add(ctx.getString(R.string.pdf_label_tob, "") to tob)
            if (!city.isNullOrBlank()) add(ctx.getString(R.string.pdf_label_city, "") to city)
            if (!gender.isNullOrBlank()) add(ctx.getString(R.string.pdf_label_gender, "") to gender)
        }.map { (label, value) -> label.trimEnd(':', ' ') to value }

        val cardH = 28f + rows.size * 24f
        PdfReportDrawer.drawCard(c, y, cardH)
        y = PdfReportDrawer.drawLabelValueGrid(
            c, rows, PdfReportDrawer.MARGIN + 14f, y + 22f,
            PdfReportDrawer.contentWidth() - 28f,
            PdfReportDrawer.labelPaint(),
            PdfReportDrawer.bodyPaint()
        )
        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addPanchangPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        state: PanchangUiState,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.panchang_title), y)

        val cardTop = y
        val body = PdfReportDrawer.bodyPaint()
        val w = PdfReportDrawer.contentWidth() - 28f
        val x = PdfReportDrawer.MARGIN + 14f

        data class PanchangSection(val title: String, val lines: List<String>)
        val sections = buildList {
            state.dayInfo?.let { d ->
                add(
                    PanchangSection(
                        ctx.getString(R.string.pdf_section_day),
                        buildList {
                            add(d.weekday)
                            add(ctx.getString(R.string.pdf_sunrise_sunset, d.sunrise, d.sunset))
                            d.moonrise?.let { mr ->
                                add(ctx.getString(R.string.pdf_moonrise_set, mr, d.moonset.orEmpty()))
                            }
                            add(ctx.getString(R.string.pdf_day_night, d.dayLength, d.nightLength))
                        }
                    )
                )
            }
            val lunarLines = buildList {
                state.tithiInfo?.let { t ->
                    add(ctx.getString(R.string.pdf_tithi, t.janmaTithi, t.endTime))
                }
                state.nakshatraInfo?.let { n ->
                    val name = PanchangLabelResolver.nakshatra(ctx, n.nakshatraIndex)
                    add(ctx.getString(R.string.pdf_nakshatra, name, n.pada, n.endTime))
                }
                state.pakshaInfo?.let { p ->
                    add(ctx.getString(R.string.pdf_paksha, p.paksha, p.tithi))
                }
                state.vikramSamvat?.let { vs ->
                    add(ctx.getString(R.string.pdf_vikram_samvat, vs))
                }
            }
            if (lunarLines.isNotEmpty()) {
                add(PanchangSection(ctx.getString(R.string.pdf_section_lunar), lunarLines))
            }
            val grahaLines = buildList {
                state.birthdayPlanetKey?.let { key ->
                    add(ctx.getString(R.string.panchang_birth_day_planet_line, PlanetNames.localizedName(ctx, key)))
                }
                state.samayGrahaDashaName?.let { key ->
                    val house = state.samayGrahaJanmaHouse ?: 1
                    add(ctx.getString(R.string.panchang_birth_time_planet_line, PlanetNames.localizedName(ctx, key)))
                    add(ctx.getString(R.string.pdf_samay_house, PlanetNames.localizedName(ctx, key), house))
                }
            }
            if (grahaLines.isNotEmpty()) {
                add(PanchangSection(ctx.getString(R.string.pdf_section_graha), grahaLines))
            }
        }

        var estH = 20f
        for (sec in sections) {
            estH += 20f + sec.lines.size * 17f + 8f
        }
        PdfReportDrawer.drawCard(c, cardTop, estH)

        var contentY = cardTop + 18f
        for (sec in sections) {
            contentY = PdfReportDrawer.drawSubheading(c, sec.title, x, contentY, body)
            contentY = PdfReportDrawer.drawBulletLines(c, sec.lines, x, contentY, body, w) + 10f
        }

        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addChartPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        pageTitle: String,
        bitmap: Bitmap,
        grahas: List<GrahaPosition>,
        tableChart: KundliChart?,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val budh = BudhSvabhavFormat.line(ctx, grahas)
        val tableRows = tableChart?.let { GrahaRashiTableHelper.rows(ctx, it) }
        val tableH = if (tableRows != null) 36f + tableRows.size * 16f + 18f else 0f
        val noteCardH = 36f
        val gap = 12f

        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, pageTitle, y)

        val maxW = PdfReportDrawer.contentWidth()
        var imgH = bitmap.height * (maxW / bitmap.width.toFloat())
        val maxChartBottom = PdfReportDrawer.contentBottom() - tableH - noteCardH - gap - 16f
        if (y + imgH > maxChartBottom && maxChartBottom > y + 120f) {
            imgH = maxChartBottom - y
        }
        val dest = RectF(PdfReportDrawer.MARGIN, y, PdfReportDrawer.MARGIN + maxW, y + imgH)
        PdfReportDrawer.drawChartFrame(c, dest)
        c.drawBitmap(bitmap, null, dest, null)
        y = dest.bottom + gap

        if (tableRows != null) {
            y = PdfReportDrawer.drawGrahaRashiTable(c, ctx, tableRows, y)
            y += 8f
        }

        PdfReportDrawer.drawCard(c, y, noteCardH, strokeGold = false)
        PdfReportDrawer.drawWrapped(
            c, budh.toString(), PdfReportDrawer.MARGIN + 12f, y + 14f,
            PdfReportDrawer.smallPaint(), maxW - 24f, 13f
        )
        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addVarshfalPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        janmaBmp: Bitmap,
        chandraBmp: Bitmap,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        val lifeYear = KundliEngine.getCurrentVarshfalYear(dob)
        val (start, end) = KundliEngine.getVarshfalDateRange(dob, lifeYear)
        val range = KundliEngine.getFormattedRange(start, end)

        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.pdf_page_varshfal), y)

        val infoH = 32f
        PdfReportDrawer.drawCard(c, y, infoH)
        c.drawText(
            ctx.getString(R.string.pdf_varshfal_year, lifeYear, range),
            PdfReportDrawer.MARGIN + 14f, y + 20f,
            PdfReportDrawer.bodyPaint()
        )
        y += infoH + 12f

        val gap = 10f
        val halfW = (PdfReportDrawer.contentWidth() - gap) / 2f
        val scaleJ = halfW / janmaBmp.width
        val scaleC = halfW / chandraBmp.width
        val hJ = janmaBmp.height * scaleJ
        val hC = chandraBmp.height * scaleC

        val labelPaint = PdfReportDrawer.labelPaint()
        c.drawText(ctx.getString(R.string.pdf_janma), PdfReportDrawer.MARGIN, y + 10f, labelPaint)
        c.drawText(ctx.getString(R.string.pdf_chandra), PdfReportDrawer.MARGIN + halfW + gap, y + 10f, labelPaint)
        y += 16f

        val leftDest = RectF(PdfReportDrawer.MARGIN, y, PdfReportDrawer.MARGIN + halfW, y + hJ)
        val rightDest = RectF(PdfReportDrawer.MARGIN + halfW + gap, y, PdfReportDrawer.PAGE_W - PdfReportDrawer.MARGIN, y + hC)
        PdfReportDrawer.drawChartFrame(c, leftDest)
        PdfReportDrawer.drawChartFrame(c, rightDest)
        c.drawBitmap(janmaBmp, null, leftDest, null)
        c.drawBitmap(chandraBmp, null, rightDest, null)

        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addCurrentDashaPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k)
        val samay = resolveSamayGraha(k)
        val result = LalKitabDashaCalculator.compute(k, samayGrahaName = samay) ?: return
        val seg = result.segments.firstOrNull { lifeYear in it.startYear..it.endYear } ?: return
        val (d0, d1) = LalKitabDashaDates.formatSegmentRange(k, seg)

        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.lalkitab_dasha_screen_title), y)

        val summaryH = 52f
        PdfReportDrawer.drawCard(c, y, summaryH)
        val body = PdfReportDrawer.bodyPaint()
        var cy = y + 18f
        val x = PdfReportDrawer.MARGIN + 14f
        c.drawText(ctx.getString(R.string.pdf_current_life_year, lifeYear), x, cy, body)
        cy += 16f
        c.drawText(ctx.getString(R.string.pdf_samay_graha, PlanetNames.localizedName(ctx, samay)), x, cy, body)
        y += summaryH + 10f

        val highlightH = 44f
        PdfReportDrawer.drawCard(c, y, highlightH)
        val highlightPaint = PdfReportDrawer.bodyPaint().apply {
            isFakeBoldText = true
            textSize = 11f
        }
        c.drawText(
            ctx.getString(
                R.string.pdf_mahadasha_line,
                PlanetNames.localizedName(ctx, seg.planetName),
                seg.startYear,
                seg.endYear,
                d0,
                d1
            ),
            x, y + 26f, highlightPaint
        )

        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addAntardashaPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k).toDouble()
        val samay = resolveSamayGraha(k)
        val result = LalKitabDashaCalculator.compute(k, samayGrahaName = samay) ?: return
        val seg = result.segments.firstOrNull { lifeYear.toInt() in it.startYear..it.endYear } ?: return
        val antars = LalKitabDashaExpand.antardashasForSegment(seg)
        if (antars.isEmpty()) return

        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.pdf_page_antardasha), y)

        val colW = listOf(
            PdfReportDrawer.contentWidth() * 0.28f,
            PdfReportDrawer.contentWidth() * 0.22f,
            PdfReportDrawer.contentWidth() * 0.28f,
            PdfReportDrawer.contentWidth() * 0.22f
        )
        val x = PdfReportDrawer.MARGIN
        y = PdfReportDrawer.drawTableHeader(
            c,
            listOf(
                ctx.getString(R.string.pdf_col_antar),
                ctx.getString(R.string.pdf_col_maha),
                ctx.getString(R.string.pdf_col_from),
                ctx.getString(R.string.pdf_col_to)
            ),
            x, y + 16f, colW
        )

        val rowPaint = PdfReportDrawer.smallPaint()
        var alt = false
        for (antar in antars) {
            val (a, b) = LalKitabDashaDates.formatAntarRange(k, antar.startFrac, antar.endFrac)
            val current = lifeYear in antar.startFrac..antar.endFrac
            val tag = if (current) " *" else ""
            y = PdfReportDrawer.drawTableRow(
                c,
                listOf(
                    PlanetNames.localizedName(ctx, antar.antarPlanet) + tag,
                    PlanetNames.localizedName(ctx, antar.mahadashaPlanet),
                    a,
                    b
                ),
                x, y, colW, rowPaint, alt
            )
            alt = !alt
            if (y > PdfReportDrawer.contentBottom()) break
        }

        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
    }

    private fun addRajaVazirDhokaPage(
        doc: PdfDocument,
        pages: PageBuilder,
        ctx: Context,
        k: KundliData,
        subscribed: Boolean,
        profile: SubscriberProfileManager.Profile?,
        footer: String,
        logo: Bitmap?
    ) {
        val lifeYear = VarshfalRajaVazirHelper.computeCurrentLifeYear(k)
        val janmaChart = runCatching { KundliEngine.calculate(k) }.getOrNull()
        val chandraChart = runCatching { KundliEngine.calculateChandraKundli(k) }.getOrNull()
        val samay = resolveSamayGraha(k)
        val segJ = LalKitabDashaCalculator.compute(k, samay, useChandraChartForHouse = false)
            ?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }
        val segC = LalKitabDashaCalculator.compute(k, samay, useChandraChartForHouse = true)
            ?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }

        val page = pages.newPage()
        val c = page.canvas
        var y = drawPageHeader(c, ctx, k, subscribed, profile, logo)
        y = PdfReportDrawer.drawSectionHeader(c, ctx.getString(R.string.raja_vajir_title), y)

        val infoH = 28f
        PdfReportDrawer.drawCard(c, y, infoH)
        c.drawText(yearInfoLine(ctx, k, lifeYear), PdfReportDrawer.MARGIN + 14f, y + 18f, PdfReportDrawer.bodyPaint())
        y += infoH + 12f

        val janmaLines = listOf(
            ctx.getString(R.string.mukhya_section_raja) + ": " + rajaLine(ctx, segJ, janmaChart, lifeYear),
            ctx.getString(R.string.mukhya_section_vazir) + ": " + vazirLine(ctx, k, lifeYear, false, janmaChart),
            ctx.getString(R.string.mukhya_section_dhoka) + ": " + dhokaLine(ctx, janmaChart, lifeYear)
        )
        val chandraLines = listOf(
            ctx.getString(R.string.mukhya_section_raja) + ": " + rajaLine(ctx, segC, chandraChart, lifeYear),
            ctx.getString(R.string.mukhya_section_vazir) + ": " + vazirLine(ctx, k, lifeYear, true, chandraChart),
            ctx.getString(R.string.mukhya_section_dhoka) + ": " + dhokaLine(ctx, chandraChart, lifeYear)
        )
        PdfReportDrawer.drawTwoColumnPanels(
            c,
            ctx.getString(R.string.pdf_janma_column),
            janmaLines,
            ctx.getString(R.string.pdf_chandra_column),
            chandraLines,
            y,
            PdfReportDrawer.bodyPaint()
        )

        PdfReportDrawer.drawFooter(c, footer, pages.pageNumber)
        pages.finish(page)
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
        val house = varshfalGrahas.firstOrNull { it.name == seg.planetName }?.house
            ?: return ctx.getString(R.string.mukhya_grah_missing)
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

    private fun chartWidthPx(): Int = (PdfReportDrawer.contentWidth() * 1.15f).toInt()
}
