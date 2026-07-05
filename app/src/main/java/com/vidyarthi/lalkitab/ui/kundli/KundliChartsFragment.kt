package com.vidyarthi.lalkitab.ui.kundli

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliEngine
import kotlinx.coroutines.launch

/** જન્મ અને ચંદ્ર કુંડળી એક જ સ્ક્રીન પર ઉપર–નીચે. */
class KundliChartsFragment : Fragment(R.layout.fragment_kundli_charts) {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    private var janmaChartView: KundliChartView? = null
    private var chandraChartView: KundliChartView? = null
    private var tvBudhJanma: TextView? = null
    private var tvBudhChandra: TextView? = null
    private var lastKundliHash: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        janmaChartView = view.findViewById(R.id.janmaChartView)
        chandraChartView = view.findViewById(R.id.chandraChartView)
        tvBudhJanma = view.findViewById(R.id.tvBudhSvabhavJanma)
        tvBudhChandra = view.findViewById(R.id.tvBudhSvabhavChandra)
        observeKundli()
    }

    private fun observeKundli() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVM.kundliData.collect { k ->
                    k ?: return@collect
                    val hash = k.hashCode()
                    if (hash == lastKundliHash) return@collect
                    lastKundliHash = hash
                    try {
                        val ctx = requireContext()
                        val janma = KundliEngine.calculate(k)
                        val chandra = KundliEngine.calculateChandraKundli(k)
                        janmaChartView?.setChartData(janma.grahas, janma.lagnaHouse)
                        chandraChartView?.setChartData(
                            chandra.grahas,
                            KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE
                        )
                        janmaChartView?.invalidate()
                        chandraChartView?.invalidate()
                        tvBudhJanma?.text = BudhSvabhavFormat.line(ctx, janma)
                        tvBudhChandra?.text = BudhSvabhavFormat.line(ctx, chandra)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            getString(R.string.toast_error_prefix, e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        janmaChartView = null
        chandraChartView = null
        tvBudhJanma = null
        tvBudhChandra = null
        super.onDestroyView()
    }
}
