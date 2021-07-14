package com.lucascabral.biometriclogin.ui.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.lucascabral.biometriclogin.*
import com.lucascabral.biometriclogin.databinding.ActivityEnableBiometricLoginBinding
import com.lucascabral.biometriclogin.ui.viewmodel.LoginViewModel
import com.lucascabral.biometriclogin.utils.BiometricPromptUtils

class EnableBiometricLoginActivity : AppCompatActivity() {

    private val TAG = "EnableBiometricLogin"
    private lateinit var cryptographyManager: CryptographyManager
    private val loginViewModel by viewModels<LoginViewModel>()
    private lateinit var binding: ActivityEnableBiometricLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnableBiometricLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.enableBioCancelButton.setOnClickListener { finish() }

        loginViewModel.loginWithPasswordFormState.observe(this, Observer { formState ->
            val loginState = formState ?: return@Observer
            when (loginState) {
                is SuccessfulLoginFormState -> binding.enableBioAuthorizeButton.isEnabled =
                    loginState.isDataValid
                is FailedLoginFormState -> {
                    loginState.usernameError?.let {
                        binding.enableBioUsernameEditText.error = getString(it)
                    }
                    loginState.passwordError?.let {
                        binding.enableBioPasswordEditText.error = getString(it)
                    }
                }
            }
        })
        loginViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer
            if (loginResult.success) {
                showBiometricPromptForEncryption()
            }
        })
        binding.enableBioUsernameEditText.doAfterTextChanged {
            loginViewModel.onLoginDataChanged(
                binding.enableBioUsernameEditText.text.toString(),
                binding.enableBioPasswordEditText.text.toString()
            )
        }
        binding.enableBioPasswordEditText.doAfterTextChanged {
            loginViewModel.onLoginDataChanged(
                binding.enableBioUsernameEditText.text.toString(),
                binding.enableBioPasswordEditText.text.toString()
            )
        }
        binding.enableBioPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE ->
                    loginViewModel.login(
                        binding.enableBioUsernameEditText.text.toString(),
                        binding.enableBioPasswordEditText.text.toString()
                    )
            }
            false
        }
        binding.enableBioAuthorizeButton.setOnClickListener {
            loginViewModel.login(
                binding.enableBioUsernameEditText.text.toString(),
                binding.enableBioPasswordEditText.text.toString()
            )
        }
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(this, ::encryptAndStoreServerToken)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult) {
        authResult.cryptoObject?.cipher?.apply {
            SampleAppUser.fakeToken?.let { token ->
                Log.d(TAG, "The token from server is $token")
                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper,
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )
            }
        }
        finish()
    }
}