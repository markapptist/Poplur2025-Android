package com.apptists.poplur2025.prelogin

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Credentials never enter preferences in plaintext. Each decrypt requires the
 * device's biometric check and a user-authenticated Android Keystore key. */
class NativePreloginBiometrics(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences("poplur_native_biometrics", 0)
    private var pending: CancellationSignal? = null
    fun cancel() { pending?.cancel(); pending = null }
    private fun key(create: Boolean): SecretKey? {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        if (!create) return null
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true).build())
        return generator.generateKey()
    }
    fun save(email: String, password: String, finished: () -> Unit) {
        if (Build.VERSION.SDK_INT < 28) { finished(); return }
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(true)) }
            prompt(cipher, "Enable biometric sign-in", { authenticated ->
                runCatching { val encrypted = authenticated.doFinal(password.toByteArray(Charsets.UTF_8)); prefs.edit().putString("email", email).putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).putString("iv", Base64.encodeToString(authenticated.iv, Base64.NO_WRAP)).apply() }
                finished()
            }, { finished() })
        }.onFailure { finished() }
    }
    fun signIn(success: (String, String) -> Unit, error: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 28) { error("Biometric sign-in is unavailable on this device. Use email and password."); return }
        val email = prefs.getString("email", null)
        val encrypted = prefs.getString("ciphertext", null)
        val iv = prefs.getString("iv", null)
        if (email == null || encrypted == null || iv == null) { error("Sign in with email and enable biometric sign-in on this device first."); return }
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(false) ?: kotlin.error("Saved sign-in is unavailable."), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))) }
            prompt(cipher, "Sign in to Poplur", { authenticated ->
                runCatching { String(authenticated.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8) }.onSuccess { success(email, it) }.onFailure { error("Saved sign-in changed. Sign in with email and enable biometrics again.") }
            }, error)
        }.onFailure { error("Saved sign-in changed. Sign in with email and enable biometrics again.") }
    }
    @androidx.annotation.RequiresApi(28)
    private fun prompt(cipher: Cipher, title: String, success: (Cipher) -> Unit, error: (String) -> Unit) {
        cancel(); val signal = CancellationSignal(); pending = signal
        BiometricPrompt.Builder(activity).setTitle(title).setNegativeButton("Cancel", activity.mainExecutor) { _, _ -> error("Biometric sign-in cancelled.") }.build().authenticate(BiometricPrompt.CryptoObject(cipher), signal, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { pending = null; result.cryptoObject?.cipher?.let(success) ?: error("Biometric sign-in was unavailable.") }
            override fun onAuthenticationError(code: Int, message: CharSequence) { pending = null; error(message.toString()) }
        })
    }
    private companion object { const val ALIAS = "poplur.native.prelogin.biometric.v1" }
}
