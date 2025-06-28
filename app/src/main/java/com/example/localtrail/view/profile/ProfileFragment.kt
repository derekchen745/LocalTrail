package com.example.localtrail.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.activities.LoginActivity
import com.example.localtrail.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (AccountController.getCurrentUser() == null) {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
            return
        }
        
        binding.textViewProfileUsername.text = ""
        binding.textViewProfileBio.text = ""
        
        lifecycleScope.launch {
            val user = AccountController.getUserDetails()
            
            if (user == null) {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
                return@launch
            }
            
            binding.textViewProfileUsername.text = if (user.username.isNotEmpty()) {
                user.username
            } else {
                user.email
            }
            
            binding.textViewProfileBio.text = if (user.description.isNotEmpty()) {
                user.description
            } else {
                getString(R.string.default_bio_text)
            }
        }
        
            
        // TODO: Replace with real friend count
        binding.textViewProfileFriends.text = "10 Friends"

        binding.buttonLogoutIcon.setOnClickListener {
            AccountController.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        val tabLayout = binding.tabLayoutTrails
        tabLayout.addTab(tabLayout.newTab().setText("My Trails"))
        tabLayout.addTab(tabLayout.newTab().setText("Saved Trails"))

        childFragmentManager.beginTransaction()
            .replace(binding.frameLayoutTrailsContent.id, MyTrailsTabFragment())
            .commit()

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val fragment = when (tab.position) {
                    0 -> MyTrailsTabFragment()
                    1 -> SavedTrailsTabFragment()
                    else -> MyTrailsTabFragment()
                }
                childFragmentManager.beginTransaction()
                    .replace(binding.frameLayoutTrailsContent.id, fragment)
                    .commit()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
