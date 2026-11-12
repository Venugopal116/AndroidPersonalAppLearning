package dev.personal.backend

import dev.personal.contracts.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ApiTest {
    @Test fun registrationAuthenticationValidationAndDeletion() = testApplication {
        val service = IdentityService(Database("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""), "test-invite")
        application { foundation(service) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/devices").status)
        assertEquals(HttpStatusCode.BadRequest, client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json); setBody("{broken")
        }.status)
        val registration = client.post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(Credentials("test@example.com", "a long test password", "test-invite")))
        }
        assertEquals(HttpStatusCode.Created, registration.status)
        val tokens = Json.decodeFromString<Tokens>(registration.bodyAsText())
        val input = DeviceInput(UUID.randomUUID().toString(), "Moto", "Motorola", "Edge", "13", 50, "wifi")
        val enrolled = client.post("/v1/devices") {
            bearerAuth(tokens.accessToken); contentType(ContentType.Application.Json); setBody(Json.encodeToString(input))
        }
        assertEquals(HttpStatusCode.Created, enrolled.status)
        val device = Json.decodeFromString<Device>(enrolled.bodyAsText())
        assertEquals(HttpStatusCode.OK, client.post("/v1/devices/${device.id}/heartbeat") {
            bearerAuth(tokens.accessToken); contentType(ContentType.Application.Json); setBody(Json.encodeToString(input))
        }.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/v1/me") { bearerAuth(tokens.accessToken) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/devices") { bearerAuth(tokens.accessToken) }.status)
    }
    @Test fun oversizedRequestsAndBruteForceAreRejected() = testApplication {
        application { foundation(IdentityService(Database("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""), "invite")) }
        assertEquals(HttpStatusCode.PayloadTooLarge, client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json); setBody("x".repeat(16_385))
        }.status)
        repeat(9) { client.post("/v1/auth/login") { contentType(ContentType.Application.Json); setBody("{}") } }
        assertEquals(HttpStatusCode.TooManyRequests, client.post("/v1/auth/login").status)
    }
}