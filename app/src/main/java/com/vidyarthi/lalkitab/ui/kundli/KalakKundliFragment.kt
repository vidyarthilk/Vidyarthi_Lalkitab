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

class KalakKundliFragment : Fragment() {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_kalak_kundli, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DualKundliTabHelper.setup(view)
        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: return
        val ctx = requireContext()
        val tvPeriodRange = view.findViewById<TextView>(R.id.tvPeriodRange)
        val hourSpinner = view.findViewById<Spinner>(R.id.hourSpinner)
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        val now = LocalDateTime.now()
        val lagna = KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE

        val currentDayDivas = KundliEngine.getVarshfalDayIndexInCurrentMonth(dob, now)
        val currentHourIndex = KundliEngine.getBirthAlignedHourIndex(dob, now)

        val janmaBase = KundliEngine.calculate(k)
        val chandraBase = KundliEngine.calculateChandraKundli(k).grahas
        val varshAge = now.year - dob.year
        val varshfalYear = if (now.isBefore(dob.withYear(now.year))) varshAge else varshAge + 1
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, janmaBase.grahas, varshfalYear)
        val chandraVarshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chandraBase, varshfalYear)
        val currentMonth = KundliEngine.getVarshfalMonth(dob)

        val (janmaMaas, _) = KundliEngine.applyMonthlySunShift(varshfalGrahas, lagna, currentMonth)
        val (janmaDivas, _) = KundliEngine.applyDivasKundliShift(janmaMaas, lagna, currentDayDivas)
        val (chandraMaas, _) = KundliEngine.applyMonthlySunShift(chandraVarshfalGrahas, lagna, currentMonth)
        val (chandraDivas, _) = KundliEngine.applyDivasKundliShift(chandraMaas, lagna, currentDayDivas)

        YearSpinnerUi.applyCompactDropdownStyle(hourSpinner, ctx)
        hourSpinner.adapter = YearSpinnerUi.createAdapter(ctx, (1..24).map { "$it" })

        hourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val (janmaKalak, _) = KundliEngine.applyKalakKundliShift(janmaDivas, lagna, position)
                view.findViewById<KundliChartView>(R.id.janmaChart).setChartData(janmaKalak, lagna)
                view.findViewById<TextView>(R.id.textBudhJanma).text =
                    BudhSvabhavFormat.line(ctx, janmaKalak)
                val (chandraKalak, _) = KundliEngine.applyKalakKundliShift(chandraDivas, lagna, position)
                view.findViewById<KundliChartView>(R.id.chandraChart).setChartData(chandraKalak, lagna)
                view.findViewById<TextView>(R.id.textBudhChandra).text =
                    BudhSvabhavFormat.line(ctx, chandraKalak)
                tvPeriodRange.text = KundliEngine.formatKalakSlotRangeLabel(dob, position, now)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        hourSpinner.setSelection(currentHourIndex)
        tvPeriodRange.text = KundliEngine.formatKalakSlotRangeLabel(dob, currentHourIndex, now)
    }
}
