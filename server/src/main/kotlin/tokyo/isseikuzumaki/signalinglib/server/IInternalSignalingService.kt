package tokyo.isseikuzumaki.signalinglib.server

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.signalinglib.shared.ISignalingService
import tokyo.isseikuzumaki.signalinglib.shared.SessionRequest
import tokyo.isseikuzumaki.signalinglib.shared.SignalingAnswer
import tokyo.isseikuzumaki.signalinglib.shared.SignalingCandidate
import tokyo.isseikuzumaki.signalinglib.shared.SignalingOffer

typealias AnswerFlowGenerator = (SignalingOffer) -> Flow<SignalingAnswer>
typealias CandidateFlowGenerator = (SignalingCandidate) -> Flow<Unit>

interface IInternalSignalingService: ISignalingService {
    fun waitForOfferInternal(
        request: SessionRequest,
        howToAnswer: AnswerFlowGenerator,
        registerRemoteCandidate: CandidateFlowGenerator
    ): Flow<Unit>
}