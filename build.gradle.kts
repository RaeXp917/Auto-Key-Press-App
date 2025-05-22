plugins {
    kotlin("jvm") version "2.0.0" // Or your stable Kotlin version
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.github.kwhat:jnativehook:2.2.2")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("Main.Kt") // <<< ENSURE THIS IS CORRECT FOR YOUR PROJECT
}

tasks.shadowJar {
    archiveBaseName.set("KotlinAutoKeyPresser")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass.get()))
    }
}

kotlin {
    jvmToolchain(17) // JDK 17 is a good choice for compatibility
}