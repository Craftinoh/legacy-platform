import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    // Fornito a runtime dal plugin NetworkLanguage: includerlo qui creerebbe una
    // seconda definizione delle stesse classi e l'instanceof sull'holder
    // fallirebbe fra classloader diversi.
    compileOnly(project(":language-common"))
    testImplementation(project(":language-common"))

    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")

    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.postgresql:postgresql:42.7.3")

    // Solo per i test del repository JDBC: lo schema e' scritto in SQL portabile,
    // quindi le stesse istruzioni girano su SQLite in memoria.
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.3")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.named("compileClasspath") {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.jar {
    enabled = false
}

/**
 * Prefissi che non devono mai finire nell'artefatto finale.
 *
 * Le classi lingua arrivano da NetworkLanguage a runtime, le API server sono di
 * un altro mondo (Bukkit) e le librerie di test non hanno nulla da fare dentro
 * un plugin spedito in produzione.
 */
val forbiddenJarEntries = listOf(
    "it/legacynetwork/language/**",
    "org/bukkit/**",
    "org/spigotmc/**",
    "org/junit/**",
    "org/mockito/**",
    "net/bytebuddy/**",
    "org/sqlite/**"
)

tasks.shadowJar {
    archiveBaseName.set("LegacyReports")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    relocate("com.zaxxer.hikari", "it.legacynetwork.shadow.hikari")
    relocate("org.postgresql", "it.legacynetwork.shadow.postgresql")
    relocate("org.yaml.snakeyaml", "it.legacynetwork.shadow.snakeyaml")

    // Guardia strutturale, come in chickenwars-velocity.
    doLast {
        val jar = archiveFile.get().asFile
        val leaked = zipTree(jar).matching {
            include(forbiddenJarEntries)
        }.files
        if (leaked.isNotEmpty()) {
            throw GradleException(
                "Classi non consentite in ${jar.name}: ${leaked.size} voci " +
                    "corrispondenti a $forbiddenJarEntries"
            )
        }
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
