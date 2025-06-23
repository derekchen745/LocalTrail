package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.databinding.ActivityMainBinding
import androidx.navigation.ui.setupWithNavController

class MainActivity : BaseAuthenticatedActivity() {

private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navView.setupWithNavController(navController)

        // Hide toolbar on profile page
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_profile) {
                binding.toolbar.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.VISIBLE
            }
        }

        navView.setOnItemSelectedListener { item ->
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            when (item.itemId) {
                R.id.navigation_profile -> {
                    navController.popBackStack(R.id.navigation_profile, false)
                    navController.navigate(R.id.navigation_profile)
                    true
                }
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_dashboard -> {
                    navController.popBackStack(R.id.navigation_dashboard, false)
                    navController.navigate(R.id.navigation_dashboard)
                    true
                }
                else -> false
            }
        }
    }
}