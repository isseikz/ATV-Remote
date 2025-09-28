package tokyo.isseikuzumaki.signalinglib.demo.server.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.signalinglib.demo.server.client.TurnServerService
import tokyo.isseikuzumaki.signalinglib.demo.server.client.WebRTCClientImpl
import tokyo.isseikuzumaki.signalinglib.demo.server.config.TurnServerConfig
import tokyo.isseikuzumaki.signalinglib.demo.server.service.signaling.IInternalSignalingService
import tokyo.isseikuzumaki.signalinglib.server.SessionManager
import tokyo.isseikuzumaki.signalinglib.shared.Logger
import tokyo.isseikuzumaki.signalinglib.shared.SdpOffer
import tokyo.isseikuzumaki.signalinglib.shared.SessionName
import tokyo.isseikuzumaki.signalinglib.shared.SessionRequest
import tokyo.isseikuzumaki.signalinglib.shared.SignalingAnswer
import java.util.UUID


fun Application.configureClient(
    signalingService: IInternalSignalingService,
    sessionManager: SessionManager,
) {
    val clients = WebRTCClientImpl.Factory(
        TurnServerService(
            TurnServerConfig.fromSystemProperties()
        )
    ).let { factory ->
        factory.getDevices().map { factory.createClient(it) }
    }

    val sessions =
        clients.map { it to SessionRequest(SessionName("Internal-${UUID.randomUUID()}")) }

    val connections = sessions.map { (client, request) ->
        signalingService.waitForOfferInternal(
            request,
            { offer ->
                Logger.d("Client", "Received offer for session ${offer.sessionID}, answering...")
                client.handleOffer(SdpOffer(offer.sdp)).map {
                    Logger.d("Client", "Answering to offer for session ${offer.sessionID}")
                    if (it.candidates.isEmpty()) {
                        SignalingAnswer.Answer(offer.sessionID, it.sdp)
                    } else {
                        SignalingAnswer.Candidate(offer.sessionID, it.candidates.last())
                    }
                }
            },
            { candidates ->
                Logger.d(
                    "Client",
                    "Registering remote candidates for session ${candidates.sessionID}"
                )
                client.handlePutIceCandidates(candidates.candidates)
            }
        )
    }

    CoroutineScope(parentCoroutineContext).launch {
        connections.forEach { it.first() }
    }
}
