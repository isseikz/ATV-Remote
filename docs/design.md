# 設計仕様：Signaling Library & ATV-Remote Demo

## バージョン履歴

- 2025-09-28: ライブラリ分離アーキテクチャ設計版 (isseikz)
- 2025-09-22: 初版作成 (isseikz)

## 概要 🎯

本プロジェクトは、汎用的なWebRTCシグナリングライブラリ（`signalinglib`）とそのデモアプリケーションであるATV-Remoteから構成されます。WebRTC通信とkotlinx-rpcを活用したマルチプラットフォーム対応により、再利用可能なシグナリング基盤を提供します。

### プロジェクトの目的

1. **汎用WebRTCライブラリの提供**: 他のWebRTCプロジェクトで再利用可能なシグナリング機能
2. **デモアプリケーション**: ATV-Remoteによる実用的な活用例の提示
3. **テンプレート化**: 新しいWebRTCアプリケーション開発のベースフレームワーク

## ライブラリ分離アーキテクチャ 🏗️

### モジュール構造

```
signalinglib/ (Root project: tokyo.isseikuzumaki.signalinglib)
├── shared/           # 汎用シグナリング共通コード
│   └── src/commonMain/kotlin/tokyo/isseikuzumaki/signalinglib/shared/
│       ├── ISignalingService.kt      # WebRTCシグナリングインターフェース
│       ├── ISessionService.kt        # セッション管理インターフェース
│       └── SignalingModels.kt        # シグナリング関連データモデル
├── server/           # シグナリングサーバーライブラリ
│   └── src/main/kotlin/tokyo/isseikuzumaki/signalinglib/server/
│       ├── SignalingServiceImpl.kt   # シグナリングサービス実装
│       ├── SessionManager.kt         # セッション管理実装
│       └── plugins/                  # 汎用Ktorプラグイン
├── client/           # シグナリングクライアントライブラリ
│   └── src/commonMain/kotlin/tokyo/isseikuzumaki/signalinglib/client/
│       ├── SignalingClient.kt        # クライアントサイド実装
│       └── WebRTCWrapper.kt          # WebRTC-KMPラッパー
└── demo/            # デモアプリケーション（ATV-Remote）
    ├── shared/       # デモアプリ専用共通コード
    │   └── src/commonMain/kotlin/tokyo/isseikuzumaki/signalinglib/demo/shared/
    │       ├── IAtvControlService.kt # ADB制御インターフェース
    │       └── AtvModels.kt          # ATV固有データモデル
    ├── server/       # デモアプリサーバー
    │   └── src/main/kotlin/tokyo/isseikuzumaki/signalinglib/demo/server/
    │       ├── AtvControlServiceImpl.kt # ADB制御実装
    │       ├── AdbManager.kt            # ADBマネージャー
    │       └── Application.kt           # メインサーバーアプリ
    └── composeApp/   # デモアプリUI
        └── src/commonMain/kotlin/tokyo/isseikuzumaki/signalinglib/demo/
            ├── App.kt                # メインUI
            ├── viewmodel/            # ViewModel
            └── components/           # UI コンポーネント
```

### 責務分離

#### 汎用ライブラリ部分 📚

**signalinglib:shared**
- WebRTCシグナリングの標準的なインターフェース定義
- セッション管理の基本機能
- 他のWebRTCアプリケーションで再利用可能な基盤

**signalinglib:server**
- シグナリングサーバーの実装
- Ktorベースのサーバー基盤
- WebRTCシグナリングプロトコルの処理

**signalinglib:client**
- クライアントサイドWebRTC機能
- マルチプラットフォーム対応
- WebRTC-KMPのラッパー実装

#### アプリケーション固有部分 📱

**signalinglib:demo:shared**
- ATV-Remote固有のビジネスロジック
- ADB関連のデータモデル
- スクリーンショット等の専用機能

**signalinglib:demo:server**
- ADBデバイス管理
- Android TV固有の制御機能
- 汎用ライブラリを利用したアプリケーション実装

**signalinglib:demo:composeApp**
- ATV-Remote専用UI
- DPadコンポーネント等
- デバイス制御画面

### 依存関係

```
signalinglib:server → signalinglib:shared
signalinglib:client → signalinglib:shared
signalinglib:demo:server → signalinglib:server, signalinglib:demo:shared
signalinglib:demo:composeApp → signalinglib:client, signalinglib:demo:shared
```

## 技術スタック 🛠️

### 基盤技術

| カテゴリ       | 技術                         | バージョン  | 役割                                   |
|------------|----------------------------|---------|--------------------------------------|
| 言語         | Kotlin                     | 2.2.0   | プロジェクト全体の開発言語                        |
| プラットフォーム  | Kotlin Multiplatform (KMP) | 2.2.0   | マルチプラットフォーム開発基盤                      |
| サーバー      | Ktor                       | 3.2.1   | WebSocketサーバー、HTTP API                |
| UIフレームワーク | Compose Multiplatform      | 1.8.2   | 宣言的クロスプラットフォームUI                     |
| RPC        | kotlinx-rpc                | 0.9.1   | 型安全なRPC通信フレームワーク                     |

### WebRTC技術

| カテゴリ         | 技術                         | バージョン     | 役割                                   |
|--------------|----------------------------|------------|--------------------------------------|
| WebRTC (共通)  | webrtc-kmp                 | 0.125.11   | マルチプラットフォームWebRTC実装                  |
| WebRTC (Server) | webrtc-java               | 0.14.0     | サーバーサイドWebRTC、MediaDevices映像キャプチャ機能含む |
| WebRTC (iOS)  | WebRTC-SDK                | 125.6422.07| iOS向けネイティブWebRTC実装               |

### 技術選択の根拠

* **Kotlin Multiplatform (KMP)**: サーバー・クライアント間のコード共有により開発効率を向上し、型安全性を保証
* **kotlinx-rpc**: 型安全なRPC通信フレームワーク、サーバー・クライアント間の統一的なAPI定義
* **WebRTC**: P2P通信による超低遅延ストリーミングを実現
* **Compose Multiplatform**: 宣言的UIによる保守性の高いクロスプラットフォーム開発（Android、iOS、Web対応）

## システムアーキテクチャ（デモ：ATV-Remote） 📺

デモアプリケーションであるATV-Remoteは、マルチプラットフォームクライアント、Ktorサーバー、Android TV デバイス、そしてHDMIキャプチャデバイスの4コンポーネントで構成されます。

![システムアーキテクチャ図](./images/architecture_20250922.jpeg)

### 映像ストリーミングフロー

1. **映像キャプチャ**: Android TV デバイスのHDMI出力がUSBキャプチャデバイスに入力
2. **映像取得**: サーバーがwebrtc-javaのMediaDevicesでキャプチャデバイスから映像を取得
3. **WebRTCシグナリング**: クライアントが`ISignalingService`を通じてSDP offer/answerを交換
4. **P2P接続確立**: WebRTCのPeerConnection確立、ICE候補交換
5. **映像配信**: サーバーからクライアントにWebRTCストリームを直接送信
6. **映像表示**: クライアントがCompose Video要素で映像をレンダリング

### デバイス制御フロー

1. **デバイス発見**: クライアントが`IAtvControlService.adbDevices()`でデバイス一覧を取得
2. **コマンド送信**: クライアントが`sendAdbCommand()`でリモート操作コマンドを送信
3. **ADB実行**: サーバーの`AdbManager`がプロセス経由でadbコマンドを実行
4. **結果返却**: ADB実行結果を`Flow<AdbCommandResult>`でクライアントに返却

## 主要コンポーネント 🔧

### 汎用ライブラリコンポーネント

**シグナリング基盤**
* `ISignalingService`: WebRTCシグナリングインターフェース
* `SignalingServiceImpl`: シグナリングサービス実装
* `SessionManager`: WebRTC セッション管理
* `ISessionService`: セッション管理インターフェース

**クライアントライブラリ**
* `SignalingClient`: クライアントサイドシグナリング実装
* `WebRTCWrapper`: WebRTC-KMPラッパー

### デモアプリケーション固有コンポーネント

**サーバーサイド (Ktor JVM)**
* `AdbManager`: ADBコマンド実行・デバイス管理
* `AtvControlServiceImpl`: ATV制御RPC サービス実装
* `WebRTCClientImpl`: サーバーサイドWebRTC実装

**クライアントサイド (Compose Multiplatform)**
* `AppViewModel`: UI状態管理・RPC通信制御
* `Video`: プラットフォーム固有の映像レンダリング
* `DPadComponent`: Android TV リモートコントロール UI
* `ExpandableDropDown`: デバイス選択UI


## 期待される効果 🎯

### 開発効率の向上
* **再利用性**: 他のWebRTCプロジェクトでライブラリ部分を簡単に再利用
* **テンプレート化**: 新しいWebRTCアプリケーション開発のベースとして活用
* **保守性**: 汎用機能とアプリ固有機能が明確に分離

### コード品質の向上
* **関心の分離**: 各モジュールの責務が明確
* **テスト性**: ライブラリ部分を独立してテスト可能
* **拡張性**: 新しいアプリケーションをdemoと同様の構造で追加可能

## 実装上の制約と考慮事項 ⚠️

### 技術的制約

* WebRTCシグナリングサーバーの実装とP2P接続管理の複雑性
* USBキャプチャデバイスの性能制限とエンコード処理負荷
* プラットフォーム間でのWebRTC実装差異（特にiOS WebRTC-SDK）
* macOS aarch64でのwebrtc-java依存性

### セキュリティ考慮事項

* イントラネット利用が前提だが、システムへのアクセス制御が必要
* ADBによる強力なデバイス操作権限の管理
* 映像ストリームの暗号化と認証
* RPC通信のセキュリティ確保

## 開発コマンド 🚀

### ビルドコマンド

**ライブラリ部分:**
```bash
./gradlew :shared:build
./gradlew :server:build
./gradlew :client:build
```

**デモアプリケーション:**
```bash
# サーバー
./gradlew :demo:server:run

# Android アプリ
./gradlew :demo:composeApp:assembleDebug

# Web アプリ
./gradlew :demo:composeApp:wasmJsBrowserDevelopmentRun
```

### テストコマンド
```bash
./gradlew test
./gradlew :demo:composeApp:testDebugUnitTest  # Android tests
./gradlew :shared:commonTest                   # Shared module tests
```

### 全体ビルド
```bash
./gradlew build
```