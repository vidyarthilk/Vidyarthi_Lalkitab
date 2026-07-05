package com.vidyarthi.lalkitab.ui.lalkitab



import android.os.Bundle

import android.widget.Toast

import com.vidyarthi.lalkitab.BaseActivity

import com.vidyarthi.lalkitab.R

import com.vidyarthi.lalkitab.utils.KundliHolder

import com.google.android.material.appbar.MaterialToolbar



/**

 * Three cards: birth vaar/hora planets; janma chart (Raja, Vazir, Dhoka h12);

 * chandra chart (same). Matches Lal Kitab mukhya grah reference UI.

 */

class LalKitabMukhyaGrahActivity : BaseActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lalkitab_mukhya_grah)



        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }



        val k = KundliHolder.kundliData ?: run {

            Toast.makeText(this, getString(R.string.toast_kundli_not_available), Toast.LENGTH_SHORT).show()

            finish()

            return

        }



        LalKitabMukhyaGrahBinder.bind(findViewById(R.id.mukhyaNestedScroll), k, this, this)

    }

}

