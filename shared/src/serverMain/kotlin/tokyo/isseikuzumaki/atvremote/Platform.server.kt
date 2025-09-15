package tokyo.isseikuzumaki.atvremote

class ServerPlatform: Platform {
    override val name: String = "Server with Kotlin/JVM"
}

actual fun getPlatform(): Platform = ServerPlatform()
