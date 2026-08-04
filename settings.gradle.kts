pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "legacy-platform"

include(
    "language-common",
    "language-velocity",
    "legacy-lobby"
)
