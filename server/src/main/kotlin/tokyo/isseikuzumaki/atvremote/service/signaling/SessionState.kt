package tokyo.isseikuzumaki.atvremote.service.signaling

import tokyo.isseikuzumaki.atvremote.shared.SessionID

sealed class SessionState {
    data class Waiting(
        val sessionID: SessionID,
        val waitingClient: ISignalingClient
    ): SessionState()
    data class Connecting(
        val sessionID: SessionID,
        val clients: Pair<ISignalingClient, ISignalingClient>
    ) : SessionState()
    data class Connected(
        val sessionID: SessionID,
        val clients: Pair<ISignalingClient, ISignalingClient>
    ) : SessionState()
    data class Error(val message: String) : SessionState()
}
