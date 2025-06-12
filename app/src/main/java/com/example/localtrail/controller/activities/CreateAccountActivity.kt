package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.controller.AccountController
import com.example.localtrail.R

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val buttonCreateAccount = findViewById<Button>(R.id.buttonCreateAccount)
        val textViewBackToLogin = findViewById<TextView>(R.id.textViewBackToLogin)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val editTextConfirmPassword = findViewById<EditText>(R.id.editTextConfirmPassword)

        textViewBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonCreateAccount.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showCreateAccountError("Please enter a valid email address")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showCreateAccountError("Passwords do not match")
                return@setOnClickListener
            }
            AccountController.createAccount(email, password) { success, exception ->
                if (success) {
                    onCreateAccountSuccess()
                } else {
                    showCreateAccountError("Authentication failed.")
                }
            }
        }
    }

    private fun onCreateAccountSuccess() {
        Log.d(TAG, "createUserWithEmail:success")
        Toast.makeText(
            baseContext,
            "Account created successfully.",
            Toast.LENGTH_SHORT,
        ).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showCreateAccountError(message: String) {
        Log.w(TAG, "createUserWithEmail:failure")
        Toast.makeText(
            baseContext,
            message,
            Toast.LENGTH_SHORT,
        ).show()
    }

    companion object {
        private const val TAG = "CreateAccountActivity"
    }
}
