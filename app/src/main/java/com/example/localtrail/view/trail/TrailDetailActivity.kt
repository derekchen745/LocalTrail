package com.example.localtrail.view.trail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.R
import com.example.localtrail.databinding.ActivityTrailDetailBinding

class TrailDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrailDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the action bar since we have a custom toolbar in the fragment
        supportActionBar?.hide()

        if (savedInstanceState == null) {
            val fragment = TrailDetailFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .replace(R.id.trailDetailContainer, fragment)
                .commit()
        }
    }

    // Handle back button click
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
