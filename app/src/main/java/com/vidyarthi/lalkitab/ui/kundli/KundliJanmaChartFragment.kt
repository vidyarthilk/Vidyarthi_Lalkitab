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

class KundliJanmaChartFragment : Fragment(R.layout.fragment_kundli_janma_only) {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    private var janmaChartView: KundliChartView? = null
    private var tvBudhJanma: TextView? = null
    private var lastKundliHash: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        janmaChartView = view.findViewById(R.id.janmaChartView)
        tvBudhJanma = view.findViewById(R.id.tvBudhSvabhavJanma)
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
                        janmaChartView?.setChartData(janma.grahas, janma.lagnaHouse)
                        janmaChartView?.invalidate()
                        tvBudhJanma?.text = BudhSvabhavFormat.line(ctx, janma)
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
        tvBudhJanma = null
        super.onDestroyView()
    }
}
