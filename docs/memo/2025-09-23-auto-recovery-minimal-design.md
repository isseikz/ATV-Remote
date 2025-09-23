# 自動復旧機能設計メモ（最小実装版）

**日付**: 2025-09-23
**作成者**: Claude Code
**実装予定工数**: 1-2時間

## 概要

要求仕様の「自動復旧機能」を最小限の実装で実現する設計案。既存コードへの影響を最小化し、短時間で実装可能な内容に絞った。

## 実装対象

### ✅ 対象
- **クライアント側**: WebRTC接続失敗時の自動再接続
- **クライアント側**: RPC接続断線時の自動再接続（HttpClientレベル）
- **サーバー側**: VideoSource/PeerConnectionリソースの適切な管理
- **サーバー側**: 接続断線時のリソース解放

### ❌ 対象外（後回し）
- ADB接続監視
- 複雑な状態管理UI
- 接続状態可視化

## 実装内容

### 1. WebRTC自動再接続

**対象ファイル**: `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/atvremote/viewmodel/AppViewModel.kt`

**現状の問題**: Line 196-197でWebRTC失敗時の処理が空実装

**実装内容**:
```kotlin
// onIceConnectionStateChange内の修正
IceConnectionState.Failed -> {
    Logger.w(TAG, "WebRTC connection failed, attempting reconnect...")
    viewModelScope.launch {
        delay(3000) // 3秒待機
        reopenVideo() // 新規追加する再接続関数
    }
}

IceConnectionState.Disconnected -> {
    Logger.w(TAG, "WebRTC disconnected, attempting reconnect...")
    viewModelScope.launch {
        delay(1000) // 1秒待機
        reopenVideo()
    }
}

// 新規追加: 再接続用ヘルパー関数
private var isReconnecting = false

private fun reopenVideo() {
    if (isReconnecting) return // 重複実行防止
    isReconnecting = true

    try {
        _activeVideo.value = null // 現在の接続をクリア
        openVideo() // 既存関数を再利用
    } finally {
        isReconnecting = false
    }
}
```

### 2. RPC自動再接続

**対象ファイル**:
- `composeApp/build.gradle.kts` (依存関係追加)
- `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/atvremote/viewmodel/AppViewModel.kt` (HttpClient設定)

**実装内容**:
```kotlin
// build.gradle.kts commonMain.dependencies に追加
implementation("io.ktor:ktor-client-plugins-retry:$ktor_version")

// AppViewModel.kt の rpcClient 修正
val rpcClient by lazy {
    HttpClient {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            retryIf { _, httpResponse ->
                !httpResponse.status.isSuccess()
            }
            delayMillis { retry -> retry * 1000L } // 1秒、2秒、3秒
        }
        installKrpc {
            waitForServices = true
        }
    }.rpc {
        // 既存設定
        url {
            host = SERVER_DOMAIN
            port = SERVER_PORT
            path("rpc")
        }
        rpcConfig {
            serialization {
                json()
            }
        }
    }
}
```

## サーバー側実装

### 3. WebRTCリソース管理

**対象ファイル**: `server/src/main/kotlin/tokyo/isseikuzumaki/atvremote/client/WebRTCClientImpl.kt`

**現状の問題**: Line 198でVideoSourceを開始するが停止処理なし、Line 239-244の接続状態監視が未実装

**実装内容**:
```kotlin
class WebRTCClientImpl(...) : ISignalingClient {
    private var videoSource: VideoDeviceSource? = null
    private var isActive = false

    // 新規追加: リソース解放
    fun cleanup() {
        Logger.d(TAG, "Cleaning up WebRTC client resources")
        isActive = false
        videoSource?.stop()
        videoSource = null
        localConnection.value?.close()
        localConnection.value = null
    }

    // CandidateCollector内の接続状態監視強化
    override fun onIceConnectionChange(state: RTCIceConnectionState) {
        Logger.d(TAG, "onIceConnectionChange: $state")
        when (state) {
            RTCIceConnectionState.FAILED -> {
                Logger.w(TAG, "Server-side ICE connection failed, cleaning up")
                cleanup()
            }
            RTCIceConnectionState.DISCONNECTED -> {
                Logger.w(TAG, "Server-side ICE connection disconnected")
            }
            RTCIceConnectionState.CLOSED -> {
                Logger.d(TAG, "Server-side ICE connection closed, cleaning up")
                cleanup()
            }
            else -> {
                // ログのみ
            }
        }
    }
}
```

## 実装手順

### Step 1: RPC再接続（15分）
1. `composeApp/build.gradle.kts`にktor-client-plugins-retry追加
2. `AppViewModel.kt`のrpcClientにHttpRequestRetry設定追加

### Step 2: クライアント側WebRTC再接続（30分）
1. `AppViewModel.kt`に`reopenVideo()`関数追加
2. `onIceConnectionStateChange`内のFailed/Disconnected処理実装
3. 重複実行防止フラグ`isReconnecting`追加

### Step 3: サーバー側リソース管理（45分）
1. `WebRTCClientImpl.kt`に`cleanup()`関数追加
2. `CandidateCollector`の接続状態監視を実装
3. VideoSourceの適切な停止処理追加

### Step 4: テスト（30分）
1. ネットワーク断線テスト
2. サーバー再起動テスト
3. リソースリーク確認

## 期待効果

- **クライアント側**: WebRTC接続失敗時に3秒後自動再接続
- **クライアント側**: WebRTC接続断線時に1秒後自動再接続
- **クライアント側**: RPC接続失敗時に最大3回リトライ（1-3秒間隔）
- **サーバー側**: 接続断線時のVideoSource自動停止でリソースリーク防止
- **サーバー側**: PeerConnection適切な解放で再接続時の競合回避
- 実装工数: 2時間（サーバー側追加により+30分）
- 既存コードへの影響: 最小限

## 将来の拡張案

この最小実装をベースに、必要に応じて以下を追加可能:

1. 接続状態のUI表示
2. より高度なBackoff戦略
3. ADB接続の監視と復旧
4. サーバー側セッション管理の強化
5. 接続品質メトリクスの収集

## 参考

- 要求仕様: `docs/requirements.md` 4.2 可用性要件
- 現在の実装: `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/atvremote/viewmodel/AppViewModel.kt:176-211`