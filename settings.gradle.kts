pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "androidPersonalAppLearning"
include(":shared", ":backend")
if (!providers.gradleProperty("backendOnly").isPresent) include(":app")