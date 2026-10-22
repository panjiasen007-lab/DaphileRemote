package com.daphile.remote.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts server passwords with an AES/GCM key held by Android Keystore.
 * If the Keystore is unavailable (e.g. on a damaged device), values fall back
 * to a clearly marked local encoding so the user can still connect, but we
 * never silently write plain text without the marker.
 */
class CryptoStore {
    private val alias = "daphile_remote_credentials"

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            "v1:" + base64(cipher.iv) + ":" + base64(encrypted)
        } catch (_: Throwable) {
            "plain:" + base64(value.toByteArray(Charsets.UTF_8))
        }
    }

    fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            when {
                value.startsWith("v1:") -> {
                    val parts = value.split(":")
                    val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                    val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                    String(cipher.doFinal(encrypted), Charsets.UTF_8)
                }
                value.startsWith("plain:") -> String(
                    Base64.decode(value.removePrefix("plain:"), Base64.NO_WRAP),
                    Charsets.UTF_8
                )
                else -> value
            }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun base64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
