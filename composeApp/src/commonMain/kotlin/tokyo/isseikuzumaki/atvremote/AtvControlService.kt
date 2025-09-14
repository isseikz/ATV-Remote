package tokyo.isseikuzumaki.atvremote

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.AdbCommandResult
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateResponse
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer

/**
 * クライアント → サーバー への RPC サービス
 * WebRTC シグナリングとADBコマンド実行を担当
 */
@Rpc
interface AtvControlService {
    /**
     * WebRTC接続を開始するため、クライアントのSDP Offerをサーバーに送信します。
     * @param offer SDP Offer情報を含むデータクラス。
     */
    suspend fun sendSdpOffer(offer: SdpOffer): Flow<SdpAnswer>

    /**
     * クライアント側で発見されたICE Candidateをサーバーに送信します。
     * @param candidate ICE Candidate情報を含むデータクラス。
     */
    suspend fun sendIceCandidate(candidate: IceCandidateData): Flow<IceCandidateResponse>

    /**
     * 指定されたADBコマンドの実行をサーバーに要求します。
     * @param command 実行したいADBコマンド。
     */
    suspend fun sendAdbCommand(command: AdbCommand): Flow<AdbCommandResult>
}
