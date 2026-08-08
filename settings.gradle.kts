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
    "chickenwars-common",
    "chickenwars-velocity",
    "legacy-lobby",
    "legacy-items",
    "legacy-menu",
    "legacy-combat",
    "legacy-regions",
    "legacy-chickenwars",
    "language-backend"
)
