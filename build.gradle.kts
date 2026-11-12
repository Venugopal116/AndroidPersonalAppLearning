tasks.register("verifyFoundation") {
    dependsOn(":shared:test", ":backend:test")
}