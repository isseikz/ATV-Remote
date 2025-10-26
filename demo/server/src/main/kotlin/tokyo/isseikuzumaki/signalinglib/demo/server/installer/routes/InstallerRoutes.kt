package tokyo.isseikuzumaki.signalinglib.demo.server.installer.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.BuildNotificationRequest
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.DeviceRegistration
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.FcmNotificationPayload
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.RegisteredDevice
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.DeviceRegistry
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.FcmNotificationService
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.service.S3PresignedUrlService
import java.util.UUID

/**
 * Configure routes for the APK installer system
 */
fun Route.installerRoutes(
    deviceRegistry: DeviceRegistry,
    fcmService: FcmNotificationService,
    s3Service: S3PresignedUrlService
) {
    val logger = LoggerFactory.getLogger("InstallerRoutes")

    route("/api") {
        
        /**
         * Device registration endpoint
         * POST /api/devices/register
         */
        post("/devices/register") {
            try {
                val registration = call.receive<DeviceRegistration>()
                
                val device = RegisteredDevice(
                    deviceId = registration.deviceId,
                    fcmToken = registration.fcmToken,
                    deviceModel = registration.deviceModel,
                    androidVersion = registration.androidVersion,
                    appVersion = registration.appVersion,
                    registeredAt = System.currentTimeMillis(),
                    lastSeen = System.currentTimeMillis()
                )

                deviceRegistry.registerDevice(device)
                logger.info("Device registered: ${device.deviceId} (${device.deviceModel})")

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Device registered successfully",
                        "deviceId" to device.deviceId
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to register device", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to register device: ${e.message}"
                    )
                )
            }
        }

        /**
         * Get all registered devices
         * GET /api/devices
         */
        get("/devices") {
            try {
                val devices = deviceRegistry.getAllDevices()
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "count" to devices.size,
                        "devices" to devices
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to get devices", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to get devices: ${e.message}"
                    )
                )
            }
        }

        /**
         * Build notification webhook from CI/CD
         * POST /api/builds/notify
         */
        post("/builds/notify") {
            try {
                val buildRequest = call.receive<BuildNotificationRequest>()
                logger.info("Received build notification: ${buildRequest.versionName} (${buildRequest.branchName})")

                // Generate pre-signed URL for APK download
                val downloadUrl = s3Service.generatePresignedUrl(
                    bucket = buildRequest.s3Bucket,
                    key = buildRequest.s3Key,
                    expirationMinutes = 120 // 2 hours
                )

                // Get target devices
                val targetDevices = deviceRegistry.getDevicesByGroup(buildRequest.targetDeviceGroup)
                
                if (targetDevices.isEmpty()) {
                    logger.warn("No devices registered to receive build notification")
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "No devices to notify",
                            "devicesNotified" to 0
                        )
                    )
                    return@post
                }

                // Create FCM payload
                val fcmPayload = FcmNotificationPayload(
                    downloadUrl = downloadUrl,
                    branchName = buildRequest.branchName,
                    commitHash = buildRequest.commitHash,
                    versionName = buildRequest.versionName,
                    versionCode = buildRequest.versionCode,
                    buildTimestamp = buildRequest.buildTimestamp,
                    buildId = UUID.randomUUID().toString()
                )

                // Send notifications to all target devices
                val results = fcmService.sendBuildNotificationToDevices(targetDevices, fcmPayload)
                val successCount = results.values.count { it }

                logger.info("Build notification sent to $successCount/${targetDevices.size} devices")

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Build notification sent",
                        "devicesNotified" to successCount,
                        "totalDevices" to targetDevices.size,
                        "results" to results
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to process build notification", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to process build notification: ${e.message}"
                    )
                )
            }
        }

        /**
         * Health check endpoint
         * GET /api/health
         */
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "healthy",
                    "service" to "apk-installer",
                    "devicesRegistered" to deviceRegistry.getDeviceCount()
                )
            )
        }
    }
}
