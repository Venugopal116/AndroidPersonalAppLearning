package dev.personal.backend

import dev.personal.contracts.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.sql.SQLException

fun main() {
    fun required(name: String) = requireNotNull(System.getenv(name)?.takeIf(String::isNotBlank)) { "$name is required" }
    val invite = required("REGISTRATION_CODE")
    require(invite.length >= 24 && !invite.startsWith("replace-")) { "Set a random REGISTRATION_CODE of at least 24 characters" }
    val password = required("DATABASE_PASSWORD")
    require(password.length >= 24 && !password.startsWith("replace-")) { "Set a random DATABASE_PASSWORD of at least 24 characters" }
    val database = Database(required("DATABASE_URL"), required("DATABASE_USER"), password)
    val service = IdentityService(database, invite)
    embeddedServer(Netty, host = System.getenv("BIND_HOST") ?: "127.0.0.1", port = 8080) {
        foundation(service)
    }.start(wait = true)
}

fun Application.foundation(service: IdentityService, limiter: RateLimiter = RateLimiter()) {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<ApiFailure> { call, failure ->
            if (failure.status == 429) call.response.headers.append(HttpHeaders.RetryAfter, "60")
            call.respond(HttpStatusCode.fromValue(failure.status), ApiError(failure.code, failure.message))
        }
        exception<SerializationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ApiError("invalid_json", "Invalid JSON body.")) }
        exception<SQLException> { call, failure ->
            if (failure.sqlState == "23505") call.respond(HttpStatusCode.Conflict, ApiError("conflict", "Account or installation already exists."))
            else {
                call.application.environment.log.error("Database operation failed")
                call.respond(HttpStatusCode.InternalServerError, ApiError("internal", "Unable to complete the request."))
            }
        }
        exception<Throwable> { call, failure ->
            if (failure is CancellationException) throw failure
            call.application.environment.log.error("Request failed: ${failure::class.simpleName}")
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal", "Unable to complete the request."))
        }
    }
    install(createApplicationPlugin("FoundationGuard") {
        onCall { call ->
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")
            call.response.headers.append("X-Content-Type-Options", "nosniff")
            val host = call.request.local.remoteHost
            limiter.check("all:$host", 120)
            if (call.request.path() in setOf("/v1/auth/register", "/v1/auth/login", "/v1/auth/refresh", "/v1/pairings/redeem")) {
                limiter.check("sensitive:$host", 10)
            }
        }
    })
    routing {
        get("/health") { call.respond(Health("ok")) }
        get("/ready") { withContext(Dispatchers.IO) { call.respond(service.readiness()) } }
        route("/v1") {
            post("/auth/register") { withContext(Dispatchers.IO) { call.respond(HttpStatusCode.Created, service.register(call.body())) } }
            post("/auth/login") { withContext(Dispatchers.IO) { call.respond(service.login(call.body())) } }
            post("/auth/refresh") { withContext(Dispatchers.IO) { call.respond(service.refresh(call.body<RefreshRequest>().refreshToken)) } }
            post("/auth/logout") { withContext(Dispatchers.IO) { service.logout(call.identity(service)); call.respond(HttpStatusCode.NoContent) } }
            get("/devices") { withContext(Dispatchers.IO) { call.respond(service.devices(call.identity(service))) } }
            post("/devices") { withContext(Dispatchers.IO) { call.respond(HttpStatusCode.Created, service.registerDevice(call.identity(service), call.body())) } }
            post("/devices/{id}/heartbeat") { withContext(Dispatchers.IO) {
                call.respond(service.heartbeat(call.identity(service), requireNotNull(call.parameters["id"]), call.body()))
            } }
            post("/devices/{id}/revoke") { withContext(Dispatchers.IO) {
                service.revoke(call.identity(service), requireNotNull(call.parameters["id"])); call.respond(HttpStatusCode.NoContent)
            } }
            post("/pairings") { withContext(Dispatchers.IO) { call.respond(HttpStatusCode.Created, service.pairing(call.identity(service), call.body())) } }
            post("/pairings/redeem") { withContext(Dispatchers.IO) { call.respond(HttpStatusCode.Created, service.redeem(call.identity(service), call.body())) } }
            delete("/me") { withContext(Dispatchers.IO) { service.deleteAccount(call.identity(service)); call.respond(HttpStatusCode.NoContent) } }
        }
    }
}

private fun ApplicationCall.identity(service: IdentityService): Principal {
    val header = request.header(HttpHeaders.Authorization).orEmpty()
    if (!header.startsWith("Bearer ")) throw ApiFailure(401, "unauthorized", "Sign in again.")
    return service.authenticate(header.removePrefix("Bearer "))
}

private suspend inline fun <reified T> ApplicationCall.body(): T {
    if (request.contentType().withoutParameters() != ContentType.Application.Json)
        throw ApiFailure(415, "content_type", "Use application/json.")
    val channel = receiveChannel()
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (true) {
        val count = channel.readAvailable(buffer, 0, buffer.size)
        if (count == -1) break
        if (output.size() + count > 16_384) throw ApiFailure(413, "body_limit", "Request body exceeds 16 KiB.")
        output.write(buffer, 0, count)
    }
    return Json.decodeFromString(output.toString(Charsets.UTF_8.name()))
}