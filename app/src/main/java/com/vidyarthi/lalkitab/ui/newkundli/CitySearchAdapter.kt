package com.vidyarthi.lalkitab.ui.newkundli

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.CityPick

/**
 * City dropdown driven by [CityRepository] search — does not re-filter/replace typed text
 * while the user is still typing (avoids AutoComplete eating letters like "t").
 */
class CitySearchAdapter(context: Context) : ArrayAdapter<CityPick>(
    context,
    R.layout.dropdown_line_kalam,
    android.R.id.text1,
    mutableListOf()
) {

    private val items = mutableListOf<CityPick>()

    fun replaceAll(newItems: List<CityPick>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clearItems() {
        if (items.isEmpty()) return
        items.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): CityPick? = items.getOrNull(position)

    override fun getFilter(): Filter = NoReplaceFilter()

    private inner class NoReplaceFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults =
            FilterResults().apply {
                values = items
                count = items.size
            }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }
}
