package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.AdbCommandResult
import tokyo.isseikuzumaki.atvremote.shared.AdbDevice
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService
import tokyo.isseikuzumaki.atvremote.shared.DeviceId
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer

class AtvControlServiceImpl : AtvControlService {

    companion object {
        private const val TAG = "AtvControlServiceImpl"
        private val adbManager = AdbManager()
        private val webRTCManager = WebRTCSignalingManager()
    }

    override fun adbDevices(): Flow<List<AdbDevice>> = flow {
        Logger.d(TAG, "Fetching ADB devices")
        val devices = adbManager.devices()
        Logger.d(TAG, "Found ${devices.size} ADB devices")
        emit(devices)
    }

    override fun sendSdpOffer(deviceId: DeviceId, offer: SdpOffer): Flow<SdpAnswer> = flow {
        Logger.d(TAG, "Received SDP Offer from client: $deviceId")
        try {
            webRTCManager.handleOffer(deviceId, offer).first()
                .getOrThrow().let {
                    Logger.d(TAG, "SDP Answer generated for client: ${it.deviceId}")
                    emit(it)
                }
        } catch (e: IllegalArgumentException) {
            Logger.d(TAG, "Invalid SDP Offer: ${e.message}")
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing SDP Offer", e)
            throw InternalError("Failed to process SDP Offer")
        }
    }

    override fun sendIceCandidate(deviceId: DeviceId, candidate: IceCandidateData): Flow<IceCandidateData> =
        webRTCManager.addRemoteCandidate(deviceId, candidate)

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
}
