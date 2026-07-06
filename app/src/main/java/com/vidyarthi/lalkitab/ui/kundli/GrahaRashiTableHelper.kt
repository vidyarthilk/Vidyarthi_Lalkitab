package com.vidyarthi.lalkitab.ui.kundli

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.RashiNames

object GrahaRashiTableHelper {

    data class Row(val graha: String, val rashi: String, val degree: String)

    fun rows(ctx: Context, chart: KundliChart): List<Row> {
        val lagna = Row(
            graha = ctx.getString(R.string.graha_table_lagna),
            rashi = RashiNames.localizedName(ctx, chart.lagnaRashi),
            degree = formatDegree(chart.lagnaDegree)
        )
        val grahas = chart.grahas.map { g ->
            Row(
                graha = PlanetNames.localizedName(ctx, g.name),
                rashi = RashiNames.localizedName(ctx, g.rashi),
                degree = formatDegree(g.degree)
            )
        }
        return listOf(lagna) + grahas
    }

    fun formatDegree(degree: Double): String = "%.2f°".format(degree)

    fun bind(container: LinearLayout, ctx: Context, chart: KundliChart) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.item_graha_rashi_table_header, container, true)
        val tableRows = rows(ctx, chart)
        tableRows.forEachIndexed { index, row ->
            val rowView = inflater.inflate(R.layout.item_graha_rashi_table_row, container, false)
            rowView.findViewById<TextView>(R.id.tvGraha).text = row.graha
            rowView.findViewById<TextView>(R.id.tvRashi).text = row.rashi
            rowView.findViewById<TextView>(R.id.tvDegree).text = row.degree
            if (index % 2 == 1) {
                rowView.setBackgroundColor(ctx.getColor(R.color.dasha_highlight_fill))
            }
            container.addView(rowView)
            if (index < tableRows.lastIndex) {
                container.addView(divider(ctx))
            }
        }
    }

    private fun divider(ctx: Context): View {
        val h = (ctx.resources.displayMetrics.density * 1f).toInt().coerceAtLeast(1)
        return View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                h
            )
            setBackgroundColor(ctx.getColor(R.color.divider_warm))
            alpha = 0.35f
        }
    }
}
