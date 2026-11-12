package dev.personal.contracts

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable data class ApiError(val code: String, val message: String)
@Serializable data class Credentials(val email: String, val password: String, val registrationCode: String = "")
@Serializable data class Tokens(val accessToken: String, val refreshToken: String, val expiresAt: Long)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class DeviceInput(
    val installationId: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val batteryPercent: Int?,
    val connectivity: String,
)
@Serializable data class Device(
    val id: String,
    val installationId: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val batteryPercent: Int?,
    val connectivity: String,
    val lastSeenAt: Long,
    val revokedAt: Long? = null,
)
@Serializable data class PairingInput(val installationId: String)
@Serializable data class PairingCode(val code: String, val expiresAt: Long)
@Serializable data class RedeemPairing(val code: String, val device: DeviceInput)
@Serializable data class Health(val status: String)
@Serializable data class Readiness(val status: String, val service: String, val apiVersion: Int)

fun DeviceInput.isValid(): Boolean = validInstallationId(installationId) &&
    listOf(name, manufacturer, model, androidVersion).all { it.isNotBlank() && it.length <= 100 } &&
    (batteryPercent == null || batteryPercent in 0..100) &&
    connectivity in setOf("wifi", "cellular", "ethernet", "other", "offline")

fun validInstallationId(value: String): Boolean = runCatching {
    UUID.fromString(value).toString() == value
}.getOrDefault(false)