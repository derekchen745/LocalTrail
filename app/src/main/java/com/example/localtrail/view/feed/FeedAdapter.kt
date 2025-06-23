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
        private const val TYPE_HEADER = 0
        private const val TYPE_TRAIL = 1
        private const val TYPE_EMPTY = 2
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardTrail)
        val avatar: ImageView = view.findViewById(R.id.imageTrailAvatar)
        val nameText: TextView = view.findViewById(R.id.textTrailName)
        val locationText: TextView = view.findViewById(R.id.textTrailLocation)
        val dateText: TextView = view.findViewById(R.id.textTrailDate)
        val userText: TextView = view.findViewById(R.id.textTrailUser)
        val menu: ImageView = view.findViewById(R.id.imageTrailMenu)
        val image: ImageView = view.findViewById(R.id.imageTrailPhoto)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emptyText: TextView = view.findViewById(R.id.textEmptyFeed)
    }

    override fun getItemViewType(position: Int): Int {
        return if (trails.isEmpty()) TYPE_EMPTY
        else if (position == 0) TYPE_HEADER
        else TYPE_TRAIL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_feed_header, parent, false)
            )
            TYPE_TRAIL -> FeedViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_feed_trail, parent, false)
            )
            else -> EmptyViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_feed_empty, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FeedViewHolder -> {
                val trail = trails[position - 1] // -1 for header
                holder.nameText.text = trail.name
                holder.locationText.text = trail.location
                holder.userText.text = "Derek Chen" // Placeholder
                holder.dateText.text = "June 22, 2025" // Placeholder
                // Placeholders for avatar and image are already set in XML
            }
            is EmptyViewHolder -> {
                holder.emptyText.text = holder.itemView.context.getString(R.string.feed_empty_message)
            }
            // HeaderViewHolder needs no binding
        }
    }

    override fun getItemCount(): Int = when {
        trails.isEmpty() -> 1
        else -> trails.size + 1 // +1 for header
    }
}
