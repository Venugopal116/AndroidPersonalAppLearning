package dev.personal.app.security

import androidx.test.platform.app.InstrumentationRegistry
import dev.personal.contracts.Tokens
import org.junit.Assert.*
import org.junit.Test

class KeystoreSessionStoreTest {
    @Test fun encryptsPersistsAndClearsSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = KeystoreSessionStore(context)
        store.clear()
        val tokens = Tokens("test-access-sensitive", "test-refresh-sensitive", 123)
        try {
            store.write(tokens)
            assertEquals(tokens, KeystoreSessionStore(context).read())
            val raw = context.getSharedPreferences("encrypted_session", 0).getString("record", "").orEmpty()
            assertFalse(raw.contains(tokens.accessToken))
            assertFalse(raw.contains(tokens.refreshToken))
            store.clear()
            assertNull(store.read())
        } finally { store.clear() }
    }
    @Test fun tamperedCiphertextFailsClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = KeystoreSessionStore(context)
        store.write(Tokens("test", "refresh", 0))
        context.getSharedPreferences("encrypted_session", 0).edit().putString("record", "tampered").commit()
        assertNull(store.read())
    }
}