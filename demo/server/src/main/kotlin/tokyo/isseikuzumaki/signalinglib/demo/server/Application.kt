package tokyo.isseikuzumaki.signalinglib.demo.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tokyo.isseikuzumaki.signalinglib.demo.server.plugins.*
import tokyo.isseikuzumaki.signalinglib.demo.server.service.signaling.SignalingServiceImpl
import tokyo.isseikuzumaki.signalinglib.demo.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.signalinglib.demo.shared.SERVER_PORT

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = SERVER_DOMAIN, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val sessionManagement = tokyo.isseikuzumaki.signalinglib.server.SessionManager()
    val signalingService = SignalingServiceImpl(this, sessionManagement)
    configureRPC()
    configureRouting(signalingService, sessionManagement)
    configureClient(signalingService, sessionManagement)
}
