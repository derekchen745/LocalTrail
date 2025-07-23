package com.example.localtrail.view.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.model.Trail
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

class TrailsAdapter(
    private val onTrailClick: (Trail) -> Unit
) : RecyclerView.Adapter<TrailsAdapter.TrailViewHolder>() {

    private val trails = mutableListOf<Trail>()
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    class TrailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardTrail)
        val nameText: TextView = itemView.findViewById(R.id.textTrailName)
        val dateText: TextView = itemView.findViewById(R.id.textTrailDate)
        val locationText: TextView = itemView.findViewById(R.id.textTrailLocation)
        val descriptionText: TextView = itemView.findViewById(R.id.textTrailDescription)
        val tagChipGroup: ChipGroup = itemView.findViewById(R.id.tagChipGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trail, parent, false)
        return TrailViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrailViewHolder, position: Int) {
        val trail = trails[position]
        holder.nameText.text = trail.name
        holder.dateText.text = dateFormat.format(trail.createdAt)
        holder.locationText.text = trail.location
        
        // Handle description visibility
        if (trail.description.isNullOrBlank()) {
            holder.descriptionText.visibility = View.GONE
        } else {
            holder.descriptionText.visibility = View.VISIBLE
            holder.descriptionText.text = trail.description
        }
        
        holder.card.setOnClickListener {
            onTrailClick.invoke(trail)
        }

        // Clear existing chips
        holder.tagChipGroup.removeAllViews()

        // Add chips for each tag
        trail.tags?.forEach { tag ->
            val chip = Chip(holder.itemView.context).apply {
                text = tag
                isCheckable = false
                setChipBackgroundColorResource(R.color.chip_background)
                setTextColor(ContextCompat.getColor(context, R.color.chip_text))
            }
            holder.tagChipGroup.addView(chip)
        }
    }

    override fun getItemCount() = trails.size

    fun updateTrails(newTrails: List<Trail>) {
        trails.clear()
        trails.addAll(newTrails)
        notifyDataSetChanged()
    }
}
