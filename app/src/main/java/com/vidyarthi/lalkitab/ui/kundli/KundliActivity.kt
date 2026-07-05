package com.vidyarthi.lalkitab.ui.kundli

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import com.vidyarthi.lalkitab.BaseActivity
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.RashiNames

class KundliActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kundli)

        val k: KundliData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("kundli", KundliData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("kundli") as? KundliData
        }

        if (k == null) {
            finish()
            return
        }

        val janma = KundliEngine.calculate(k)
        val chandra = KundliEngine.calculateChandraKundli(k)

        findViewById<TextView>(R.id.tvLagna).text = getString(
            R.string.lagna_format,
            RashiNames.localizedName(this, janma.lagnaRashi),
            "%.2f".format(janma.lagnaDegree)
        )

        val sb = StringBuilder()
        janma.grahas.forEach {
            sb.append(
                getString(
                    R.string.graha_line_format,
                    PlanetNames.localizedName(this, it.name),
                    RashiNames.localizedName(this, it.rashi),
                    "%.2f".format(it.degree),
                    it.house
                )
            )
        }

        findViewById<KundliChartView>(R.id.janmaChartView).setChartData(janma.grahas, janma.lagnaHouse)
        findViewById<KundliChartView>(R.id.chandraChartView).setChartData(
            chandra.grahas,
            KundliChartView.LAGNA_RASHI_DISPLAY_FIXED_ONE
        )

        findViewById<TextView>(R.id.tvBudhSvabhavJanma).text = BudhSvabhavFormat.line(this, janma)
        findViewById<TextView>(R.id.tvBudhSvabhavChandra).text = BudhSvabhavFormat.line(this, chandra)

        findViewById<TextView>(R.id.tvGraha).text = sb.toString()
    }
}
