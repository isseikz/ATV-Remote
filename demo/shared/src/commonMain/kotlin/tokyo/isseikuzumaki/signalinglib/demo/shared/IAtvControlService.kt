package tokyo.isseikuzumaki.signalinglib.demo.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

/**
 * クライアント → サーバー への RPC サービス
 * ADBコマンド実行とAndroid TV制御を担当
 *
 * TODO: セッションによって adb 接続を分ける
 */
@Rpc
interface IAtvControlService {
    /**
     * adb デバイスのリストを取得します。
     */
    fun adbDevices(): Flow<List<AdbDevice>>

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