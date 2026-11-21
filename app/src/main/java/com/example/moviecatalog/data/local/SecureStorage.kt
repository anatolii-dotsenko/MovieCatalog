package com.example.moviecatalog.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ISecureStorage {
    suspend fun setAsync(key: String, value: String)
    suspend fun getAsync(key: String): String?
    suspend fun remove(key: String)
    suspend fun clear()
}

/**
 * Encrypted key-value storage using AES/GCM/NoPadding backed by AndroidKeyStore.
 * Used for sensitive authentication tokens (id_token, access_token, refresh_token).
 */
class SecureStorage(context: Context) : ISecureStorage {

    private val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun setAsync(key: String, value: String) {
        withContext(Dispatchers.IO) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val encodedPayload = Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            prefs.edit().putString(key, encodedPayload).apply()
        }
    }

    override suspend fun getAsync(key: String): String? = withContext(Dispatchers.IO) {
        val raw = prefs.getString(key, null) ?: return@withContext null
        runCatching {
            val parts = raw.split(':', limit = 2)
            if (parts.size != 2) return@runCatching null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrElse {
            prefs.edit().remove(key).apply()
            null
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            prefs.edit().remove(key).apply()
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            prefs.edit().clear().apply()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "movie_catalog_auth_aes_key"
        const val SECURE_PREFS_NAME = "movie_catalog_secure_tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
    }
}
