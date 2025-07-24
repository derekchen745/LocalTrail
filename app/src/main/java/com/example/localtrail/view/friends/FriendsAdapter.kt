package com.example.localtrail.view.friends

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.controller.ProfilePictureController
import com.example.localtrail.controller.activities.UserProfileActivity
import com.example.localtrail.model.Friend

class FriendsAdapter(private val friends: MutableList<Friend>) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.usernameTextView.text = friend.username
        
        // Load profile picture
        ProfilePictureController.getProfilePictureBase64(friend.userId) { base64Image, _ ->
            if (base64Image != null) {
                Glide.with(holder.itemView.context)
                    .load(base64Image)
                    .circleCrop()
                    .placeholder(R.drawable.placeholder_circle)
                    .error(R.drawable.placeholder_circle)
                    .into(holder.profileImageView)
            } else {
                // Use default image if no profile picture
                holder.profileImageView.setImageResource(R.drawable.placeholder_circle)
            }
        }
        
        // Set click listener on the entire item to view user profile
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, UserProfileActivity::class.java)
            intent.putExtra("USER_ID", friend.userId)
            holder.itemView.context.startActivity(intent)
        }
        
        holder.menuImageView.setOnClickListener {
            showPopupMenu(holder.menuImageView, friend, position, holder.itemView)
        }
    }

    override fun getItemCount(): Int = friends.size
    
    private fun showPopupMenu(anchor: View, friend: Friend, position: Int, itemView: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.friend_options_menu, popupMenu.menu)
        
        // Make the "Remove Friend" text red
        val menu = popupMenu.menu
        val removeFriendItem = menu.findItem(R.id.menu_remove_friend)
        val spannable = android.text.SpannableString(removeFriendItem.title)
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(Color.RED),
            0,
            spannable.length,
            0
        )
        removeFriendItem.title = spannable
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_view_profile -> {
                    val intent = Intent(anchor.context, UserProfileActivity::class.java)
                    intent.putExtra("USER_ID", friend.userId)
                    anchor.context.startActivity(intent)
                    true
                }
                R.id.menu_remove_friend -> {
                    showConfirmationDialog(friend, position, itemView)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun showConfirmationDialog(friend: Friend, position: Int, itemView: View) {
        AlertDialog.Builder(itemView.context)
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.username} from your friends list?")
            .setPositiveButton("Remove") { _, _ ->
                removeFriend(friend, position, itemView)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
