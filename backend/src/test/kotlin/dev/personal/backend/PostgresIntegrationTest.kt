package dev.personal.backend

import dev.personal.contracts.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class PostgresIntegrationTest {
    @Test fun realPostgresPersistsAndRevokesSessions() {
        val url = System.getenv("POSTGRES_TEST_URL")
        assumeTrue("Set POSTGRES_TEST_URL to a disposable PostgreSQL database", !url.isNullOrBlank())
        val database = Database(requireNotNull(url), requireNotNull(System.getenv("POSTGRES_TEST_USER")), requireNotNull(System.getenv("POSTGRES_TEST_PASSWORD")))
        val service = IdentityService(database, "integration-invite")
        val tokens = service.register(Credentials("${UUID.randomUUID()}@example.com", "integration test password", "integration-invite"))
        val owner = service.authenticate(tokens.accessToken)
        try {
            val input = DeviceInput(UUID.randomUUID().toString(), "Test", "Vivo", "V27", "13", null, "offline")
            val device = service.registerDevice(owner, input)
            assertEquals(device.id, service.devices(owner).single().id)
            service.revoke(owner, device.id)
            try { service.authenticate(tokens.accessToken); fail("Revoked session accepted") }
            catch (failure: ApiFailure) { assertEquals(401, failure.status) }
        } finally { service.deleteAccount(owner) }
    }
}