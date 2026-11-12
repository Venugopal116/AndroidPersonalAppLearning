package dev.personal.app.data

import dev.personal.contracts.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface IdentityApi {
    @GET("ready") suspend fun readiness(): Readiness
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

private val apiClient by lazy {
    OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS).build()
}

fun createIdentityApi(origin: String): IdentityApi = Retrofit.Builder()
    .baseUrl(EndpointPolicy.normalize(origin)).client(apiClient)
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .build().create(IdentityApi::class.java)

class ServerConnector(private val apiFactory: (String) -> IdentityApi = ::createIdentityApi) {
    suspend fun verify(origin: String): String {
        val normalized = EndpointPolicy.normalize(origin)
        val readiness = apiFactory(normalized).readiness()
        require(readiness.service == "personal-device-lab" && readiness.apiVersion == 1) {
            "This address does not provide a compatible Personal Device Lab API."
        }
        require(readiness.status == "ready") { "The server is not ready. Try again after checking its database." }
        return normalized
    }
}