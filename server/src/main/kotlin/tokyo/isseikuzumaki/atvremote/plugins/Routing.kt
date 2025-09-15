package tokyo.isseikuzumaki.atvremote.plugins

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import tokyo.isseikuzumaki.atvremote.service.AtvControlServiceImpl
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService
import tokyo.isseikuzumaki.atvremote.shared.PATH_RPC

fun Application.configureRouting() {
    routing {
        // RPC WebSocket endpoint for kotlinx-rpc communication
        rpc(PATH_RPC) {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<AtvControlService> { AtvControlServiceImpl() }
        }

        // Static file serving for web client
        staticResources("/", "static")
    }
}
