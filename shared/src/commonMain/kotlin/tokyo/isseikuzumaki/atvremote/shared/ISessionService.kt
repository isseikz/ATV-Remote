package tokyo.isseikuzumaki.atvremote.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class SessionName(val value: String)

@Serializable
data class Session(
    val id: SessionID,
    val name: SessionName,
    val capabilities: List<String>
)

@Serializable
data class SessionRequest(
    val name: SessionName
)

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
