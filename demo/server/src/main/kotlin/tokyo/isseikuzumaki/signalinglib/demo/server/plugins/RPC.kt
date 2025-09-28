package tokyo.isseikuzumaki.signalinglib.demo.server.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlin.time.Duration.Companion.seconds

fun Application.configureRPC() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 5.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Krpc)
}
