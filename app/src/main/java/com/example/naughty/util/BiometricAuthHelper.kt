package com.example.naughty.util

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

object BiometricAuthHelper {

    fun canAuthenticate(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricStatus.AVAILABLE
        }

        // Check KeyguardManager for device PIN/pattern/password as fallback
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true
        if (isDeviceSecure) {
            return BiometricStatus.AVAILABLE
        }

        return when (canAuth) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                if (isDeviceSecure) BiometricStatus.AVAILABLE else BiometricStatus.NOT_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                if (isDeviceSecure) BiometricStatus.AVAILABLE else BiometricStatus.UNAVAILABLE
            }
            else -> BiometricStatus.NOT_ENROLLED
        }
    }

    fun openSecuritySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Note",
        subtitle: String = "Authenticate using biometric or device lock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onCancel()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object NoteLockSession {
    private val unlockedNoteIds = mutableSetOf<String>()

    fun unlock(noteId: String) {
        synchronized(unlockedNoteIds) {
            unlockedNoteIds.add(noteId)
        }
    }

    fun lock(noteId: String) {
        synchronized(unlockedNoteIds) {
            unlockedNoteIds.remove(noteId)
        }
    }

    fun isUnlocked(noteId: String): Boolean = synchronized(unlockedNoteIds) {
        unlockedNoteIds.contains(noteId)
    }

    fun clear() {
        synchronized(unlockedNoteIds) {
            unlockedNoteIds.clear()
        }
    }
}

