package com.example.localtrail.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.activities.LoginActivity
import com.example.localtrail.databinding.FragmentProfileBinding

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
        val user = com.example.localtrail.controller.AccountController.getCurrentUser()
        if (user == null) {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
            return
        }
        // Set username to empty while loading to avoid flicker
        binding.textViewProfileUsername.text = ""
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: user.username
                    binding.textViewProfileUsername.text = username
                } else {
                    binding.textViewProfileUsername.text = user.username
                }
            }
            .addOnFailureListener {
                binding.textViewProfileUsername.text = user.username
            }
        // TODO: Replace with real bio
        binding.textViewProfileBio.text = "4th Year computer Engineering student enjoying hiking and dawe;wek;k ;asdl ka;dk;asldk; askd ;alskd;laskd"
        // TODO: Replace with real friend count
        binding.textViewProfileFriends.text = "10 Friends"

        // Set up logout icon
        binding.buttonLogoutIcon.setOnClickListener {
            AccountController.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        // Set up tabs
        val tabLayout = binding.tabLayoutTrails
        tabLayout.addTab(tabLayout.newTab().setText("My Trails"))
        tabLayout.addTab(tabLayout.newTab().setText("Saved Trails"))

        // Show My Trails by default
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
