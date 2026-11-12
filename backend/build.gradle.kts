plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jib)
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("dev.personal.backend.MainKt") }
jib {
    from {
        image = "eclipse-temurin:17-jre-jammy"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
        }
    }
    to { image = "personal-device-backend:0.1.0" }
    container {
        mainClass = "dev.personal.backend.MainKt"
        user = "10001:10001"
        ports = listOf("8080")
        workingDirectory = "/app"
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.core)
    implementation(libs.ktor.netty)
    implementation(libs.ktor.content)
    implementation(libs.ktor.json)
    implementation(libs.ktor.status)
    implementation(libs.coroutines.core)
    runtimeOnly(libs.postgres)
    runtimeOnly(libs.logback)
    testImplementation(libs.h2)
    testImplementation(libs.ktor.test)
    testImplementation(libs.junit)
}