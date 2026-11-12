package dev.personal.backend

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class ApiFailure(val status: Int, val code: String, override val message: String) : RuntimeException(message)

object Security {
    private val random = SecureRandom()
    fun token(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
    fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun equal(left: String, right: String): Boolean = MessageDigest.isEqual(
        left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))
    fun password(value: String, salt: ByteArray = ByteArray(16).also(random::nextBytes)): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, 600_000, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        return "600000:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(key)}"
    }
    fun verify(value: String, encoded: String): Boolean {
        val parts = encoded.split(':')
        return parts.size == 3 && parts[0] == "600000" &&
            equal(password(value, Base64.getDecoder().decode(parts[1])), encoded)
    }
}

class RateLimiter(private val now: () -> Long = System::currentTimeMillis) {
    private data class Window(val start: Long, var count: Int)
    private val windows = HashMap<String, Window>()
    @Synchronized fun check(key: String, maximum: Int) {
        val time = now()
        windows.entries.removeIf { time - it.value.start >= 60_000 }
        val window = windows.getOrPut(key) { Window(time, 0) }
        if (++window.count > maximum) throw ApiFailure(429, "rate_limited", "Try again in one minute.")
    }
}