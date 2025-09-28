package tokyo.isseikuzumaki.signalinglib.demo.server.service

import io.ktor.server.application.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tokyo.isseikuzumaki.signalinglib.server.SessionManager
import tokyo.isseikuzumaki.signalinglib.demo.server.client.AdbManager
import tokyo.isseikuzumaki.signalinglib.demo.shared.AdbCommand
import tokyo.isseikuzumaki.signalinglib.demo.shared.AdbCommandResult
import tokyo.isseikuzumaki.signalinglib.demo.shared.AdbDevice
import tokyo.isseikuzumaki.signalinglib.demo.shared.DeviceId
import tokyo.isseikuzumaki.signalinglib.demo.shared.IAtvControlService
import tokyo.isseikuzumaki.signalinglib.shared.Logger
import tokyo.isseikuzumaki.signalinglib.demo.shared.ScreenshotResult

class AtvControlServiceImpl(
    private val application: Application,
    private val sessionManager: SessionManager
) : IAtvControlService {

    companion object {
        private const val TAG = "AtvControlServiceImpl"
        private val adbManager = AdbManager()
    }

    override fun adbDevices(): Flow<List<AdbDevice>> = flow {
        Logger.d(TAG, "Fetching ADB devices")
        val devices = adbManager.devices()
        Logger.d(TAG, "Found ${devices.size} ADB devices")
        emit(devices)
    }

    override fun sendAdbCommand(deviceId: DeviceId, command: AdbCommand): Flow<AdbCommandResult> = flow {
        Logger.d(TAG, "Received ADB Command from client : ${command.command}")

        try {
            // Execute ADB command
            val result = adbManager.executeCommand(deviceId, command)

            Logger.d(TAG, "ADB Command executed for client : success=${result.isSuccess}")

            // Notify client of execution result
            emit(result)

        } catch (e: Exception) {
            val errorResult = AdbCommandResult(
                output = "Error: ${e.message}",
                isSuccess = false
            )
            Logger.d(TAG, "Error executing ADB Command: ${e.message}")

            emit(errorResult)
        }
    }

    override fun takeScreenshot(deviceId: DeviceId): Flow<ScreenshotResult> = flow {
        Logger.d(TAG, "Taking screenshot for device: $deviceId")
        
        try {
            val result = adbManager.takeScreenshot(deviceId)
            Logger.d(TAG, "Screenshot taken for device $deviceId: success=${result.isSuccess}, size=${result.imageData.size} bytes")
            emit(result)
        } catch (e: Exception) {
            val errorResult = ScreenshotResult(
                imageData = ByteArray(0),
                isSuccess = false,
                fileName = "error_screenshot.png"
            )
            Logger.e(TAG, "Error taking screenshot for device $deviceId: ${e.message}")
            emit(errorResult)
        }
    }
}
