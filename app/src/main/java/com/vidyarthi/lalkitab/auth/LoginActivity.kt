package com.vidyarthi.lalkitab.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.vidyarthi.lalkitab.BaseActivity
import com.vidyarthi.lalkitab.R

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<MaterialToolbar>(R.id.loginToolbar).setNavigationOnClickListener { finish() }

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val tvHint = findViewById<TextView>(R.id.tvLoginHint)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        val hasAccount = UserAccountManager.hasRegisteredAccount(this)
        if (hasAccount) {
            UserAccountManager.registeredEmail(this)?.let { etEmail.setText(it) }
            tvHint.setText(R.string.auth_login_hint_existing)
            btnRegister.visibility = android.view.View.GONE
        } else {
            tvHint.setText(R.string.auth_register_hint)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            when (val result = UserAccountManager.login(this, email, password)) {
                is UserAccountManager.AuthResult.Success -> {
                    Toast.makeText(this, R.string.auth_login_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is UserAccountManager.AuthResult.Error -> {
                    Toast.makeText(this, result.messageRes, Toast.LENGTH_LONG).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            when (val result = UserAccountManager.register(this, email, password)) {
                is UserAccountManager.AuthResult.Success -> {
                    Toast.makeText(this, R.string.auth_register_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is UserAccountManager.AuthResult.Error -> {
                    Toast.makeText(this, result.messageRes, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
