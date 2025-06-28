package com.example.localtrail.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.databinding.IncludeMyTrailsBinding

class MyTrailsTabFragment : Fragment() {
    private var _binding: IncludeMyTrailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = IncludeMyTrailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = AccountController.getCurrentUser() ?: return
        TrailsController.fetchUserTrails(user.uid) { trails ->
            val adapter = TrailsAdapter(trails) { trail ->
                val intent = Intent(requireContext(), com.example.localtrail.view.trail.TrailDetailActivity::class.java)
                intent.putExtra("trail", trail)
                startActivity(intent)
            }
            binding.recyclerViewTrails.adapter = adapter
            binding.recyclerViewTrails.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
