package com.example.localtrail.view.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.databinding.FragmentFeedBinding
import com.example.localtrail.model.Trail

class FeedFragment : Fragment() {

private var _binding: FragmentFeedBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentFeedBinding.inflate(inflater, container, false)
    return binding.root
  }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = AccountController.getCurrentUser()
        if (user != null) {
            TrailsController.fetchOtherUsersTrails(user.uid) { trails ->
                _binding?.let { binding ->
                    val adapter = FeedAdapter(trails) { trail, menuItemId ->
                        if (menuItemId == R.id.menu_add_friend) {
                            FriendsController.sendFriendRequest(trail.userID) { success, exception ->
                                val message = if (success) {
                                    "Friend request sent!"
                                } else if (exception?.message == "Friend request already pending!") {
                                    "Friend request already pending!"
                                } else {
                                    exception?.localizedMessage ?: "Failed to send friend request."
                                }
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    binding.recyclerViewFeed.adapter = adapter
                    binding.recyclerViewFeed.layoutManager = LinearLayoutManager(requireContext())
                }
            }
        }
    }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
