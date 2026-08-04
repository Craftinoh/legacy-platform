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

tasks.register("buildPlugins") {
    group = "build"
    description = "Compila, testa e assembla tutti i moduli e i plugin finali."
    dependsOn(
        ":language-common:build",
        ":language-velocity:build",
        ":legacy-lobby:build",
        ":legacy-items:build",
        ":legacy-menu:build",
        ":legacy-combat:build"
    )
}
