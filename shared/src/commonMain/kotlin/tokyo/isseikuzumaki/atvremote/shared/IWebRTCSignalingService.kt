package tokyo.isseikuzumaki.atvremote.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Serializable
data class SignalingOffer(
    val sessionID: SessionID,
    val sdp: Sdp
)

@Serializable
sealed class SignalingAnswer {
    @Serializable
    data class Answer(
        val sessionID: SessionID,
        val sdp: Sdp,
    ) : SignalingAnswer()

    @Serializable
    data class Candidate(
        val sessionID: SessionID,
        val candidate: IceCandidateData
    ) : SignalingAnswer()
}

@Serializable
data class SignalingCandidate(
    val sessionID: SessionID,
    val candidates: List<IceCandidateData>
)


@Serializable
data class Sdp(val value: String)

@Serializable
data class SdpOffer(
    val sdp: Sdp
)

@Serializable
data class SdpAnswer(
    val sdp: Sdp,
    val candidates: List<IceCandidateData>
)

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)

/**
 * WebRTC シグナリングサーバーの RPC サービスインターフェース
 * クライアントがシグナリングサーバーに対して行う操作を定義する
 */
@Rpc
interface ISignalingService {
    /**
     * クライアントからのOfferを受け取り、対応するAnswerを返す
     * @param offer クライアントから送信されたSDP Offer
     * @return 対応するSDP AnswerとICE Candidate
     */
    fun offer(offer: SignalingOffer): Flow<SignalingAnswer>

    /**
     * ウェイティングリストに自身のセッションを登録して、Offer を待機する
     *
     * @param request セッションの詳細情報
     * @return SDP Offer
     */
    fun waitForOffer(request: SessionRequest): Flow<SignalingOffer>

    /**
     * Offer に対する Answer を送信する
     *
     * @param answer SDP Answer
     * @return リモートデバイスの ICE Candidate. これを setRemoteDescription 後に addIceCandidate で追加する
     */
    fun answer(answer: SignalingAnswer): Flow<SignalingCandidate>

    /**
     * クライアントからのICE Candidateを受け取り、保存する
     * @param candidate クライアントから送信されたICE Candidate情報
     */
    fun putIceCandidates(candidate: SignalingCandidate): Flow<Unit>
}
