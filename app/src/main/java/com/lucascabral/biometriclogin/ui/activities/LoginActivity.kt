package com.lucascabral.biometriclogin.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.lucascabral.biometriclogin.*
import com.lucascabral.biometriclogin.ui.viewmodel.LoginViewModel
import com.lucascabral.biometriclogin.databinding.ActivityLoginBinding
import com.lucascabral.biometriclogin.utils.BiometricPromptUtils

class LoginActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )

    private lateinit var binding: ActivityLoginBinding
    private val loginWithPasswordViewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.mainUseBiometricsTextView.visibility = View.VISIBLE
            binding.mainUseBiometricsTextView.setOnClickListener {
                if (ciphertextWrapper != null) {
                    showBiometricPromptForDecryption()
                } else {
                    startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
                }
            }
        } else {
            binding.mainUseBiometricsTextView.visibility = View.INVISIBLE
        }

        if (ciphertextWrapper == null) {
            setupForLoginWithPassword()
        }
    }

    /**
     * The logic is kept inside onResume instead of onCreate so that authorizing biometrics takes
     * immediate effect.
     */
    override fun onResume() {
        super.onResume()

        if (ciphertextWrapper != null) {
            if (SampleAppUser.fakeToken == null) {
                showBiometricPromptForDecryption()
            } else {
                // The user has already logged in, so proceed to the rest of the app
                // this is a todo for you, the developer
                updateApp(getString(R.string.already_signedin))
            }
        }
    }

    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = getString(R.string.secret_key_name)
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initializationVector
            )
            biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(
                    this,
                    ::decryptServerTokenFromStorage
                )
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext =
                    cryptographyManager.decryptData(textWrapper.ciphertext, it)
                SampleAppUser.fakeToken = plaintext
                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.

                updateApp(getString(R.string.already_signedin))
            }
        }
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