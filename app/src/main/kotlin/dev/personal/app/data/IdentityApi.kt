package dev.personal.app.data

import dev.personal.contracts.*
import retrofit2.http.*

interface IdentityApi {
    @POST("v1/auth/register") suspend fun register(@Body body: Credentials): Tokens
    @POST("v1/auth/login") suspend fun login(@Body body: Credentials): Tokens
    @POST("v1/auth/refresh") suspend fun refresh(@Body body: RefreshRequest): Tokens
    @POST("v1/auth/logout") suspend fun logout(@Header("Authorization") authorization: String)
    @GET("v1/devices") suspend fun devices(@Header("Authorization") authorization: String): List<Device>
    @POST("v1/devices") suspend fun enroll(@Header("Authorization") authorization: String, @Body body: DeviceInput): Device
    @POST("v1/devices/{id}/heartbeat") suspend fun heartbeat(@Header("Authorization") authorization: String, @Path("id") id: String, @Body body: DeviceInput): Device
    @POST("v1/devices/{id}/revoke") suspend fun revoke(@Header("Authorization") authorization: String, @Path("id") id: String)
    @POST("v1/pairings") suspend fun pairing(@Header("Authorization") authorization: String, @Body body: PairingInput): PairingCode
    @POST("v1/pairings/redeem") suspend fun redeem(@Header("Authorization") authorization: String, @Body body: RedeemPairing): Device
    @DELETE("v1/me") suspend fun deleteAccount(@Header("Authorization") authorization: String)
}