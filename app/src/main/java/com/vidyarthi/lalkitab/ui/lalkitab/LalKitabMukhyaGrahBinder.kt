package com.vidyarthi.lalkitab.ui.lalkitab

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment
import com.vidyarthi.lalkitab.utils.BirthPlanetsHelper
import com.vidyarthi.lalkitab.utils.DhokaNaGrahHelper
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.VarshfalRajaVazirHelper
import com.vidyarthi.lalkitab.utils.YearSpinnerUi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LalKitabMukhyaGrahBinder {

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

    fun bind(root: View, k: KundliData, owner: LifecycleOwner, ctx: Context) {
        val tvYearInfo = root.findViewById<TextView>(R.id.tvVarshfalYearInfo)
        val tvHeroRajaJanma = root.findViewById<TextView>(R.id.tvHeroRaja)
        val tvHeroVazirJanma = root.findViewById<TextView>(R.id.tvHeroVazir)
        val tvHeroDhokaJanma = root.findViewById<TextView>(R.id.tvHeroDhoka)
        val tvHeroRajaChandra = root.findViewById<TextView>(R.id.tvHeroRajaChandra)
        val tvHeroVazirChandra = root.findViewById<TextView>(R.id.tvHeroVazirChandra)
        val tvHeroDhokaChandra = root.findViewById<TextView>(R.id.tvHeroDhokaChandra)
        val spinner = root.findViewById<Spinner>(R.id.spinnerVarshfalYear)

        val janmaChart = runCatching { KundliEngine.calculate(k) }.getOrNull()
        val chandraChart = runCatching { KundliEngine.calculateChandraKundli(k) }.getOrNull()

        val horaKey = runCatching { BirthPlanetsHelper.vaarAndHoraPlanetKeys(k).second }
            .getOrDefault(LalKitabDashaCalculator.MOON_NAME)

        val samayGrahaForDasha = if (janmaChart?.grahas?.any { it.name == horaKey } == true) {
            horaKey
        } else {
            LalKitabDashaCalculator.MOON_NAME
        }
        KundliHolder.dashaSamayGrahaName = samayGrahaForDasha

        YearSpinnerUi.applyCompactDropdownStyle(spinner, ctx)
        spinner.adapter = YearSpinnerUi.createYearAdapter(ctx, maxYear = 120)
        val defaultYear = runCatching { VarshfalRajaVazirHelper.computeCurrentLifeYear(k) }.getOrDefault(1)
        spinner.setSelection((defaultYear - 1).coerceIn(0, 119), false)

        fun refresh() {
            val lifeYear = spinner.selectedItemPosition + 1
            setYearInfoText(tvYearInfo, ctx, k, lifeYear)

            val segJ = runCatching {
                LalKitabDashaCalculator.compute(
                    k = k,
                    samayGrahaName = samayGrahaForDasha,
                    useChandraChartForHouse = false
                )?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }
            }.getOrNull()
            val segC = runCatching {
                LalKitabDashaCalculator.compute(
                    k = k,
                    samayGrahaName = samayGrahaForDasha,
                    useChandraChartForHouse = true
                )?.segments?.firstOrNull { lifeYear in it.startYear..it.endYear }
            }.getOrNull()

            setHeroPlanetText(
                tvHeroRajaJanma,
                formatHeroLine(ctx, formatRajaKhanaLine(ctx, segJ, janmaChart, lifeYear))
            )
            setHeroPlanetText(
                tvHeroRajaChandra,
                formatHeroLine(ctx, formatRajaKhanaLine(ctx, segC, chandraChart, lifeYear))
            )

            val vj = runCatching { VarshfalRajaVazirHelper.computeVazir(k, lifeYear, fromChandra = false) }.getOrNull()
            val vc = runCatching { VarshfalRajaVazirHelper.computeVazir(k, lifeYear, fromChandra = true) }.getOrNull()
            setHeroPlanetText(
                tvHeroVazirJanma,
                formatHeroLine(ctx, formatVazirKhanaLine(ctx, vj, janmaChart, lifeYear))
            )
            setHeroPlanetText(
                tvHeroVazirChandra,
                formatHeroLine(ctx, formatVazirKhanaLine(ctx, vc, chandraChart, lifeYear))
            )

            setHeroPlanetText(
                tvHeroDhokaJanma,
                formatHeroLine(ctx, formatDhokaKhanaLine(ctx, janmaChart, lifeYear))
            )
            setHeroPlanetText(
                tvHeroDhokaChandra,
                formatHeroLine(ctx, formatDhokaKhanaLine(ctx, chandraChart, lifeYear))
            )
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refresh()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        refresh()
    }

    private fun buildYearInfoLine(ctx: Context, k: KundliData, lifeYear: Int): String {
        val safeDob = runCatching {
            LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        }.getOrElse {
            LocalDate.now().atStartOfDay()
        }
        val start = safeDob.plusYears((lifeYear - 1).toLong())
        val end = safeDob.plusYears(lifeYear.toLong())
        val range = ctx.getString(R.string.mukhya_between_dates, start.format(dateFmt), end.format(dateFmt))
        val currentYear = runCatching { VarshfalRajaVazirHelper.computeCurrentLifeYear(k) }.getOrDefault(1)
        val currentTag = if (lifeYear == currentYear) {
            ctx.getString(R.string.mukhya_current_varshfal_tag)
        } else {
            ""
        }
        return ctx.getString(R.string.mukhya_varshfal_year_info, lifeYear, range, currentTag)
    }

    private fun setYearInfoText(textView: TextView, ctx: Context, k: KundliData, lifeYear: Int) {
        val line = buildYearInfoLine(ctx, k, lifeYear)
        val yearToken = lifeYear.toString()
        val spannable = SpannableString(line)
        val start = line.indexOf(yearToken)
        if (start >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                start + yearToken.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spannable
    }

    private fun formatHeroLine(ctx: Context, line: String): String {
        if (line == ctx.getString(R.string.mukhya_grah_missing)) return line
        val parts = line.split(" - ", limit = 2)
        if (parts.size == 2) {
            return "${parts[0].trim()}\n${parts[1].trim()}"
        }
        return line
    }

    private fun setHeroPlanetText(textView: TextView, line: String) {
        val newline = line.indexOf('\n')
        val spannable = SpannableString(line)
        val bold = StyleSpan(Typeface.BOLD)
        val grahaSize = RelativeSizeSpan(1.14f)
        val khanaSize = RelativeSizeSpan(1.22f)
        if (newline >= 0) {
            spannable.setSpan(bold, 0, newline, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(grahaSize, 0, newline, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(bold, newline + 1, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(khanaSize, newline + 1, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            spannable.setSpan(bold, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(grahaSize, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        textView.text = spannable
    }

    private fun formatRajaKhanaLine(ctx: Context, seg: LalKitabDashaSegment?, chart: KundliChart?, lifeYear: Int): String {
        if (chart == null) return ctx.getString(R.string.mukhya_grah_missing)
        val key = seg?.planetName ?: return ctx.getString(R.string.mukhya_grah_missing)
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chart.grahas, lifeYear)
        val house = varshfalGrahas.firstOrNull { it.name == key }?.house?.takeIf { it in 1..12 }
            ?: return ctx.getString(R.string.mukhya_grah_missing)
        val label = PlanetNames.localizedName(ctx, key)
        return ctx.getString(R.string.mukhya_grah_khana_line, label, house)
    }

    private fun formatVazirKhanaLine(
        ctx: Context,
        v: VarshfalRajaVazirHelper.VazirResult?,
        chart: KundliChart?,
        lifeYear: Int
    ): String {
        if (v == null) return ctx.getString(R.string.mukhya_grah_missing)
        val sep = ctx.getString(R.string.mukhya_planet_sep)
        val labels = if (v.grahaKeys.isEmpty()) {
            ctx.getString(R.string.mukhya_grah_missing)
        } else {
            v.grahaKeys.joinToString(sep) { PlanetNames.localizedName(ctx, it) }
        }
        if (labels == ctx.getString(R.string.mukhya_grah_missing)) return labels
        val varshfalGrahas = chart?.let { KundliEngine.applyLalKitabVarshfal(ctx, it.grahas, lifeYear) } ?: emptyList()
        val house = v.grahaKeys.firstNotNullOfOrNull { key ->
            varshfalGrahas.firstOrNull { it.name == key }?.house?.takeIf { h -> h in 1..12 }
        } ?: v.khana.coerceIn(1, 12)
        return ctx.getString(R.string.mukhya_grah_khana_line, labels, house)
    }

    private fun formatDhokaKhanaLine(ctx: Context, chart: KundliChart?, lifeYear: Int): String {
        if (chart == null) return ctx.getString(R.string.mukhya_grah_missing)
        val yearResult = DhokaNaGrahHelper.computeForLifeYear(chart, lifeYear)
        val keys = yearResult.grahaKeys
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chart.grahas, lifeYear)
        val actualHouse = keys.firstNotNullOfOrNull { key ->
            varshfalGrahas.firstOrNull { it.name == key }?.house?.takeIf { it in 1..12 }
        }
        val h = (actualHouse ?: yearResult.firstYearAnchorHouse).coerceIn(1, 12)
        val sep = ctx.getString(R.string.mukhya_planet_sep)
        val labels = if (keys.isEmpty()) {
            ctx.getString(R.string.mukhya_grah_missing)
        } else {
            keys.joinToString(sep) { PlanetNames.localizedName(ctx, it) }
        }
        if (labels == ctx.getString(R.string.mukhya_grah_missing)) return labels
        return ctx.getString(R.string.mukhya_grah_khana_line, labels, h)
    }
}
