plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    // Contratti di routing condivisi con il proxy Velocity: stessa forma, stesso
    // schema, nessun algoritmo duplicato ai due lati.
    implementation(project(":chickenwars-common"))
    compileOnly(project(":language-common"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("com.zaxxer:HikariCP:4.0.3")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    testImplementation(project(":language-common"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("com.h2database:h2:2.2.224")
    constraints {
        testImplementation("net.bytebuddy:byte-buddy:1.17.6")
        testImplementation("net.bytebuddy:byte-buddy-agent:1.17.6")
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.jar { enabled = false }

tasks.shadowJar {
    archiveBaseName.set("LegacyChickenWars")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }
