package tokyo.isseikuzumaki.signalinglib.demo.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import tokyo.isseikuzumaki.signalinglib.server.SessionManager
import tokyo.isseikuzumaki.signalinglib.demo.server.service.AtvControlServiceImpl
import tokyo.isseikuzumaki.signalinglib.demo.server.service.SessionServiceImpl
import tokyo.isseikuzumaki.signalinglib.demo.shared.IAtvControlService
import tokyo.isseikuzumaki.signalinglib.shared.ISessionService
import tokyo.isseikuzumaki.signalinglib.shared.ISignalingService
import tokyo.isseikuzumaki.signalinglib.demo.shared.PATH_RPC
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.routes.installerRoutes
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.DeviceRegistry
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.FcmNotificationService
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.S3PresignedUrlService

fun Application.configureRouting(
    signaling: ISignalingService,
    sessionManager: SessionManager,
    deviceRegistry: DeviceRegistry,
    fcmService: FcmNotificationService,
    s3Service: S3PresignedUrlService
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

        // Installer REST API routes
        installerRoutes(deviceRegistry, fcmService, s3Service)

        // Static file serving for web client
        staticResources("/", "static")
    }
}
