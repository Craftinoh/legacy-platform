plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
    withSourcesJar()
}

// Contratti condivisi fra il backend Bukkit (Java 8) e il proxy Velocity
// (Java 21): il livello di bytecode piu' basso deve restare quello del
// backend, altrimenti PandaSpigot 1.8.8 non potrebbe caricarli.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
