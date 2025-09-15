package tokyo.isseikuzumaki.atvremote

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tokyo.isseikuzumaki.atvremote.plugins.*
import tokyo.isseikuzumaki.atvremote.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.atvremote.shared.SERVER_PORT

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = SERVER_DOMAIN, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRPC()
    configureRouting()
}
