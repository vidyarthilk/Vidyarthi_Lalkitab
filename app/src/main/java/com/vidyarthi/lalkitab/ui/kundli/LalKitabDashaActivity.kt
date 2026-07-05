package com.vidyarthi.lalkitab.ui.kundli

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyarthi.lalkitab.BaseActivity
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.LalKitabDashaDates
import com.vidyarthi.lalkitab.utils.LalKitabDashaExpand
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.VarshfalRajaVazirHelper
import com.google.android.material.appbar.MaterialToolbar

class LalKitabDashaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lalkitab_dasha)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val k = KundliHolder.kundliData ?: run {
            Toast.makeText(this, getString(R.string.toast_kundli_not_available), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val result = LalKitabDashaCalculator.compute(k) ?: run {
            Toast.makeText(this, getString(R.string.lalkitab_dasha_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val currentLifeYear = runCatching { VarshfalRajaVazirHelper.computeCurrentLifeYear(k) }.getOrDefault(1)
        val cards = result.segments.mapIndexed { idx, seg ->
            val (d0, d1) = LalKitabDashaDates.formatSegmentRange(k, seg)
            MahadashaCardUi(
                segmentIndex = idx,
                planetLabel = PlanetNames.localizedName(this, seg.planetName),
                startLifeYear = seg.startYear,
                dateStart = d0,
                dateEnd = d1,
                isCurrent = currentLifeYear in seg.startYear..seg.endYear
            )
        }

        findViewById<RecyclerView>(R.id.rvDasha).apply {
            layoutManager = LinearLayoutManager(this@LalKitabDashaActivity)
            adapter = LalKitabDashaAdapter(cards) { segmentIndex ->
                val seg = result.segments[segmentIndex]
                if (LalKitabDashaExpand.antardashasForSegment(seg).isEmpty()) {
                    Toast.makeText(
                        this@LalKitabDashaActivity,
                        getString(R.string.lalkitab_dasha_no_antar),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@LalKitabDashaAdapter
                }
                startActivity(
                    Intent(this@LalKitabDashaActivity, LalKitabAntardashaActivity::class.java)
                        .putExtra(LalKitabAntardashaActivity.EXTRA_SEGMENT_INDEX, segmentIndex)
                )
            }
        }
    }
}
