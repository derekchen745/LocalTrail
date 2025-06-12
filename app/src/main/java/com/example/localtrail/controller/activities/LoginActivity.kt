package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = AccountController.getCurrentUser()
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        buttonLogin.setOnClickListener {
            val email = findViewById<EditText>(R.id.editTextUsername).text.toString()
            val password = findViewById<EditText>(R.id.editTextPassword).text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                showLoginError("Please enter email and password")
                return@setOnClickListener
            }
            AccountController.login(email, password) { success, exception ->
                if (success) {
                    onLoginSuccess()
                } else {
                    showLoginError("Authentication failed.")
                }
            }
        }

        val textViewCreateAccount = findViewById<TextView>(R.id.textViewCreateAccount)
        textViewCreateAccount.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun onLoginSuccess() {
        Log.d(TAG, "signInWithEmail:success")
        Toast.makeText(
            baseContext,
            "Authentication successful.",
            Toast.LENGTH_SHORT,
        ).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoginError(message: String) {
        Log.w(TAG, "signInWithEmail:failure")
        Toast.makeText(
            baseContext,
            message,
            Toast.LENGTH_SHORT,
        ).show()
    }
}
