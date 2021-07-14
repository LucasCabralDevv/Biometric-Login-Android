package com.lucascabral.biometriclogin.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.lucascabral.biometriclogin.FailedLoginFormState
import com.lucascabral.biometriclogin.ui.viewmodel.LoginViewModel
import com.lucascabral.biometriclogin.SampleAppUser
import com.lucascabral.biometriclogin.SuccessfulLoginFormState
import com.lucascabral.biometriclogin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginWithPasswordViewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupForLoginWithPassword()
    }

    private fun setupForLoginWithPassword() {
        loginWithPasswordViewModel.loginWithPasswordFormState.observe(this, Observer { formState ->
            val loginState = formState ?: return@Observer
            when (loginState) {
                is SuccessfulLoginFormState -> binding.mainLoginButton.isEnabled =
                    loginState.isDataValid
                is FailedLoginFormState -> {
                    loginState.usernameError?.let {
                        binding.mainUsernameEditText.error = getString(it)
                    }
                    loginState.passwordError?.let {
                        binding.mainPasswordEditText.error = getString(it)
                    }
                }
            }
        })
        loginWithPasswordViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer
            if (loginResult.success) {
                updateApp(
                    "You successfully signed up using password as: user " +
                            "${SampleAppUser.username} with fake token ${SampleAppUser.fakeToken}"
                )
            }
        })
        binding.mainUsernameEditText.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.mainUsernameEditText.text.toString(),
                binding.mainPasswordEditText.text.toString()
            )
        }
        binding.mainPasswordEditText.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.mainUsernameEditText.text.toString(),
                binding.mainPasswordEditText.text.toString()
            )
        }
        binding.mainPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE ->
                    loginWithPasswordViewModel.login(
                        binding.mainUsernameEditText.text.toString(),
                        binding.mainPasswordEditText.text.toString()
                    )
            }
            false
        }
        binding.mainLoginButton.setOnClickListener {
            loginWithPasswordViewModel.login(
                binding.mainUsernameEditText.text.toString(),
                binding.mainPasswordEditText.text.toString()
            )
        }
        Log.d(
            "LoginActivity",
            "Username ${SampleAppUser.username}; fake token ${SampleAppUser.fakeToken}"
        )
    }

    private fun updateApp(successMsg: String) {
        binding.mainSuccessTextView.text = successMsg
    }
}