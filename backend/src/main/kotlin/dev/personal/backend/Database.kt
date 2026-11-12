package dev.personal.backend

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class Database(private val url: String, private val user: String, private val password: String) {
    init {
        transaction {
            val ddl = requireNotNull(Database::class.java.getResource("/db/V001__identity.sql")).readText()
            ddl.split(';').filter(String::isNotBlank).forEach { sql -> createStatement().use { it.execute(sql) } }
        }
    }
    @Synchronized fun <T> transaction(block: Connection.() -> T): T =
        DriverManager.getConnection(url, user, password).use { connection ->
            connection.autoCommit = false
            try {
                val value = connection.block()
                connection.commit()
                value
            } catch (failure: Throwable) {
                connection.rollback()
                throw failure
            }
        }
}

fun Connection.execute(sql: String, vararg values: Any?): Int = prepareStatement(sql).use { statement ->
    values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
    statement.executeUpdate()
}
fun <T> Connection.rows(sql: String, vararg values: Any?, map: (ResultSet) -> T): List<T> =
    prepareStatement(sql).use { statement ->
        values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
        statement.executeQuery().use { result -> buildList { while (result.next()) add(map(result)) } }
    }