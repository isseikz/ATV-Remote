package tokyo.isseikuzumaki.atvremote.service.signaling

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.atvremote.shared.ISignalingService
import tokyo.isseikuzumaki.atvremote.shared.SessionRequest
import tokyo.isseikuzumaki.atvremote.shared.SignalingAnswer
import tokyo.isseikuzumaki.atvremote.shared.SignalingCandidate
import tokyo.isseikuzumaki.atvremote.shared.SignalingOffer


typealias AnswerFlowGenerator = (SignalingOffer) -> Flow<SignalingAnswer>
typealias CandidateFlowGenerator = (SignalingCandidate) -> Flow<Unit>

interface IInternalSignalingService: ISignalingService {
    fun waitForOfferInternal(
        request: SessionRequest,
        howToAnswer: AnswerFlowGenerator,
        registerRemoteCandidate: CandidateFlowGenerator
    ): Flow<Unit>
}
