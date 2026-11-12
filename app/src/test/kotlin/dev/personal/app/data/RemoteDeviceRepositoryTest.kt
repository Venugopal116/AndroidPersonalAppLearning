package dev.personal.app.data

import dev.personal.contracts.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteDeviceRepositoryTest {
    private class MemorySessions(var tokens: Tokens?) : SessionStore {
        override fun read() = tokens
        override fun write(tokens: Tokens) { this.tokens = tokens }
        override fun clear() { tokens = null }
    }
    private fun api(server: MockWebServer) = Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType())).build().create(IdentityApi::class.java)
    @Test fun expiredAccessTokenRefreshesOnceAndRetries() = runTest {
        MockWebServer().use { server ->
            val sessions = MemorySessions(Tokens("old-access", "old-refresh", 0))
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(Json.encodeToString(Tokens("new-access", "new-refresh", 100))))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("[]"))
            assertTrue(RemoteDeviceRepository(api(server), sessions).devices().isEmpty())
            assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
            val refresh = server.takeRequest()
            assertEquals("/v1/auth/refresh", refresh.path)
            assertFalse(refresh.headers.names().contains("Authorization"))
            assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
            assertEquals("new-refresh", sessions.read()?.refreshToken)
        }
    }
    @Test fun revokedRefreshClearsLocalSession() = runTest {
        MockWebServer().use { server ->
            val sessions = MemorySessions(Tokens("old", "refresh", 0))
            repeat(2) { server.enqueue(MockResponse().setResponseCode(401).setBody("{}")) }
            try { RemoteDeviceRepository(api(server), sessions).devices(); fail("Expected unauthorized") }
            catch (_: retrofit2.HttpException) { assertNull(sessions.read()) }
        }
    }
    @Test fun logoutClearsLocalTokensEvenIfServerFails() = runTest {
        MockWebServer().use { server ->
            val sessions = MemorySessions(Tokens("access", "refresh", 0))
            server.enqueue(MockResponse().setResponseCode(503))
            try { RemoteDeviceRepository(api(server), sessions).logout(); fail("Expected unavailable") }
            catch (_: retrofit2.HttpException) { assertNull(sessions.read()) }
        }
    }
    @Test fun endpointPolicyRequiresHttpsOrigin() {
        assertEquals("https://devices.example.com/", EndpointPolicy.normalize("https://devices.example.com"))
        listOf("http://example.com", "https://user:pass@example.com", "https://example.com/api", "https://example.com?token=secret").forEach {
            try { EndpointPolicy.normalize(it); fail("Accepted unsafe origin") } catch (_: IllegalArgumentException) { }
        }
    }
}