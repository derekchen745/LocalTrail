package com.example.localtrail.view.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.model.Trail

class TrailsAdapter(private val trails: List<Trail>) : RecyclerView.Adapter<TrailsAdapter.TrailViewHolder>() {
    class TrailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.textTrailName)
        val locationText: TextView = view.findViewById(R.id.textTrailLocation)
        val descriptionText: TextView = view.findViewById(R.id.textTrailDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trail, parent, false)
        return TrailViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrailViewHolder, position: Int) {
        val trail = trails[position]
        holder.nameText.text = trail.name
        holder.locationText.text = trail.location
        holder.descriptionText.text = trail.description
    }

    override fun getItemCount() = trails.size
}
