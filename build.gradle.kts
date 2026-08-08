plugins {
    base
}

allprojects {
    group = "it.legacynetwork"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()

        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        maven {
            name = "spigotmc"
            url = uri("https://hub.spigotmc.org/nexus/content/groups/public/")
        }

        maven {
            name = "placeholderapi"
            url = uri("https://repo.helpch.at/releases/")
        }

        maven {
            name = "codemc"
            url = uri("https://repo.codemc.io/repository/maven-releases/")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// LegacyAuth (legacy-auth)
//
// LegacyAuth is a Maven project (fork of AuthMeReloaded 5.6.0) and is intentionally NOT included
// as a Gradle subproject: its official build relies on Maven Shade to relocate and bundle its
// dependencies. It is driven through its own Maven Wrapper instead, so no Maven dependency is
// duplicated here.
// ---------------------------------------------------------------------------------------------
val legacyAuthDir = layout.projectDirectory.dir("legacy-auth")

fun Exec.legacyAuthMaven(vararg mavenArgs: String) {
    group = "legacy-auth"
    workingDir = legacyAuthDir.asFile
    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        commandLine(listOf("cmd", "/c", "mvnw.cmd") + mavenArgs)
    } else {
        commandLine(listOf("./mvnw") + mavenArgs)
    }
}

tasks.register<Exec>("legacyAuthTest") {
    description = "Esegue i test di LegacyAuth tramite il Maven Wrapper."
    legacyAuthMaven("-B", "test")
}

tasks.register<Exec>("legacyAuthBuild") {
    description = "Compila, testa e assembla LegacyAuth tramite il Maven Wrapper."
    legacyAuthMaven("-B", "clean", "package")
}

tasks.register<Exec>("legacyAuthClean") {
    description = "Pulisce la directory target di LegacyAuth tramite il Maven Wrapper."
    legacyAuthMaven("-B", "clean")
}

tasks.register("buildPlugins") {
    group = "build"
    description = "Compila, testa e assembla tutti i moduli e i plugin finali."
    dependsOn(
        ":language-common:build",
        ":language-velocity:build",
        ":language-backend:build",
        ":chickenwars-common:build",
        ":chickenwars-velocity:build",
        ":legacy-reports-velocity:build",
        ":legacy-screenshare-velocity:build",
        ":legacy-lobby:build",
        ":legacy-items:build",
        ":legacy-menu:build",
        ":legacy-combat:build",
        ":legacy-regions:build",
        ":legacy-chickenwars:build"
    )
}
