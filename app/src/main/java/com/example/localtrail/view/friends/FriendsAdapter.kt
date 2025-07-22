package com.example.localtrail.view.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.model.Friend

class FriendsAdapter(private val friends: MutableList<Friend>) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.usernameTextView.text = friend.username
        holder.profileImageView.setImageResource(R.drawable.placeholder_circle)
        
        holder.menuImageView.setOnClickListener {
            showPopupMenu(holder.menuImageView, friend, position, holder.itemView)
        }
    }

    override fun getItemCount(): Int = friends.size
    
    private fun showPopupMenu(anchor: View, friend: Friend, position: Int, itemView: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.friend_options_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_remove_friend -> {
                    removeFriend(friend, position, itemView)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun removeFriend(friend: Friend, position: Int, itemView: View) {
        FriendsController.removeFriend(friend.userId) { success, exception ->
            if (success) {
                // Remove from local list and update RecyclerView
                friends.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, friends.size)
                Toast.makeText(itemView.context, "Friend removed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(itemView.context, "Failed to remove friend: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewUsername)
        val profileImageView: ImageView = itemView.findViewById(R.id.imageViewProfile)
        val menuImageView: ImageView = itemView.findViewById(R.id.imageViewFriendMenu)
    }
}
