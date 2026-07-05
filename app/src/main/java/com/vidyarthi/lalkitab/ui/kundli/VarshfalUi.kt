package com.vidyarthi.lalkitab.ui.kundli

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.VarshfalTable
import com.vidyarthi.lalkitab.utils.YearSpinnerUi
import java.time.LocalDateTime

object VarshfalUi {

    fun bind(root: View, context: Context, k: KundliData) {
        DualKundliTabHelper.setup(root)
        VarshfalTable.load(context)
        val janmaChart = KundliEngine.calculate(k)
        val chandraBase = KundliEngine.calculateChandraKundli(k).grahas
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)

        val yearSpinner = root.findViewById<Spinner>(R.id.yearSpinner)
        val tvYearInfo = root.findViewById<TextView>(R.id.tvYearInfo)

        YearSpinnerUi.applyCompactDropdownStyle(yearSpinner, context)
        yearSpinner.adapter = YearSpinnerUi.createYearAdapter(context, maxYear = 100)

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedYear = position + 1
                val janmaVarshfal = KundliEngine.applyLalKitabVarshfal(
                    context,
                    janmaChart.grahas,
                    selectedYear
                )
                root.findViewById<KundliChartView>(R.id.janmaChart)
                    .setChartData(janmaVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE)
                root.findViewById<TextView>(R.id.textBudhJanma)?.text =
                    BudhSvabhavFormat.line(context, janmaVarshfal)

                val chandraVarshfal = KundliEngine.applyLalKitabVarshfal(
                    context,
                    chandraBase,
                    selectedYear
                )
                root.findViewById<KundliChartView>(R.id.chandraChart)
                    .setChartData(chandraVarshfal, KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE)
                root.findViewById<TextView>(R.id.textBudhChandra)?.text =
                    BudhSvabhavFormat.line(context, chandraVarshfal)

                val (start, end) = KundliEngine.getVarshfalDateRange(dob, selectedYear)
                tvYearInfo.text = "(${KundliEngine.getFormattedRange(start, end)})"
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        yearSpinner.setSelection(KundliEngine.getCurrentVarshfalYear(dob) - 1)
    }
}
