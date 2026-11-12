package dev.personal.app.data

import dev.personal.contracts.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.net.URI

interface SessionStore {
    fun read(): Tokens?
    fun write(tokens: Tokens)
    fun clear()
}

object EndpointPolicy {
    fun normalize(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.rawPath.orEmpty() in setOf("", "/")) {
            "Enter an HTTPS origin, for example https://devices.example.com"
        }
        return uri.toString().trimEnd('/') + "/"
    }
}

interface DeviceRepository {
    fun hasSession(): Boolean
    suspend fun signIn(credentials: Credentials, register: Boolean)
    suspend fun devices(): List<Device>
    suspend fun enroll(input: DeviceInput, pairingCode: String = ""): Device
    suspend fun synchronize(id: String, input: DeviceInput): Device
    suspend fun pairing(installationId: String): PairingCode
    suspend fun revoke(id: String)
    suspend fun logout()
    suspend fun deleteAccount()
}

class RemoteDeviceRepository(private val api: IdentityApi, private val sessions: SessionStore) : DeviceRepository {
    private val mutex = Mutex()
    override fun hasSession() = sessions.read() != null
    override suspend fun signIn(credentials: Credentials, register: Boolean) {
        sessions.write(if (register) api.register(credentials) else api.login(credentials))
    }
    override suspend fun devices() = authorized { api.devices(it) }
    override suspend fun enroll(input: DeviceInput, pairingCode: String): Device = authorized {
        if (pairingCode.isBlank()) api.enroll(it, input) else api.redeem(it, RedeemPairing(pairingCode.trim(), input))
    }
    override suspend fun synchronize(id: String, input: DeviceInput) = authorized { api.heartbeat(it, id, input) }
    override suspend fun pairing(installationId: String) = authorized { api.pairing(it, PairingInput(installationId.trim())) }
    override suspend fun revoke(id: String) = authorized { api.revoke(it, id) }
    override suspend fun deleteAccount() { authorized { api.deleteAccount(it) }; sessions.clear() }
    override suspend fun logout() {
        try { if (hasSession()) authorized { api.logout(it) } } finally { sessions.clear() }
    }
    private suspend fun <T> authorized(block: suspend (String) -> T): T {
        val original = sessions.read() ?: error("Sign in again.")
        try { return block("Bearer ${original.accessToken}") }
        catch (failure: HttpException) {
            if (failure.code() != 401) throw failure
            mutex.withLock {
                val latest = sessions.read() ?: error("Sign in again.")
                if (latest.accessToken == original.accessToken) {
                    try { sessions.write(api.refresh(RefreshRequest(latest.refreshToken))) }
                    catch (refreshFailure: HttpException) {
                        if (refreshFailure.code() in setOf(401, 403)) sessions.clear()
                        throw refreshFailure
                    }
                }
            }
            val renewed = sessions.read() ?: error("Sign in again.")
            return block("Bearer ${renewed.accessToken}")
        }
    }
}

fun userMessage(failure: Throwable): String = when (failure) {
    is HttpException -> {
        val text = runCatching { failure.response()?.errorBody()?.string() }.getOrNull()
        runCatching { Json.decodeFromString<ApiError>(text.orEmpty()).message }.getOrDefault("Server request failed (${failure.code()}).")
    }
    is java.io.IOException -> "Cannot reach the server securely. Check connectivity, DNS and the server certificate."
    is IllegalArgumentException -> failure.message ?: "Check the entered values."
    else -> "Unable to complete the operation. Please sign in again if the session has expired."
}