package com.vidyarthi.lalkitab

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.vidyarthi.lalkitab.ui.widget.PinchZoomFrameLayout
import com.vidyarthi.lalkitab.utils.LocaleHelper
import com.vidyarthi.lalkitab.utils.WindowInsetsUi

abstract class BaseActivity : AppCompatActivity() {

    /** New Kundli / Saved Kundli screens override to false. */
    protected open fun isPinchZoomEnabled(): Boolean = true

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        val inflater = layoutInflater
        val wrapper = inflater.inflate(R.layout.activity_root_watermark, null) as FrameLayout
        val container = wrapper.findViewById<FrameLayout>(R.id.content_container)
        val zoomHost = createZoomHost()
        inflater.inflate(layoutResID, zoomHost, true)
        container.addView(
            zoomHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        publishContentView(wrapper)
    }

    override fun setContentView(view: View) {
        wrapWithWatermark(view, null)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        wrapWithWatermark(view, params)
    }

    private fun createZoomHost(): PinchZoomFrameLayout =
        PinchZoomFrameLayout(this).apply {
            isZoomEnabled = isPinchZoomEnabled()
        }

    private fun wrapWithWatermark(content: View, params: ViewGroup.LayoutParams?) {
        val inflater = layoutInflater
        val wrapper = inflater.inflate(R.layout.activity_root_watermark, null) as FrameLayout
        val container = wrapper.findViewById<FrameLayout>(R.id.content_container)
        val zoomHost = createZoomHost()
        if (params != null) {
            zoomHost.addView(content, params)
        } else {
            zoomHost.addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        container.addView(
            zoomHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        publishContentView(wrapper)
    }

    private fun publishContentView(wrapper: FrameLayout) {
        super.setContentView(wrapper)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsUi.applyToContent(wrapper.findViewById(R.id.content_container))
    }
}
