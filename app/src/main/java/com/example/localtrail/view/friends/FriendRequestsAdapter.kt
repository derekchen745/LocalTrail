package com.example.localtrail.view.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.model.FriendRequest

class FriendRequestsAdapter(
    private val requests: MutableList<FriendRequest>,
    private val onAction: (FriendRequest, Action) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder>() {

    enum class Action {
        ACCEPT, DENY
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.textUsername)
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

        holder.acceptButton.setOnClickListener {
            onAction(friendRequest, Action.ACCEPT)
        }

        holder.denyButton.setOnClickListener {
            onAction(friendRequest, Action.DENY)
        }
    }

    override fun getItemCount(): Int = requests.size

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
