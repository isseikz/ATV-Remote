package tokyo.isseikuzumaki.signalinglib.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

/**
 * 待機中のセッションを管理するサービスインターフェース
 */
@Rpc
interface ISessionService {
    /**
     * 待機中のセッションIDのリストを取得する
     * @return 待機中のセッションIDのリスト
     */
    fun waitingSessions(): Flow<List<Session>>
}