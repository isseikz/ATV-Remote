package tokyo.isseikuzumaki.signalinglib.demo.server.service.signaling

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.signalinglib.shared.IceCandidateData
import tokyo.isseikuzumaki.signalinglib.shared.SdpAnswer
import tokyo.isseikuzumaki.signalinglib.shared.SdpOffer


/**
 * Signaling サーバー内で用いる WebRTC クライアントの抽象インターフェース
 */
interface ISignalingClient {
    /**
     * リモートからの Offer を受け取り、Answer を返す
     * シグナリングサーバーがセッション情報を生成する
     * Answer には SDP と ICE Candidate を含む
     *
     * @param offer: リモートから送信された SDP Offer
     * @return: ローカルで生成した SDP Answer
     */
    fun handleOffer(offer: SdpOffer): Flow<SdpAnswer>

    /**
     * リモートからの ICE Candidate を受け取り、ローカルの PeerConnection の Candidate リストを上書きする
     *
     * @param candidate: リモートから送信された ICE Candidate 情報
     * @return: 追加された ICE Candidate 情報
     */
    fun handlePutIceCandidates(candidate: List<IceCandidateData>): Flow<Unit>
}
