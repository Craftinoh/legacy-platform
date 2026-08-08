import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    // Solo contratti puri: il proxy non vede mai una classe Bukkit.
    implementation(project(":chickenwars-common"))

    // Fornito a runtime dal plugin NetworkLanguage, come si fa con LuckPerms:
    // includerlo qui creerebbe una seconda definizione delle stesse classi e
    // il controllo instanceof sull'holder fallirebbe fra classloader diversi.
    compileOnly(project(":language-common"))
    testImplementation(project(":language-common"))

    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")

    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.postgresql:postgresql:42.7.3")

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

tasks.shadowJar {
    archiveBaseName.set("LegacyChickenWarsProxy")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    relocate("com.zaxxer.hikari", "it.legacynetwork.shadow.hikari")
    relocate("org.postgresql", "it.legacynetwork.shadow.postgresql")
    relocate("org.yaml.snakeyaml", "it.legacynetwork.shadow.snakeyaml")

    // Guardia strutturale: le classi lingua devono esistere in una sola
    // definizione, quella spedita da NetworkLanguage. Un doppio packaging
    // renderebbe falso ogni instanceof fra i due plugin.
    doLast {
        val jar = archiveFile.get().asFile
        val duplicated = zipTree(jar).matching {
            include("it/legacynetwork/language/**")
        }.files
        if (duplicated.isNotEmpty()) {
            throw GradleException(
                "language-common non deve finire in ${jar.name}: " +
                    "${duplicated.size} classi duplicate rispetto a NetworkLanguage"
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
