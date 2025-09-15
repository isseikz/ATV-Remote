package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.AdbCommandResult
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateResponse
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer
import java.util.UUID

class AtvControlServiceImpl : AtvControlService {

    companion object {
        private const val TAG = "AtvControlServiceImpl"
        private val adbManager = AdbManager()
        private val webRTCManager = WebRTCSignalingManager()
    }


    override fun sendSdpOffer(offer: SdpOffer): Flow<SdpAnswer> = flow {
        Logger.d(TAG, "Received SDP Offer from client: $offer")
        val sessionId = UUID.randomUUID().toString()
        emit(webRTCManager.handleOffer(sessionId, offer))
    }

    override fun sendIceCandidate(candidate: IceCandidateData): Flow<IceCandidateResponse> = flow {
        Logger.d(TAG, "Received ICE Candidate from client ${candidate.sessionId}")

        try {
            // Store ICE Candidate
            webRTCManager.addIceCandidate(candidate)

            // Send server-side ICE Candidate to client (mock for now)
            val serverCandidate = IceCandidateData(
                sessionId = candidate.sessionId,
                candidate = "candidate:1 1 UDP 2113667326 192.168.1.100 54400 typ host",
                sdpMid = candidate.sdpMid,
                sdpMLineIndex = candidate.sdpMLineIndex
            )

            Logger.d(TAG, "ICE Candidate processed for client $candidate.sessionId")

            emit(IceCandidateResponse(listOf(serverCandidate)))
        } catch (e: Exception) {
            Logger.d(TAG, "Error handling ICE Candidate: ${e.message}")

            emit(IceCandidateResponse(emptyList()))
        }
    }

    override fun sendAdbCommand(command: AdbCommand): Flow<AdbCommandResult> = flow {
        Logger.d(TAG, "Received ADB Command from client : ${command.command}")

        try {
            // Execute ADB command
            val result = adbManager.executeCommand(command)

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
