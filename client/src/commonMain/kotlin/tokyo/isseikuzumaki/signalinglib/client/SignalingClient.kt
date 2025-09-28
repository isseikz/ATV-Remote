package tokyo.isseikuzumaki.signalinglib.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.path
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import tokyo.isseikuzumaki.signalinglib.shared.ISignalingService
import tokyo.isseikuzumaki.signalinglib.shared.ISessionService

/**
 * WebRTCシグナリングクライアント
 * サーバーとの通信を管理し、WebRTCシグナリングを処理する
 */
class SignalingClient(
    private val serverHost: String,
    private val serverPort: Int
) {
    private val rpcClient by lazy {
        HttpClient {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                retryIf { _, httpResponse ->
                    httpResponse.status.value >= 500
                }
                delayMillis { retry -> retry * 1000L } // 1秒、2秒、3秒
            }
            installKrpc {
                waitForServices = true
            }
        }.rpc {
            url {
                host = serverHost
                port = serverPort
                path("rpc")
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    val signalingService by lazy { rpcClient.withService<ISignalingService>() }
    val sessionService by lazy { rpcClient.withService<ISessionService>() }

    /**
     * クライアントを閉じる
     */
    fun close() {
        rpcClient.close()
    }
}