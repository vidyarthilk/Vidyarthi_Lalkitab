package com.vidyarthi.lalkitab.ui.kundli

import android.os.Bundle
import com.vidyarthi.lalkitab.BaseActivity
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliHolder

class VarshfalActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_varshfal)

        val k = KundliHolder.kundliData ?: run {
            finish()
            return
        }

        VarshfalUi.bind(findViewById(R.id.varshfalRoot), this, k)
    }
}
