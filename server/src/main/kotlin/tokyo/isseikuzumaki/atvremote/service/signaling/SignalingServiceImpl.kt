package tokyo.isseikuzumaki.atvremote.service.signaling

import io.ktor.server.application.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import tokyo.isseikuzumaki.atvremote.SessionManager
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SessionID
import tokyo.isseikuzumaki.atvremote.shared.SessionRequest
import tokyo.isseikuzumaki.atvremote.shared.SignalingAnswer
import tokyo.isseikuzumaki.atvremote.shared.SignalingCandidate
import tokyo.isseikuzumaki.atvremote.shared.SignalingOffer
import java.util.concurrent.ConcurrentHashMap


class SignalingServiceImpl(
    private val ktor: Application,
    private val sessionManager: SessionManager
): IInternalSignalingService {
    private val offerCallbacks = ConcurrentHashMap<SessionID, AnswerFlowGenerator>()
    private val registerIce = ConcurrentHashMap<SessionID, CandidateFlowGenerator>()

    override fun offer(offer: SignalingOffer): Flow<SignalingAnswer> = flow {
        val sessions = sessionManager.sessions().first()
        val session = sessions.find { it.id == offer.sessionID }
            ?: throw IllegalArgumentException("Session not found for ID: ${offer.sessionID}")

        val answerMethod = offerCallbacks[session.id]
            ?: throw IllegalStateException("No offer callback registered for session ID: ${session.id}")

        answerMethod(offer).onEach {
            Logger.d("SignalingService", "Generated answer for session ${offer.sessionID}")
        }.collect {
            emit(it)
        }
    }

    override fun waitForOffer(request: SessionRequest): Flow<SignalingOffer> = callbackFlow {
        throw NotImplementedError("waitForOffer is not implemented in SignalingServiceImpl")
    }

    override fun waitForOfferInternal(
        request: SessionRequest,
        howToAnswer: AnswerFlowGenerator,
        registerRemoteCandidate: CandidateFlowGenerator
    ): Flow<Unit> = flow {
        val session = sessionManager.register(request).first().getOrThrow().id

        offerCallbacks[session] = howToAnswer
        registerIce[session] = registerRemoteCandidate
        emit(Unit)
    }

    override fun answer(answer: SignalingAnswer): Flow<SignalingCandidate> =
        TODO("answer is not implemented in SignalingServiceImpl")



    override fun putIceCandidates(candidate: SignalingCandidate): Flow<Unit> {
        return registerIce[candidate.sessionID]?.invoke(candidate)
            ?.onEach { Logger.d("SignalingService", "Registered ICE candidates for session ${candidate.sessionID}") }
            ?: throw IllegalArgumentException("No callback registered for session ID: ${candidate.sessionID}")
    }
}