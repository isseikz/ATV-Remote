package tokyo.isseikuzumaki.atvremote.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import tokyo.isseikuzumaki.atvremote.service.AtvControlServiceImpl
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService

fun Application.configureRouting() {
    routing {
        // RPC WebSocket endpoint for kotlinx-rpc communication
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            webSocket {
                registerService<AtvControlService> { AtvControlServiceImpl() }
            }
        }

        // Static file serving for web client
        staticResources("/", "static")
    }
}
