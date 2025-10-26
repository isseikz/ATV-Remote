package tokyo.isseikuzumaki.signalinglib.demo.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tokyo.isseikuzumaki.signalinglib.demo.server.plugins.*
import tokyo.isseikuzumaki.signalinglib.demo.server.service.signaling.SignalingServiceImpl
import tokyo.isseikuzumaki.signalinglib.demo.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.signalinglib.demo.shared.SERVER_PORT
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.DeviceRegistry
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.FcmNotificationService
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.S3PresignedUrlService

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = SERVER_DOMAIN, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val sessionManagement = tokyo.isseikuzumaki.signalinglib.server.SessionManager()
    val signalingService = SignalingServiceImpl(this, sessionManagement)
    
    // Initialize installer services
    val deviceRegistry = DeviceRegistry()
    
    // Initialize FCM service (requires Firebase service account JSON)
    val fcmServiceAccountPath = environment.config.propertyOrNull("installer.fcm.serviceAccountPath")?.getString()
        ?: System.getenv("FCM_SERVICE_ACCOUNT_PATH")
        ?: "/path/to/firebase-service-account.json"
    
    val fcmService = FcmNotificationService(fcmServiceAccountPath)
    
    // Initialize S3 service
    val s3Region = environment.config.propertyOrNull("installer.s3.region")?.getString()
        ?: System.getenv("AWS_REGION")
        ?: "us-east-1"
    
    val s3Service = S3PresignedUrlService(s3Region)
    
    configureRPC()
    configureRouting(signalingService, sessionManagement, deviceRegistry, fcmService, s3Service)
    configureClient(signalingService, sessionManagement)
}
