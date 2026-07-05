package com.vidyarthi.lalkitab.ui.kundli

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.LalKitabDashaDates
import com.vidyarthi.lalkitab.utils.LalKitabDashaExpand
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.VarshfalRajaVazirHelper

class LalKitabDashaFlowFragment : Fragment(R.layout.fragment_lalkitab_dasha) {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: run {
            Toast.makeText(requireContext(), getString(R.string.toast_kundli_not_available), Toast.LENGTH_SHORT).show()
            return
        }

        val result = LalKitabDashaCalculator.compute(k) ?: run {
            Toast.makeText(requireContext(), getString(R.string.lalkitab_dasha_error), Toast.LENGTH_SHORT).show()
            return
        }

        val currentLifeYear = runCatching { VarshfalRajaVazirHelper.computeCurrentLifeYear(k) }.getOrDefault(1)
        val cards = result.segments.mapIndexed { idx, seg ->
            val (d0, d1) = LalKitabDashaDates.formatSegmentRange(k, seg)
            MahadashaCardUi(
                segmentIndex = idx,
                planetLabel = PlanetNames.localizedName(requireContext(), seg.planetName),
                startLifeYear = seg.startYear,
                dateStart = d0,
                dateEnd = d1,
                isCurrent = currentLifeYear in seg.startYear..seg.endYear
            )
        }

        view.findViewById<RecyclerView>(R.id.rvDasha).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = LalKitabDashaAdapter(cards) { segmentIndex ->
                val seg = result.segments[segmentIndex]
                if (LalKitabDashaExpand.antardashasForSegment(seg).isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.lalkitab_dasha_no_antar),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@LalKitabDashaAdapter
                }
                startActivity(
                    Intent(requireContext(), LalKitabAntardashaActivity::class.java)
                        .putExtra(LalKitabAntardashaActivity.EXTRA_SEGMENT_INDEX, segmentIndex)
                )
            }
        }
    }
}
