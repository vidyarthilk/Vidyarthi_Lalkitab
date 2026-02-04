package com.example.vidyarthi_lalkitab.ui.savedkundli

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vidyarthi_lalkitab.R
import com.example.vidyarthi_lalkitab.data.entity.KundliEntity

class SavedKundliAdapter(
    private val list: List<KundliEntity>
) : RecyclerView.Adapter<SavedKundliAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_kundli, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val k = list[position]
        holder.tvName.text = k.name
        holder.tvDetails.text = "${k.date} | ${k.time} | ${k.city}"
    }
}
