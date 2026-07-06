package com.vidyarthi.lalkitab.auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.vidyarthi.lalkitab.BaseActivity
import com.vidyarthi.lalkitab.R
import kotlinx.coroutines.launch

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

        UserAccountManager.lastUsedEmail(this)?.let { etEmail.setText(it) }
        tvHint.setText(R.string.auth_register_hint)

        btnLogin.setOnClickListener {
            performAuth(btnLogin, btnRegister, etEmail, etPassword, register = false)
        }

        btnRegister.setOnClickListener {
            performAuth(btnLogin, btnRegister, etEmail, etPassword, register = true)
        }
    }

    private fun performAuth(
        btnLogin: MaterialButton,
        btnRegister: MaterialButton,
        etEmail: EditText,
        etPassword: EditText,
        register: Boolean
    ) {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        setAuthInProgress(btnLogin, btnRegister, true)

        lifecycleScope.launch {
            val result = if (register) {
                UserAccountManager.register(this@LoginActivity, email, password)
            } else {
                UserAccountManager.login(this@LoginActivity, email, password)
            }
            setAuthInProgress(btnLogin, btnRegister, false)

            when (result) {
                is UserAccountManager.AuthResult.Success -> {
                    val message = if (register) R.string.auth_register_success else R.string.auth_login_success
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is UserAccountManager.AuthResult.Error -> {
                    Toast.makeText(this@LoginActivity, result.messageRes, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setAuthInProgress(
        btnLogin: MaterialButton,
        btnRegister: MaterialButton,
        inProgress: Boolean
    ) {
        btnLogin.isEnabled = !inProgress
        btnRegister.isEnabled = !inProgress
        btnLogin.alpha = if (inProgress) 0.6f else 1f
        btnRegister.alpha = if (inProgress) 0.6f else 1f
        findViewById<View>(R.id.etLoginEmail).isEnabled = !inProgress
        findViewById<View>(R.id.etLoginPassword).isEnabled = !inProgress
    }
}
