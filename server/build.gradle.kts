plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.kotlinSerialization)
}

group = "tokyo.isseikuzumaki.signalinglib.server"
version = "1.0.0"

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.rpc.krpcServer.ktor)
    implementation(libs.kotlinx.rpc.krpcServer)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
}