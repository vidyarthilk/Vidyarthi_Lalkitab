package com.vidyarthi.lalkitab.ui.kundli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.YearSpinnerUi
import java.time.LocalDateTime

class DivasKundliFragment : Fragment() {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_divas_kundli, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DualKundliTabHelper.setup(view)
        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: return
        val ctx = requireContext()
        val tvPeriodRange = view.findViewById<TextView>(R.id.tvPeriodRange)
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        val now = LocalDateTime.now()
        var age = now.year - dob.year
        if (now.isBefore(dob.withYear(now.year))) {
            age--
        }
        val varshfalAge = age + 1
        val lagna = KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE

        val janmaBase = KundliEngine.calculate(k)
        val chandraBase = KundliEngine.calculateChandraKundli(k).grahas
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, janmaBase.grahas, varshfalAge)
        val chandraVarshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chandraBase, varshfalAge)
        val currentMonth = KundliEngine.getVarshfalMonth(dob)

        val daySpinner = view.findViewById<Spinner>(R.id.daySpinner)
        YearSpinnerUi.applyCompactDropdownStyle(daySpinner, ctx)
        val varshStart = dob.plusYears(age.toLong())
        val monthStartDate = varshStart.plusMonths((currentMonth - 1).toLong())
        val daysInMonth = monthStartDate.toLocalDate().lengthOfMonth()
        daySpinner.adapter = YearSpinnerUi.createAdapter(ctx, (1..daysInMonth).map { "$it" })

        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selectedDay = position + 1
                val (janmaMaas, _) = KundliEngine.applyMonthlySunShift(
                    varshfalGrahas,
                    lagna,
                    currentMonth
                )
                val (janmaDivas, _) = KundliEngine.applyDivasKundliShift(
                    janmaMaas,
                    lagna,
                    selectedDay
                )
                view.findViewById<KundliChartView>(R.id.janmaChart).setChartData(janmaDivas, lagna)
                view.findViewById<TextView>(R.id.textBudhJanma).text =
                    BudhSvabhavFormat.line(ctx, janmaDivas)
                val (chandraMaas, _) = KundliEngine.applyMonthlySunShift(
                    chandraVarshfalGrahas,
                    lagna,
                    currentMonth
                )
                val (chandraDivas, _) = KundliEngine.applyDivasKundliShift(
                    chandraMaas,
                    lagna,
                    selectedDay
                )
                view.findViewById<KundliChartView>(R.id.chandraChart).setChartData(chandraDivas, lagna)
                view.findViewById<TextView>(R.id.textBudhChandra).text =
                    BudhSvabhavFormat.line(ctx, chandraDivas)
                val (startD, endD) = KundliEngine.getDivasDateRangeFromMonth(dob, currentMonth, selectedDay)
                tvPeriodRange.text = "(${KundliEngine.getFormattedRange(startD, endD)})"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val currentDayIndex = KundliEngine.getVarshfalDayIndexInCurrentMonth(dob, now)
            .coerceIn(1, daysInMonth)
        daySpinner.setSelection(currentDayIndex - 1)
    }
}
