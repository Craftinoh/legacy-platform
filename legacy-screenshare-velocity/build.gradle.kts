import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    // Entrambe le dipendenze arrivano a runtime dai rispettivi plugin: sono
    // dichiarate nel metadata Velocity e non vengono impacchettate qui, perche'
    // una seconda definizione delle stesse classi renderebbe falso ogni
    // instanceof fra classloader diversi.
    compileOnly(project(":language-common"))
    testImplementation(project(":language-common"))
    compileOnly(project(":legacy-reports-velocity"))
    testImplementation(project(":legacy-reports-velocity"))

    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")

    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.postgresql:postgresql:42.7.3")

    // Solo per i test del repository JDBC: lo schema e' SQL portabile.
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
 * Lingue e report arrivano dai plugin che li possiedono; Bukkit non esiste su
 * un proxy; le librerie di test non hanno nulla da fare in produzione.
 */
val forbiddenJarEntries = listOf(
    "it/legacynetwork/language/**",
    "it/legacynetwork/reports/**",
    "org/bukkit/**",
    "org/spigotmc/**",
    "org/junit/**",
    "org/mockito/**",
    "net/bytebuddy/**",
    "org/sqlite/**"
)

tasks.shadowJar {
    archiveBaseName.set("LegacyScreenshare")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    relocate("com.zaxxer.hikari", "it.legacynetwork.shadow.hikari")
    relocate("org.postgresql", "it.legacynetwork.shadow.postgresql")
    relocate("org.yaml.snakeyaml", "it.legacynetwork.shadow.snakeyaml")

    // Guardia strutturale, come in chickenwars-velocity e legacy-reports.
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
