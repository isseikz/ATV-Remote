plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
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
    implementation(libs.kotlinx.rpc.krpcServer.ktor)
    implementation(libs.kotlinx.rpc.krpcServer)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.webcam.capture)
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
