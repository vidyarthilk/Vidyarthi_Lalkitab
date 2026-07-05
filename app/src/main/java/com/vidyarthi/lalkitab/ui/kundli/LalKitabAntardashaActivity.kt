package com.vidyarthi.lalkitab.ui.kundli

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
import com.google.android.material.appbar.MaterialToolbar

class LalKitabAntardashaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lalkitab_antardasha)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val segmentIndex = intent.getIntExtra(EXTRA_SEGMENT_INDEX, -1)
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

        val seg = result.segments.getOrNull(segmentIndex) ?: run {
            Toast.makeText(this, getString(R.string.lalkitab_dasha_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val planetLabel = PlanetNames.localizedName(this, seg.planetName)
        toolbar.title = getString(R.string.lalkitab_antardasha_toolbar, planetLabel)

        val antars = LalKitabDashaExpand.antardashasForSegment(seg)
        val rows = antars.map { a ->
            val (ds, de) = LalKitabDashaDates.formatAntarRange(k, a.startFrac, a.endFrac)
            AntardashaRowUi(
                antarPlanetLabel = PlanetNames.localizedName(this, a.antarPlanet),
                mahadashaPlanetLabel = PlanetNames.localizedName(this, a.mahadashaPlanet),
                dateStart = ds,
                dateEnd = de
            )
        }

        findViewById<RecyclerView>(R.id.rvAntar).apply {
            layoutManager = LinearLayoutManager(this@LalKitabAntardashaActivity)
            adapter = LalKitabAntarAdapter(rows)
        }
    }

    companion object {
        const val EXTRA_SEGMENT_INDEX = "segment_index"
    }
}
