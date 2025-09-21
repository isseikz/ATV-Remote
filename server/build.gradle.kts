plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "tokyo.isseikuzumaki.atvremote"
version = "1.0.0"

application {
    mainClass.set("tokyo.isseikuzumaki.atvremote.ApplicationKt")
}

dependencies {
    implementation(projects.shared)
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

val loadVariables = tasks.register("loadVariables") {
    // .env file から環境変数を読み込み、kotlin object DotEnv を生成するタスク
    doLast {
        val envFile = rootProject.file(".env")
        if (!envFile.exists()) {
            throw GradleException(".env file not found in project root")
        }
        try {
            val entries = mutableListOf<Pair<String, String>>()
            // 行ごとに読み込み
            envFile.inputStream().use {
                it.reader().readLines().forEach { line ->
                    // コメント行と空行をスキップ
                    if (line.startsWith("#") || line.isBlank()) return@forEach

                    // KEY=VALUE の形式で key, value を取得する
                    val (key, value) = line.split("=", limit = 2)
                    // 環境変数として設定
                    entries.add(key to value)
                }
            }

            // object AppConfig のソースコードを生成
            val configFile =
                projectDir.resolve("build/generated/source/dotEnv/tokyo/isseikuzumaki/atvremote/DotEnv.kt")
            configFile.parentFile.mkdirs()
            configFile.writeText(
                """
                package tokyo.isseikuzumaki.atvremote

                object DotEnv {
                    ${entries.joinToString("\n    ") { (key, value) -> "const val $key = \"$value\"" }}
                }
            """.trimIndent()
            )
            println("Loaded environment variables from .env and generated AppConfig.kt")
        } catch (e: Exception) {
            throw GradleException("Failed to load .env file", e)
        }
    }
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

tasks.create("runApp") {
    group = "application"
    description = "Runs the ATV-Remote server"
    dependsOn(buildApp)
    finalizedBy(tasks.named("run"))
}
