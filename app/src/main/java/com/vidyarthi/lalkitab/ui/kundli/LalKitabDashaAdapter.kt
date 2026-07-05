package com.vidyarthi.lalkitab.ui.kundli

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.vidyarthi.lalkitab.R
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MahadashaCardUi(
    val segmentIndex: Int,
    val planetLabel: String,
    val startLifeYear: Int,
    val dateStart: String,
    val dateEnd: String,
    val isCurrent: Boolean
)

class LalKitabDashaAdapter(
    private val items: List<MahadashaCardUi>,
    private val onCardClick: (segmentIndex: Int) -> Unit
) : RecyclerView.Adapter<LalKitabDashaAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val timelineLineTop: View = view.findViewById(R.id.timelineLineTop)
        val timelineDot: View = view.findViewById(R.id.timelineDot)
        val timelineLineBottom: View = view.findViewById(R.id.timelineLineBottom)
        val card: MaterialCardView = view.findViewById(R.id.cardDasha)
        val cardInner: LinearLayout = view.findViewById(R.id.cardDashaInner)
        val accentBarCurrent: View = view.findViewById(R.id.accentBarCurrent)
        val tvPlanet: TextView = view.findViewById(R.id.textMahadasha)
        val tvCurrentBadge: TextView = view.findViewById(R.id.textCurrentBadge)
        val tvAgeBadge: TextView = view.findViewById(R.id.textAge)
        val tvDateRange: TextView = view.findViewById(R.id.textStartDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dasha_timeline, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        val row = items[position]
        val isFirst = position == 0
        val isLast = position == items.lastIndex

        holder.timelineLineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
        holder.timelineLineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

        holder.tvPlanet.text = row.planetLabel
        holder.tvAgeBadge.text = ctx.getString(R.string.dasha_life_year_badge, row.startLifeYear)
        holder.tvDateRange.text = ctx.getString(R.string.dasha_calendar_range, row.dateStart, row.dateEnd)

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()
        val start = runCatching { LocalDate.parse(row.dateStart, formatter) }.getOrNull()
        val end = runCatching { LocalDate.parse(row.dateEnd, formatter) }.getOrNull()
        val isCurrent = row.isCurrent ||
            (start != null && end != null && !today.isBefore(start) && !today.isAfter(end))

        val dm = holder.itemView.resources.displayMetrics
        val strokeNormal = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm).toInt()
        val strokeCurrent = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, dm).toInt()
        val gold = Color.parseColor("#D4A017")
        val strokeIdle = ContextCompat.getColor(ctx, R.color.dasha_card_stroke)

        holder.card.setCardBackgroundColor(Color.TRANSPARENT)
        holder.itemView.setOnClickListener { onCardClick(row.segmentIndex) }
        holder.card.setOnClickListener { onCardClick(row.segmentIndex) }
        holder.cardInner.setOnClickListener { onCardClick(row.segmentIndex) }

        if (isCurrent) {
            holder.timelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_current)
            holder.timelineLineTop.setBackgroundColor(gold)
            holder.timelineLineBottom.setBackgroundColor(gold)
            holder.card.strokeWidth = strokeCurrent
            holder.card.strokeColor = gold
            holder.card.cardElevation = 6f
            holder.cardInner.setBackgroundResource(R.drawable.bg_dasha_timeline_card_current)
            holder.accentBarCurrent.visibility = View.VISIBLE
            holder.tvCurrentBadge.visibility = View.VISIBLE
            TextViewCompat.setTextAppearance(
                holder.tvPlanet,
                R.style.TextAppearance_Vidyarthi_DashaMahaCurrent
            )
            TextViewCompat.setTextAppearance(
                holder.tvDateRange,
                R.style.TextAppearance_Vidyarthi_DashaMetaCurrent
            )
        } else {
            holder.timelineDot.setBackgroundResource(R.drawable.bg_timeline_dot)
            holder.timelineLineTop.setBackgroundColor(0x66D4A017)
            holder.timelineLineBottom.setBackgroundColor(0x66FF9800)
            holder.card.strokeWidth = strokeNormal
            holder.card.strokeColor = strokeIdle
            holder.card.cardElevation = 0f
            holder.cardInner.setBackgroundResource(R.drawable.bg_dasha_timeline_card)
            holder.accentBarCurrent.visibility = View.GONE
            holder.tvCurrentBadge.visibility = View.GONE
            TextViewCompat.setTextAppearance(
                holder.tvPlanet,
                R.style.TextAppearance_Vidyarthi_DashaMaha
            )
            TextViewCompat.setTextAppearance(
                holder.tvDateRange,
                R.style.TextAppearance_Vidyarthi_DashaMeta
            )
        }

        TextViewCompat.setTextAppearance(holder.tvAgeBadge, R.style.TextAppearance_Vidyarthi_CaptionBold)
        TextViewCompat.setTextAppearance(holder.tvCurrentBadge, R.style.TextAppearance_Vidyarthi_AccentTag)
    }

    override fun getItemCount() = items.size
}
