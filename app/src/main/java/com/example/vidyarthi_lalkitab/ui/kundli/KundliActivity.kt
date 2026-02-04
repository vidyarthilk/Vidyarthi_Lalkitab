package com.example.vidyarthi_lalkitab.ui.kundli

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vidyarthi_lalkitab.R
import com.example.vidyarthi_lalkitab.data.KundliData
import com.example.vidyarthi_lalkitab.utils.KundliEngine

class KundliActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kundli)

        val k = intent.getSerializableExtra("kundli") as KundliData

        // ✅ CALCULATE ONLY ONCE
        val chart = KundliEngine.calculate(k)

        findViewById<TextView>(R.id.tvLagna).text =
            "લગ્ન : ${chart.lagnaRashi} ${"%.2f".format(chart.lagnaDegree)}°"

        val sb = StringBuilder()
        chart.grahas.forEach {
            sb.append(
                "${it.name} : ${it.rashi} ${"%.2f".format(it.degree)}° (ભાવ ${it.house})\n"
            )
        }

        findViewById<KundliChartView>(R.id.kundliChartView)
            .setChart(chart)

        findViewById<TextView>(R.id.tvGraha).text = sb.toString()
    }
}
