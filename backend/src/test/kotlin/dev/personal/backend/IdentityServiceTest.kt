package dev.personal.backend

import dev.personal.contracts.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class IdentityServiceTest {
    private var time = 1_800_000_000_000L
    private val database = Database("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")
    private val service = IdentityService(database, "test-registration-code-only", { time })
    private val credentials = Credentials("owner@example.com", "correct horse battery staple", "test-registration-code-only")
    private fun phone() = DeviceInput(UUID.randomUUID().toString(), "Personal phone", "Motorola", "Edge", "13", 70, "wifi")
    private fun principal(tokens: Tokens) = service.authenticate(tokens.accessToken)
    private fun fails(status: Int, block: () -> Unit) {
        try { block(); fail("Expected failure $status") } catch (failure: ApiFailure) { assertEquals(status, failure.status) }
    }
    @Test fun loginNormalizesEmailAndRejectsBadCredentials() {
        service.register(credentials)
        assertNotNull(principal(service.login(credentials.copy(email = " OWNER@example.com "))))
        fails(401) { service.login(credentials.copy(password = "incorrect")) }
        fails(403) { service.register(credentials.copy(email = "other@example.com", registrationCode = "wrong")) }
    }
    @Test fun refreshRotatesAndLogoutInvalidates() {
        val first = service.register(credentials)
        val second = service.refresh(first.refreshToken)
        fails(401) { principal(first) }
        fails(401) { service.refresh(first.refreshToken) }
        service.logout(principal(second))
        fails(401) { principal(second) }
        fails(401) { service.refresh(second.refreshToken) }
    }
    @Test fun expiredTokensCannotAuthenticate() {
        val tokens = service.register(credentials)
        time += 600_001
        fails(401) { principal(tokens) }
        assertNotNull(principal(service.refresh(tokens.refreshToken)))
        time += 604_800_001
        fails(401) { service.refresh(tokens.refreshToken) }
    }
    @Test fun ownershipRevocationAndDeviceScopedHeartbeat() {
        val tokens = service.register(credentials)
        val owner = principal(tokens)
        val input = phone()
        val device = service.registerDevice(owner, input)
        val second = service.registerDevice(principal(service.login(credentials)), phone())
        fails(403) { service.heartbeat(principal(tokens), second.id, input) }
        assertEquals(42, service.heartbeat(principal(tokens), device.id, input.copy(batteryPercent = 42)).batteryPercent)
        val other = principal(service.register(credentials.copy(email = "other@example.com")))
        assertTrue(service.devices(other).isEmpty())
        fails(404) { service.revoke(other, device.id) }
        service.revoke(owner, device.id)
        fails(401) { principal(tokens) }
        fails(401) { service.refresh(tokens.refreshToken) }
        fails(401) { service.registerDevice(principal(service.login(credentials)), input) }
    }
    @Test fun pairingIsInstallationBoundSingleUseAndExpires() {
        val owner = principal(service.register(credentials))
        val input = phone()
        val pairing = service.pairing(owner, PairingInput(input.installationId))
        fails(400) { service.redeem(owner, RedeemPairing(pairing.code, phone())) }
        assertEquals(input.installationId, service.redeem(owner, RedeemPairing(pairing.code, input)).installationId)
        fails(400) { service.redeem(owner, RedeemPairing(pairing.code, input)) }
        val expired = service.pairing(owner, PairingInput(input.installationId))
        time += 300_001
        fails(400) { service.redeem(owner, RedeemPairing(expired.code, input)) }
    }
    @Test fun pairingCannotCrossAccounts() {
        val owner = principal(service.register(credentials))
        val other = principal(service.register(credentials.copy(email = "other@example.com")))
        val input = phone()
        val pairing = service.pairing(owner, PairingInput(input.installationId))
        fails(400) { service.redeem(other, RedeemPairing(pairing.code, input)) }
        assertNotNull(service.redeem(owner, RedeemPairing(pairing.code, input)))
    }
    @Test fun deletionCascadesAndAllowsCleanRegistration() {
        val tokens = service.register(credentials)
        val owner = principal(tokens)
        service.registerDevice(owner, phone())
        service.pairing(owner, PairingInput(UUID.randomUUID().toString()))
        service.deleteAccount(owner)
        fails(401) { principal(tokens) }
        assertTrue(service.devices(principal(service.register(credentials))).isEmpty())
    }
    @Test fun storedTokensAndPasswordsAreNotPlaintext() {
        val tokens = service.register(credentials)
        database.transaction {
            val hashes = rows("SELECT access_hash, refresh_hash FROM sessions") { it.getString(1) to it.getString(2) }.single()
            assertEquals(Security.hash(tokens.accessToken), hashes.first)
            assertEquals(Security.hash(tokens.refreshToken), hashes.second)
            val password = rows("SELECT password_hash FROM users") { it.getString(1) }.single()
            assertNotEquals(credentials.password, password)
            assertTrue(Security.verify(credentials.password, password))
        }
    }
    @Test fun rateLimitResets() {
        val limiter = RateLimiter { time }
        repeat(2) { limiter.check("login", 2) }
        fails(429) { limiter.check("login", 2) }
        time += 60_000
        limiter.check("login", 2)
    }
}