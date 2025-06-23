package com.example.localtrail.view.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.databinding.FragmentFeedBinding
import com.example.localtrail.model.Trail

class FeedFragment : Fragment() {

private var _binding: FragmentFeedBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
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
                    val adapter = FeedAdapter(trails)
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
