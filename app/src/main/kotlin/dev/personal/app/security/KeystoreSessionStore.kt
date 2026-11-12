package dev.personal.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.personal.app.data.SessionStore
import dev.personal.contracts.Tokens
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("encrypted_session", Context.MODE_PRIVATE)
    private val alias = "personal_device_session_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized override fun read(): Tokens? {
        val record = preferences.getString("record", null) ?: return null
        return try {
            val bytes = Base64.decode(record, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
            Json.decodeFromString<Tokens>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
        } catch (_: Exception) { clear(); null }
    }
    @Synchronized override fun write(tokens: Tokens) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
        val bytes = cipher.iv + cipher.doFinal(Json.encodeToString(tokens).toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("record", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()) { "Could not persist session" }
    }
    @Synchronized override fun clear() { check(preferences.edit().clear().commit()) }
}