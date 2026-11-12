plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("dev.personal.backend.MainKt") }
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