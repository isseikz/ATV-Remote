package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.atvremote.shared.*
import java.util.UUID

class AtvControlServiceImpl : AtvControlService {

    companion object {
        private val adbManager = AdbManager()
        private val webRTCManager = WebRTCSignalingManager()
    }


    override suspend fun sendSdpOffer(offer: SdpOffer): Flow<SdpAnswer> {
        val sessionId = UUID.randomUUID().toString()
        return flowOf(webRTCManager.handleOffer(sessionId, offer))
    }

    override suspend fun sendIceCandidate(candidate: IceCandidateData): Flow<IceCandidateResponse> {
        println("Received ICE Candidate from client ${candidate.sessionId}")

        return try {
            // Store ICE Candidate
            webRTCManager.addIceCandidate(candidate)

            // Send server-side ICE Candidate to client (mock for now)
            val serverCandidate = IceCandidateData(
                sessionId = candidate.sessionId,
                candidate = "candidate:1 1 UDP 2113667326 192.168.1.100 54400 typ host",
                sdpMid = candidate.sdpMid,
                sdpMLineIndex = candidate.sdpMLineIndex
            )

            println("ICE Candidate processed for client $candidate.sessionId")

            flowOf(IceCandidateResponse(listOf(serverCandidate)))
        } catch (e: Exception) {
            println("Error handling ICE Candidate: ${e.message}")

            flowOf(IceCandidateResponse(emptyList()))
        }
    }

    override suspend fun sendAdbCommand(command: AdbCommand): Flow<AdbCommandResult> {
        println("Received ADB Command from client : ${command.command}")

        return try {
            // Execute ADB command
            val result = adbManager.executeCommand(command)

            println("ADB Command executed for client : success=${result.isSuccess}")

            // Notify client of execution result
            flowOf(result)

        } catch (e: Exception) {
            val errorResult = AdbCommandResult(
                output = "Error: ${e.message}",
                isSuccess = false
            )
            println("Error executing ADB Command: ${e.message}")

            flowOf(errorResult)
        }
    }
}
