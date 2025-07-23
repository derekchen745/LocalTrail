package com.example.localtrail.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ProfilePictureController {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    /**
     * Uploads a profile picture by converting to Base64 and storing in Firestore
     * @param context Application context
     * @param imageUri The URI of the image to upload
     * @param onResult Callback with (success: Boolean, imageUrl: String?, exception: Exception?)
     */
    fun uploadProfilePicture(context: Context, imageUri: Uri, onResult: (Boolean, String?, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, null, Exception("User not logged in"))
        
        Log.d("ProfilePictureController", "Starting upload for user: ${currentUser.uid}")
        Log.d("ProfilePictureController", "Image URI: $imageUri")
        
        try {
            // Convert image to Base64
            val base64Image = convertImageToBase64(context, imageUri)
            
            if (base64Image == null) {
                onResult(false, null, Exception("Failed to process image"))
                return
            }
            
            Log.d("ProfilePictureController", "Image converted to Base64, size: ${base64Image.length} chars")
            
            // Store Base64 in Firestore
            updateUserProfilePicture(base64Image, onResult)
            
        } catch (e: Exception) {
            Log.e("ProfilePictureController", "Upload failed", e)
            onResult(false, null, e)
        }
    }
    
    /**
     * Converts image URI to Base64 string with compression
     */
    private fun convertImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return null
            
            // Compress image to reduce size (important for Firestore 1MB limit)
            val maxSize = 300 // Max width/height in pixels
            val scaledBitmap = scaleBitmap(originalBitmap, maxSize)
            
            // Convert to Base64
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ProfilePictureController", "Failed to convert image to Base64", e)
            null
        }
    }
    
    /**
     * Scales bitmap to fit within max size while maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scaleFactor = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Updates the user's profile picture Base64 in Firestore
     * @param base64Image The Base64 encoded image
     * @param onResult Callback with (success: Boolean, imageUrl: String?, exception: Exception?)
     */
    private fun updateUserProfilePicture(base64Image: String, onResult: (Boolean, String?, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, null, Exception("User not logged in"))
        
        db.collection("users").document(currentUser.uid)
            .update("profilePictureBase64", base64Image)
            .addOnSuccessListener {
                Log.d("ProfilePictureController", "Profile picture updated successfully")
                onResult(true, base64Image, null)
            }
            .addOnFailureListener { e ->
                Log.e("ProfilePictureController", "Failed to update profile picture", e)
                onResult(false, null, e)
            }
    }
    
    /**
     * Gets the current user's profile picture Base64
     * @param onResult Callback with (base64Image: String?, exception: Exception?)
     */
    fun getProfilePictureBase64(userId: String? = null, onResult: (String?, Exception?) -> Unit) {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return onResult(null, Exception("User not logged in"))
        
        db.collection("users").document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                val profilePictureBase64 = document.getString("profilePictureBase64")
                onResult(profilePictureBase64, null)
            }
            .addOnFailureListener { e ->
                onResult(null, e)
            }
    }
}
