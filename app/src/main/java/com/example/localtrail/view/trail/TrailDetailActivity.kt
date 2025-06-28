package com.example.localtrail.view.trail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.R

class TrailDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_detail)

        if (savedInstanceState == null) {
            val fragment = TrailDetailFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .replace(R.id.trailDetailContainer, fragment)
                .commit()
        }
    }
}
