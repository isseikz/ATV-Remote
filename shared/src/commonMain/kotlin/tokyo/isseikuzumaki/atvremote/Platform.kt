package tokyo.isseikuzumaki.atvremote

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform