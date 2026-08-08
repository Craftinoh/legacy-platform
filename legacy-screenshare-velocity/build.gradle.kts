import java.util.zip.ZipFile
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

    // Il jar di LegacyReports contiene esclusivamente it/legacynetwork/reports/api:
    // il dominio interno non e' nemmeno visibile al compilatore.
    compileOnly(project(":legacy-reports-velocity"))
    // I test, invece, verificano l'integrazione contro il vero ReportService.
    testImplementation(
        project(path = ":legacy-reports-velocity", configuration = "internalClasses")
    )

    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    // Serve ai test strutturali che leggono davvero @Plugin e @Dependency.
    testImplementation("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")

    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.postgresql:postgresql:42.7.3")

    // Solo per i test del repository JDBC: lo schema e' SQL portabile.
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.3")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Perche' 25 e non 21: velocity-api 4.1.0-SNAPSHOT pubblica metadati Gradle che
// la dichiarano compatibile solo con JVM 25 o superiore, quindi con l'attributo
// a 21 la *risoluzione* fallisce e il modulo non compila affatto. L'attributo
// sceglie la variante da scaricare, non il bytecode prodotto: quello resta
// Java 21 grazie a options.release, ed e' 'verifyBytecodeVersion' a dimostrarlo
// ispezionando ogni classe dell'artefatto finale, dipendenze shaded comprese.
listOf("compileClasspath", "testCompileClasspath", "testRuntimeClasspath")
    .forEach { name ->
        configurations.named(name) {
            attributes {
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
            }
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

/**
 * Il plugin dichiara di girare su Java 21: questo lo verifica sull'artefatto
 * vero, classe per classe, dipendenze impacchettate comprese. Le classi sotto
 * META-INF/versions/N con N maggiore di 21 non vengono contate: una JVM 21 non
 * le carica nemmeno.
 */
val maximumClassFileMajor = 65 // Java 21

val verifyBytecodeVersion = tasks.register("verifyBytecodeVersion") {
    group = "verification"
    description = "Verifica che ogni classe dell'artefatto sia Java 21 o precedente."
    dependsOn(tasks.shadowJar)
    doLast {
        val jar = tasks.shadowJar.get().archiveFile.get().asFile
        var scanned = 0
        var highest = 0
        var ignoredMultiRelease = 0
        val offenders = mutableListOf<String>()
        val versioned = Regex("^META-INF/versions/(\\d+)/.*")

        ZipFile(jar).use { zip ->
            for (entry in zip.entries()) {
                if (entry.isDirectory || !entry.name.endsWith(".class")) {
                    continue
                }
                val runtimeFloor = versioned.find(entry.name)
                    ?.groupValues?.get(1)?.toInt() ?: 0
                val header = ByteArray(8)
                zip.getInputStream(entry).use { input ->
                    var read = 0
                    while (read < header.size) {
                        val step = input.read(header, read, header.size - read)
                        if (step < 0) break
                        read += step
                    }
                    if (read < header.size
                        || header[0] != 0xCA.toByte() || header[1] != 0xFE.toByte()
                        || header[2] != 0xBA.toByte() || header[3] != 0xBE.toByte()
                    ) {
                        return@use
                    }
                    val major = ((header[6].toInt() and 0xFF) shl 8) or
                        (header[7].toInt() and 0xFF)
                    if (runtimeFloor > 21) {
                        ignoredMultiRelease++
                        return@use
                    }
                    scanned++
                    if (major > highest) highest = major
                    if (major > maximumClassFileMajor) {
                        offenders += "${entry.name} (major $major)"
                    }
                }
            }
        }

        logger.lifecycle(
            "verifyBytecodeVersion: ${jar.name} - classi verificate $scanned, " +
                "major massima $highest (limite $maximumClassFileMajor), " +
                "rami multi-release ignorati $ignoredMultiRelease"
        )
        if (scanned == 0) {
            throw GradleException("Nessuna classe verificata in ${jar.name}")
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Bytecode oltre Java 21 in ${jar.name}: " +
                    offenders.take(20).joinToString(", ") +
                    " (totale ${offenders.size})"
            )
        }
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.check {
    dependsOn(verifyBytecodeVersion)
}

tasks.test {
    useJUnitPlatform()
}
