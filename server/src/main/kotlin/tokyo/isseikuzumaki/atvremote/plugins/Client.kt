package tokyo.isseikuzumaki.atvremote.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.atvremote.SessionManager
import tokyo.isseikuzumaki.atvremote.client.TurnServerService
import tokyo.isseikuzumaki.atvremote.client.WebRTCClientImpl
import tokyo.isseikuzumaki.atvremote.config.TurnServerConfig
import tokyo.isseikuzumaki.atvremote.service.signaling.IInternalSignalingService
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer
import tokyo.isseikuzumaki.atvremote.shared.SessionName
import tokyo.isseikuzumaki.atvremote.shared.SessionRequest
import tokyo.isseikuzumaki.atvremote.shared.SignalingAnswer
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
