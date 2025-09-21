package tokyo.isseikuzumaki.atvremote.plugins

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import tokyo.isseikuzumaki.atvremote.SessionManager
import tokyo.isseikuzumaki.atvremote.service.AtvControlServiceImpl
import tokyo.isseikuzumaki.atvremote.service.SessionServiceImpl
import tokyo.isseikuzumaki.atvremote.shared.IAtvControlService
import tokyo.isseikuzumaki.atvremote.shared.ISessionService
import tokyo.isseikuzumaki.atvremote.shared.ISignalingService
import tokyo.isseikuzumaki.atvremote.shared.PATH_RPC

fun Application.configureRouting(
    signaling: ISignalingService,
    sessionManager: SessionManager
) {
    routing {
        // RPC WebSocket endpoint for kotlinx-rpc communication
        rpc(PATH_RPC) {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<IAtvControlService> { AtvControlServiceImpl(this@configureRouting, sessionManager) } // TODO セッション別に別の adb 接続を管理する
            registerService<ISignalingService> { signaling }
            registerService<ISessionService> { SessionServiceImpl(sessionManager) }
        }

        // Static file serving for web client
        staticResources("/", "static")
    }
}
