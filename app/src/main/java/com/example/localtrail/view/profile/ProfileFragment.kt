package com.example.localtrail.view.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.activities.LoginActivity
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.controller.ProfilePictureController
import com.example.localtrail.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadProfilePicture(uri)
            }
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                uploadProfilePicture(uri)
            }
        }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

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
        
        FriendsController.getNumberOfFriends { count, exception ->
            if (exception != null) {
                binding.textViewProfileFriends.text = "Error loading friends"
                return@getNumberOfFriends
            }
            binding.textViewProfileFriends.text = "$count Friends"
        }

        // Set up profile picture click listener and hover effects
        binding.frameProfileAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        // Add touch effects to show/hide edit icon
        binding.frameProfileAvatar.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    binding.imageEditIcon.visibility = View.VISIBLE
                    binding.imageProfileAvatar.alpha = 0.8f
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    binding.imageEditIcon.visibility = View.GONE
                    binding.imageProfileAvatar.alpha = 1.0f
                }
            }
            false // Allow click events to be processed
        }

        // Load existing profile picture
        loadProfilePicture()

        binding.buttonLogoutIcon.setOnClickListener {
            showLogoutConfirmationDialog()
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
        
        binding.root.setOnTouchListener { _, _ ->
            val descriptionEditText = binding.textViewProfileBio
            if (descriptionEditText.isFocusable && descriptionEditText.isCursorVisible) {
                descriptionEditText.clearFocus()
            }
            false
        }

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
            findNavController().navigate(R.id.navigation_friends)
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
    
    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setPositiveButton("Choose from Gallery") { _, _ ->
                openImagePicker()
            }
            .setNegativeButton("Take Photo") { _, _ ->
                checkCameraPermissionAndOpen()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open image picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            
            // Create a file to save the image
            val photoFile = createImageFile()
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                cameraLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createImageFile(): File? {
        return try {
            // Create an image file name
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error creating image file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    private fun checkCameraPermissionAndOpen() {
        if (hasCameraPermission()) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun uploadProfilePicture(imageUri: Uri) {
        // Show loading state
        binding.imageProfileAvatar.alpha = 0.5f
        
        ProfilePictureController.uploadProfilePicture(requireContext(), imageUri) { success, base64Image, exception ->
            // Restore normal state
            binding.imageProfileAvatar.alpha = 1.0f
            
            if (success && base64Image != null) {
                // Load the new Base64 image
                Glide.with(this@ProfileFragment)
                    .load(base64Image)
                    .circleCrop()
                    .into(binding.imageProfileAvatar)
                
                Toast.makeText(requireContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                val errorMessage = exception?.message ?: "Failed to upload profile picture"
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadProfilePicture() {
        ProfilePictureController.getProfilePictureBase64 { base64Image, _ ->
            if (base64Image != null) {
                Glide.with(this@ProfileFragment)
                    .load(base64Image)
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle_black_24dp)
                    .error(R.drawable.ic_account_circle_black_24dp)
                    .into(binding.imageProfileAvatar)
            }
            // If no profile picture or error, keep default image
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        AccountController.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
