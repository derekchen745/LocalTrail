package com.example.localtrail.view.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.model.Trail
import com.example.localtrail.controller.FriendsController

class FeedAdapter(
    private val trails: List<Trail>,
    private val onMenuAction: ((Trail, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                val trail = trails[position - 1]
                holder.nameText.text = trail.name
                holder.locationText.text = trail.location
                holder.userText.text = trail.username
                holder.dateText.text = "June 22, 2025"

                // Add click listener to open trail summary
                holder.card.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = android.content.Intent(context, com.example.localtrail.view.trail.TrailDetailActivity::class.java)
                    intent.putExtra("trail", trail)
                    context.startActivity(intent)
                }

                holder.menu.setOnClickListener { view ->
                    TrailsController.isTrailSavedByUser(trail.id) { isSaved ->
                        FriendsController.isFriend(trail.userID) { isFriend, exception ->
                            if (exception != null) {
                                // Handle error
                                return@isFriend
                            }

                            val popup = PopupMenu(view.context, view)
                            val saveOption = if (isSaved) "Unsave Trail" else "Save Trail"
                            popup.menu.add(saveOption)
                            popup.menu.add("View Profile")

                            if (!isFriend) {
                                popup.menu.add(view.context.getString(R.string.menu_add_friend)).setIcon(R.drawable.ic_add_gray_32)
                            }

                            popup.setOnMenuItemClickListener { menuItem ->
                                when (menuItem.title) {
                                    "Save Trail" -> {
                                        TrailsController.saveTrailToUser(trail) { success, exception ->
                                            val message = if (success) {
                                                "Trail saved to your collection"
                                            } else {
                                                "Failed to save trail: ${exception?.message ?: "Unknown error"}"
                                            }
                                            Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "Unsave Trail" -> {
                                        TrailsController.removeTrailFromUser(trail.id) { success, exception ->
                                            val message = if (success) {
                                                "Trail removed from your collection"
                                            } else {
                                                "Failed to remove trail: ${exception?.message ?: "Unknown error"}"
                                            }
                                            Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "View Profile" -> {
                                        // TODO: Implement view profile functionality
                                    }
                                    view.context.getString(R.string.menu_add_friend) -> {
                                        onMenuAction?.invoke(trail, R.id.menu_add_friend)
                                    }
                                }
                                true
                            }
                            popup.show()
                        }
                    }
                }
            }
            is EmptyViewHolder -> {
                holder.emptyText.text = holder.itemView.context.getString(R.string.feed_empty_message)
            }
            // HeaderViewHolder needs no binding
        }
    }

    override fun getItemCount(): Int = when {
        trails.isEmpty() -> 1
        else -> trails.size + 1
    }
}
