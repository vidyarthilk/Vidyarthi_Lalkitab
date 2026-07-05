package com.vidyarthi.lalkitab.ui.kundli

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vidyarthi.lalkitab.R

data class AntardashaRowUi(
    val antarPlanetLabel: String,
    val mahadashaPlanetLabel: String,
    val dateStart: String,
    val dateEnd: String
)

class LalKitabAntarAdapter(private val items: List<AntardashaRowUi>) :
    RecyclerView.Adapter<LalKitabAntarAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlanet: TextView = itemView.findViewById(R.id.tvAntarPlanet)
        val tvDates: TextView = itemView.findViewById(R.id.tvAntarDates)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lalkitab_antar_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        val row = items[position]
        holder.tvPlanet.text = ctx.getString(
            R.string.lalkitab_antar_card_title,
            row.antarPlanetLabel,
            row.mahadashaPlanetLabel
        )
        holder.tvDates.text = ctx.getString(R.string.dasha_calendar_range, row.dateStart, row.dateEnd)
    }

    override fun getItemCount() = items.size
}
