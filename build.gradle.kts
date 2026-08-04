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
    }
}

tasks.register("buildPlugins") {
    group = "build"
    description = "Compila, testa e assembla tutti i moduli e i due plugin finali."
    dependsOn(
        ":language-common:build",
        ":language-velocity:build",
        ":legacy-lobby:build"
    )
}
