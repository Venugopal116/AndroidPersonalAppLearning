package dev.personal.backend

import dev.personal.contracts.*
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale
import java.util.UUID

data class Principal(val sessionId: String, val userId: String, val deviceId: String?)

class IdentityService(
    private val database: Database,
    private val registrationCode: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dummyHash = Security.password(Security.token())
    fun readiness(): Readiness {
        try {
            database.transaction {
                if (rows("SELECT 1") { it.getInt(1) }.single() != 1)
                    throw ApiFailure(503, "not_ready", "The database is not ready.")
            }
        } catch (_: SQLException) {
            throw ApiFailure(503, "not_ready", "The database is not ready.")
        }
        return Readiness("ready", "personal-device-lab", 1)
    }
    private fun id() = UUID.randomUUID().toString()
    private fun reject(status: Int, code: String, text: String): Nothing = throw ApiFailure(status, code, text)
    private fun email(value: String): String = value.trim().lowercase(Locale.ROOT).also {
        if (it.length !in 3..254 || !it.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
            reject(400, "invalid_email", "Enter a valid email address.")
    }
    fun register(input: Credentials): Tokens {
        if (!Security.equal(input.registrationCode, registrationCode)) reject(403, "registration_denied", "Registration is not authorized.")
        val normalized = email(input.email)
        if (input.password.length !in 12..128) reject(400, "invalid_password", "Use a password of 12 to 128 characters.")
        val passwordHash = Security.password(input.password)
        return database.transaction {
            val userId = id()
            execute("INSERT INTO users VALUES (?, ?, ?, ?)", userId, normalized, passwordHash, now())
            audit(userId, "account_created")
            issue(userId, null)
        }
    }
    fun login(input: Credentials): Tokens {
        val normalized = email(input.email)
        if (input.password.length > 128) reject(401, "invalid_credentials", "Incorrect email or password.")
        val user = database.transaction {
            rows("SELECT id, password_hash FROM users WHERE email = ?", normalized) {
                it.getString("id") to it.getString("password_hash")
            }.singleOrNull()
        }
        val valid = Security.verify(input.password, user?.second ?: dummyHash)
        if (user == null || !valid) reject(401, "invalid_credentials", "Incorrect email or password.")
        return database.transaction { audit(user.first, "login"); issue(user.first, null) }
    }
    fun authenticate(token: String): Principal = database.transaction {
        if (token.length !in 40..100) reject(401, "unauthorized", "Sign in again.")
        rows("SELECT id, user_id, device_id FROM sessions WHERE access_hash = ? AND access_expires_at > ?", Security.hash(token), now()) {
            Principal(it.getString("id"), it.getString("user_id"), it.getString("device_id"))
        }.singleOrNull()?.also { ensureActive(it.deviceId) } ?: reject(401, "unauthorized", "Sign in again.")
    }
    fun refresh(token: String): Tokens = database.transaction {
        if (token.length !in 40..100) reject(401, "unauthorized", "Sign in again.")
        val principal = rows("SELECT id, user_id, device_id FROM sessions WHERE refresh_hash = ? AND refresh_expires_at > ?", Security.hash(token), now()) {
            Principal(it.getString("id"), it.getString("user_id"), it.getString("device_id"))
        }.singleOrNull() ?: reject(401, "unauthorized", "Sign in again.")
        ensureActive(principal.deviceId)
        execute("DELETE FROM sessions WHERE id = ?", principal.sessionId)
        issue(principal.userId, principal.deviceId)
    }
    fun logout(principal: Principal) = database.transaction {
        execute("DELETE FROM sessions WHERE id = ?", principal.sessionId)
        audit(principal.userId, "logout")
    }
    fun devices(principal: Principal): List<Device> = database.transaction {
        rows("SELECT * FROM devices WHERE user_id = ? ORDER BY last_seen_at DESC", principal.userId, map = ::device)
    }
    fun registerDevice(principal: Principal, input: DeviceInput): Device = database.transaction {
        enroll(principal, input)
    }
    fun heartbeat(principal: Principal, deviceId: String, input: DeviceInput): Device = database.transaction {
        if (!input.isValid()) reject(400, "invalid_device", "Invalid device metadata.")
        if (principal.deviceId != deviceId) reject(403, "device_scope", "This session cannot synchronize that device.")
        val current = owned(principal, deviceId)
        ensureActive(deviceId)
        if (current.installationId != input.installationId) reject(403, "device_scope", "Installation does not match.")
        execute("UPDATE devices SET name=?, manufacturer=?, model=?, android_version=?, battery_percent=?, connectivity=?, last_seen_at=? WHERE id=?",
            input.name, input.manufacturer, input.model, input.androidVersion, input.batteryPercent, input.connectivity, now(), deviceId)
        owned(principal, deviceId)
    }
    fun revoke(principal: Principal, deviceId: String) = database.transaction {
        owned(principal, deviceId)
        execute("UPDATE devices SET revoked_at = ? WHERE id = ?", now(), deviceId)
        execute("DELETE FROM sessions WHERE device_id = ?", deviceId)
        audit(principal.userId, "device_revoked")
    }
    fun pairing(principal: Principal, input: PairingInput): PairingCode = database.transaction {
        if (!validInstallationId(input.installationId)) reject(400, "invalid_installation", "Enter the other device's installation ID.")
        execute("DELETE FROM pairings WHERE expires_at <= ? OR consumed_at IS NOT NULL", now())
        execute("DELETE FROM pairings WHERE user_id = ? AND installation_id = ?", principal.userId, input.installationId)
        val count = rows("SELECT COUNT(*) FROM pairings WHERE user_id = ?", principal.userId) { it.getInt(1) }.single()
        if (count >= 5) reject(429, "pairing_limit", "Finish or wait for existing pairing codes to expire.")
        val code = Security.token()
        val expires = now() + 300_000
        execute("INSERT INTO pairings VALUES (?, ?, ?, ?, NULL)", Security.hash(code), principal.userId, input.installationId, expires)
        audit(principal.userId, "pairing_created")
        PairingCode(code, expires)
    }
    fun redeem(principal: Principal, input: RedeemPairing): Device = database.transaction {
        if (input.code.length !in 40..100 || !input.device.isValid()) reject(400, "invalid_pairing", "Invalid pairing request.")
        val changed = execute("UPDATE pairings SET consumed_at = ? WHERE code_hash = ? AND user_id = ? AND installation_id = ? AND expires_at > ? AND consumed_at IS NULL",
            now(), Security.hash(input.code), principal.userId, input.device.installationId, now())
        if (changed != 1) reject(400, "invalid_pairing", "Code is expired, used or does not match this installation/account.")
        enroll(principal, input.device).also { audit(principal.userId, "pairing_redeemed") }
    }
    fun deleteAccount(principal: Principal) = database.transaction {
        execute("DELETE FROM users WHERE id = ?", principal.userId)
    }
    private fun Connection.enroll(principal: Principal, input: DeviceInput): Device {
        if (!input.isValid()) reject(400, "invalid_device", "Invalid device metadata.")
        val existing = rows("SELECT * FROM devices WHERE installation_id = ?", input.installationId, map = ::device).singleOrNull()
        val deviceId = if (existing != null) {
            owned(principal, existing.id)
            ensureActive(existing.id)
            existing.id
        } else {
            val value = id()
            execute("INSERT INTO devices VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)", value, principal.userId,
                input.installationId, input.name, input.manufacturer, input.model, input.androidVersion, input.batteryPercent, input.connectivity, now())
            audit(principal.userId, "device_registered")
            value
        }
        if (principal.deviceId != null && principal.deviceId != deviceId) reject(403, "device_scope", "Use a new sign-in session to register another installation.")
        execute("UPDATE sessions SET device_id = ? WHERE id = ?", deviceId, principal.sessionId)
        return owned(principal, deviceId)
    }
    private fun Connection.owned(principal: Principal, deviceId: String): Device =
        rows("SELECT * FROM devices WHERE id = ? AND user_id = ?", deviceId, principal.userId, map = ::device).singleOrNull()
            ?: reject(404, "not_found", "Device not found.")
    private fun Connection.ensureActive(deviceId: String?) {
        if (deviceId == null) return
        val active = rows("SELECT id FROM devices WHERE id = ? AND revoked_at IS NULL", deviceId) { it.getString(1) }
        if (active.isEmpty()) reject(401, "revoked", "This device has been revoked.")
    }
    private fun Connection.issue(userId: String, deviceId: String?): Tokens {
        execute("DELETE FROM sessions WHERE refresh_expires_at <= ?", now())
        val count = rows("SELECT COUNT(*) FROM sessions WHERE user_id = ?", userId) { it.getInt(1) }.single()
        if (count >= 20) reject(429, "session_limit", "Too many active sessions. Sign out an existing session or wait for expiry.")
        val access = Security.token()
        val refresh = Security.token()
        val expiry = now() + 600_000
        execute("INSERT INTO sessions VALUES (?, ?, ?, ?, ?, ?, ?)", id(), userId, deviceId, Security.hash(access), Security.hash(refresh), expiry, now() + 604_800_000)
        return Tokens(access, refresh, expiry)
    }
    private fun Connection.audit(userId: String, event: String) {
        execute("DELETE FROM audit_events WHERE created_at < ?", now() - 2_592_000_000L)
        execute("INSERT INTO audit_events VALUES (?, ?, ?, ?)", id(), userId, event, now())
    }
    private fun device(result: ResultSet) = Device(
        result.getString("id"), result.getString("installation_id"), result.getString("name"),
        result.getString("manufacturer"), result.getString("model"), result.getString("android_version"),
        result.getObject("battery_percent")?.let { (it as Number).toInt() }, result.getString("connectivity"),
        result.getLong("last_seen_at"), result.getObject("revoked_at")?.let { (it as Number).toLong() },
    )
}