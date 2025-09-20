package tokyo.isseikuzumaki.atvremote.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

/**
 * クライアント → サーバー への RPC サービス
 * WebRTC シグナリングとADBコマンド実行を担当
 */
@Rpc
interface AtvControlService {
    /**
     * adb デバイスのリストを取得します。
     */
    fun adbDevices(): Flow<List<AdbDevice>>

    /**
     * WebRTC接続を開始するため、クライアントのSDP Offerをサーバーに送信します。
     * @param offer SDP Offer情報を含むデータクラス。
     */
    fun sendSdpOffer(deviceId: DeviceId, offer: SdpOffer): Flow<SdpAnswer>

    /**
     * クライアント側で発見されたICE Candidateをサーバーに送信します。
     * @param candidate ICE Candidate情報を含むデータクラス。
     */
    fun sendIceCandidate(deviceId: DeviceId, candidate: IceCandidateData): Flow<IceCandidateData>

    /**
     * 指定されたADBコマンドの実行をサーバーに要求します。
     * @param command 実行したいADBコマンド。
     */
    fun sendAdbCommand(deviceId: DeviceId, command: AdbCommand): Flow<AdbCommandResult>

    /**
     * 指定されたデバイスのスクリーンショットを取得します。
     * @param deviceId スクリーンショットを取得するデバイスのID
     * @return スクリーンショットのバイナリデータ
     */
    fun takeScreenshot(deviceId: DeviceId): Flow<ScreenshotResult>
}
