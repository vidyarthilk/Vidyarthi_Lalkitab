package com.vidyarthi.lalkitab.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import com.vidyarthi.lalkitab.R

/** Spinner dropdown: 5 visible numbers + scroll, cream background. */
object YearSpinnerUi {

    fun applyCompactDropdownStyle(spinner: Spinner, context: Context) {
        val bg = ContextCompat.getDrawable(context, R.drawable.bg_year_spinner_dropdown)
        val heightPx = compactDropDownHeightPx(context)
        val appSpinner = spinner as? AppCompatSpinner
        if (appSpinner != null) {
            if (bg != null) appSpinner.setPopupBackgroundDrawable(bg)
            setListPopupHeight(appSpinner, heightPx)
            appSpinner.post { setListPopupHeight(appSpinner, heightPx) }
        }
    }

    private fun setListPopupHeight(spinner: AppCompatSpinner, heightPx: Int) {
        try {
            val field = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val popup = field.get(spinner) as ListPopupWindow
            popup.height = heightPx
        } catch (_: Exception) {
            // XML android:dropDownHeight remains fallback
        }
    }

    private fun compactDropDownHeightPx(context: Context): Int {
        val rows = context.resources.getInteger(R.integer.spinner_dropdown_max_visible).coerceAtLeast(1)
        val inflater = LayoutInflater.from(context)
        val row = inflater.inflate(R.layout.spinner_dropdown_kalam, null, false)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.widthPixels,
            View.MeasureSpec.AT_MOST
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        row.measure(widthSpec, heightSpec)
        val rowPx = row.measuredHeight
        if (rowPx > 0) return rowPx * rows
        val fallbackRow = context.resources.getDimensionPixelSize(R.dimen.spinner_dropdown_row_height)
        return fallbackRow * rows
    }

    fun createAdapter(context: Context, items: List<String>): ArrayAdapter<String> =
        ArrayAdapter(
            context,
            R.layout.spinner_item_kalam,
            android.R.id.text1,
            items
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_kalam)
        }

    fun createYearAdapter(context: Context, maxYear: Int = 120): ArrayAdapter<String> =
        createAdapter(context, (1..maxYear).map { "$it" })
}
