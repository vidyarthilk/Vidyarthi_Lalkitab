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

class MaasKundliFragment : Fragment() {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_maas_kundli, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DualKundliTabHelper.setup(view)
        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: return
        val ctx = requireContext()
        val tvPeriodRange = view.findViewById<TextView>(R.id.tvPeriodRange)
        val dob = LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)
        val janmaChart = KundliEngine.calculate(k)
        val chandraBase = KundliEngine.calculateChandraKundli(k).grahas
        val varshfalYear = KundliEngine.getVarshfalYear(dob)
        val varshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, janmaChart.grahas, varshfalYear)
        val chandraVarshfalGrahas = KundliEngine.applyLalKitabVarshfal(ctx, chandraBase, varshfalYear)
        val lagna = KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE

        val monthSpinner = view.findViewById<Spinner>(R.id.monthSpinner)
        YearSpinnerUi.applyCompactDropdownStyle(monthSpinner, ctx)
        monthSpinner.adapter = YearSpinnerUi.createAdapter(ctx, (1..12).map { "$it" })

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                val month = position + 1
                val (maasGrahas, _) = KundliEngine.applyMonthlySunShift(varshfalGrahas, lagna, month)
                view.findViewById<KundliChartView>(R.id.janmaChart).setChartData(maasGrahas, lagna)
                view.findViewById<TextView>(R.id.textBudhJanma).text =
                    BudhSvabhavFormat.line(ctx, maasGrahas)
                val (chandraMaas, _) = KundliEngine.applyMonthlySunShift(chandraVarshfalGrahas, lagna, month)
                view.findViewById<KundliChartView>(R.id.chandraChart).setChartData(chandraMaas, lagna)
                view.findViewById<TextView>(R.id.textBudhChandra).text =
                    BudhSvabhavFormat.line(ctx, chandraMaas)
                val (startM, endM) = KundliEngine.getVarshfalMonthRange(dob, month)
                tvPeriodRange.text = "(${KundliEngine.getFormattedRange(startM, endM)})"
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        monthSpinner.setSelection(KundliEngine.getVarshfalMonth(dob) - 1)
    }
}
