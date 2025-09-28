package tokyo.isseikuzumaki.signalinglib.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

/**
 * WebRTC シグナリングサーバーの RPC サービスインターフェース
 * クライアントがシグナリングサーバーに対して行う操作を定義する
 */
@Rpc
interface ISignalingService {

    /**
     * クライアントA が ウェイティングリストに自身を登録して、Offer を待機する
     *
     * @param request セッションの詳細情報
     * @return SDP Offer
     */
    fun waitForOffer(request: SessionRequest): Flow<SignalingOffer>

    /**
     * クライアントB が待機中のクライアントA に Offer を送信し、Answer を待ち受ける
     * @param offer クライアントから送信されたSDP Offer
     * @return 対応するSDP AnswerとICE Candidate
     */
    fun offer(offer: SignalingOffer): Flow<SignalingAnswer>

    /**
     * クライアントA がクライアントB に Answer を送信し、Candidate を待ち受ける
     *
     * @param answer SDP Answer
     * @return リモートデバイスの ICE Candidate. これを setRemoteDescription 後に addIceCandidate で追加する
     */
    fun answer(answer: SignalingAnswer): Flow<SignalingCandidate>

    /**
     * 自身の通信経路が変わったときに、相手に新しい Candidate を送信する
     * @param candidate クライアントから送信されたICE Candidate情報
     */
    fun putIceCandidates(candidate: SignalingCandidate): Flow<Unit>
}