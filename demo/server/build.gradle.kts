import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "tokyo.isseikuzumaki.signalinglib.demo"
version = "1.0.0"

application {
    mainClass.set("tokyo.isseikuzumaki.signalinglib.demo.server.ApplicationKt")
}

dependencies {
    implementation(projects.server)
    implementation(projects.demo.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.rpc.krpcServer.ktor)
    implementation(libs.kotlinx.rpc.krpcServer)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.webcam.capture)
    implementation(libs.webrtc.java)

    // TURN server API client dependencies
    implementation(libs.ktor.client.core.v236)
    implementation(libs.ktor.client.cio.v236)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    runtimeOnly(libs.webrtc.java) {
        artifact {
            classifier = "macos-aarch64"
        }
    }
}

abstract class LoadVariablesTask : DefaultTask() {
    @get:InputFile
    abstract val envFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun loadVariables() {
        val envFile = envFile.get().asFile
        if (!envFile.exists()) {
            throw GradleException(".env file not found: ${envFile.absolutePath}")
        }

        val entries = mutableListOf<Pair<String, String>>()
        envFile.inputStream().use {
            it.reader().readLines().forEach { line ->
                if (line.startsWith("#") || line.isBlank()) return@forEach
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    entries.add(parts[0] to parts[1])
                }
            }
        }

        val configFile = outputFile.get().asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            package tokyo.isseikuzumaki.signalinglib.demo.server

            object DotEnv {
                ${entries.joinToString("\n    ") { (key, value) -> "const val $key = \"$value\"" }}
            }
        """.trimIndent()
        )
        println("Loaded environment variables from .env and generated DotEnv.kt")
    }
}

val loadVariables = tasks.register<LoadVariablesTask>("loadVariables") {
    envFile.set(rootProject.file(".env"))
    outputFile.set(projectDir.resolve("build/generated/source/dotEnv/tokyo/isseikuzumaki/signalinglib/demo/server/DotEnv.kt"))
}

kotlin {
    sourceSets {
        val main by getting {
            kotlin.srcDir("build/generated/source/dotEnv")
        }
    }
}

val buildApp = tasks.register("buildApp") {
    finalizedBy("assemble")
}

val runApp = tasks.register("runApp") {
    group = "application"
    description = "Runs the ATV-Remote demo server"
    dependsOn(loadVariables)
    doFirst {
        // Kill any existing processes using the same package to avoid port conflicts
        exec {
            commandLine("pkill", "-f", "tokyo.isseikuzumaki.signalinglib.demo.server")
            isIgnoreExitValue = true
        }
        Thread.sleep(1000) // Wait a moment for processes to terminate
    }
    finalizedBy(tasks.named("run"))
}

// Fix duplicate handling strategy for distribution tasks
tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.WARN
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.WARN
}

// Ensure loadVariables runs before compilation
tasks.named("compileKotlin") {
    dependsOn(loadVariables)
}

tasks.named("run") {
    dependsOn(loadVariables)
}