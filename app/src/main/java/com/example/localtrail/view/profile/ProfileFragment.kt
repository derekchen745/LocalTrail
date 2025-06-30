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
        binding.textViewProfileBio.setText("")
        
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
            // Only show default text visually, not in the EditText value
            if (user.description.isNotEmpty()) {
                binding.textViewProfileBio.setText(user.description)
            } else {
                binding.textViewProfileBio.setText("")
                binding.textViewProfileBio.hint = getString(R.string.default_bio_text)
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
        
        val descriptionEditText = binding.textViewProfileBio
        descriptionEditText.isFocusable = false
        descriptionEditText.isClickable = true
        descriptionEditText.setOnClickListener {
            descriptionEditText.isFocusableInTouchMode = true
            descriptionEditText.isFocusable = true
            descriptionEditText.isCursorVisible = true
            descriptionEditText.requestFocus()
            descriptionEditText.setSelection(descriptionEditText.text.length)
        }
        // In the focus change listener, only save if not blank and not default
        descriptionEditText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                descriptionEditText.isCursorVisible = false
                descriptionEditText.isFocusable = false
                descriptionEditText.isFocusableInTouchMode = false
                val newDescription = descriptionEditText.text.toString().trim()
                lifecycleScope.launch {
                    val user = AccountController.getUserDetails()
                    if (user != null && newDescription != user.description && newDescription.isNotBlank()) {
                        AccountController.updateUserDescription(newDescription)
                    }
                }
            }
        }
        
        binding.textViewProfileFriends.setOnClickListener {
            val intent = Intent(requireContext(), FriendsActivity::class.java)
            startActivity(intent)
        }
    }

    fun showTrailDetail(trail: com.example.localtrail.model.Trail) {
        val fragment = com.example.localtrail.view.trail.TrailDetailFragment()
        val args = android.os.Bundle().apply { putParcelable("trail", trail) }
        fragment.arguments = args
        childFragmentManager.beginTransaction()
            .replace(binding.frameLayoutTrailsContent.id, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
