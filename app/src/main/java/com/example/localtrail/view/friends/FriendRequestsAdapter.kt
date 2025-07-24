package com.example.localtrail.view.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localtrail.R
import com.example.localtrail.controller.ProfilePictureController
import com.example.localtrail.model.FriendRequest

class FriendRequestsAdapter(
    private val requests: MutableList<FriendRequest>,
    private val onAction: (FriendRequest, Action) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder>() {

    enum class Action {
        ACCEPT, DENY
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.imageViewProfile)
        val usernameText: TextView = view.findViewById(R.id.textUsername)
        val messageText: TextView = view.findViewById(R.id.textMessage)
        val acceptButton: ImageView = view.findViewById(R.id.buttonAccept)
        val denyButton: ImageView = view.findViewById(R.id.buttonDeny)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friendRequest = requests[position]
        holder.usernameText.text = friendRequest.username

        // Load profile picture
        ProfilePictureController.getProfilePictureBase64(friendRequest.userId) { base64Image, _ ->
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

        // Handle message display
        if (!friendRequest.message.isNullOrBlank()) {
            holder.messageText.text = friendRequest.message
            holder.messageText.visibility = View.VISIBLE
        } else {
            holder.messageText.visibility = View.GONE
        }

        holder.acceptButton.setOnClickListener {
            onAction(friendRequest, Action.ACCEPT)
        }

        holder.denyButton.setOnClickListener {
            showDeclineConfirmationDialog(friendRequest, holder.itemView)
        }
    }

    override fun getItemCount(): Int = requests.size

    private fun showDeclineConfirmationDialog(friendRequest: FriendRequest, itemView: View) {
        AlertDialog.Builder(itemView.context)
            .setTitle("Decline Friend Request")
            .setMessage("Are you sure you want to decline the friend request from ${friendRequest.username}?")
            .setPositiveButton("Decline") { _, _ ->
                onAction(friendRequest, Action.DENY)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun removeRequest(friendRequest: FriendRequest) {
        val index = requests.indexOf(friendRequest)
        if (index != -1) {
            requests.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
