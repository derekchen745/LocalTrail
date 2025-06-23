package com.example.localtrail.view.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.model.Trail

class FeedAdapter(private val trails: List<Trail>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_TRAIL = 0
        private const val TYPE_EMPTY = 1
    }

    class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardTrail)
        val image: ImageView = view.findViewById(R.id.imageTrailPhoto)
        val nameText: TextView = view.findViewById(R.id.textTrailName)
        val locationText: TextView = view.findViewById(R.id.textTrailLocation)
        val descriptionText: TextView = view.findViewById(R.id.textTrailDescription)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emptyText: TextView = view.findViewById(R.id.textEmptyFeed)
    }

    override fun getItemViewType(position: Int): Int {
        return if (trails.isEmpty()) TYPE_EMPTY else TYPE_TRAIL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_TRAIL) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_trail, parent, false)
            FeedViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_empty, parent, false)
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedViewHolder) {
            val trail = trails[position]
            holder.nameText.text = trail.name
            holder.locationText.text = trail.location
            holder.descriptionText.text = trail.description
        } else if (holder is EmptyViewHolder) {
            holder.emptyText.text = holder.itemView.context.getString(R.string.feed_empty_message)
        }
    }

    override fun getItemCount() = if (trails.isEmpty()) 1 else trails.size
}
