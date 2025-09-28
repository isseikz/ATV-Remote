import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            implementation(libs.kotlinx.rpc.krpcClient)
            implementation(libs.kotlinx.rpc.krpc.ktorClient)
            implementation(libs.kotlinx.rpc.krpc.serialization.json)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientWebsockets)
            implementation(libs.webrtc.kmp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.clientEngine.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.clientEngine.ios)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.clientEngine.js)
        }
    }
}

android {
    namespace = "tokyo.isseikuzumaki.signalinglib.client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}