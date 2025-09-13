# ATV-Remote Server

## 概要

ATV-Remote-KMPプロジェクトのサーバーサイド実装です。kotlinx-rpcを使用して型安全なWebSocket通信を提供し、Android TVデバイスのリモート制御とWebRTCストリーミングを実現します。

## 実装済み機能

### 1. RPC サービス
- **AtvControlService**: クライアントからサーバーへのRPC通信
  - WebRTC SDP Offer/Answer交換
  - ICE Candidate交換
  - ADBコマンド実行

- **ClientNotificationService**: サーバーからクライアントへの通知
  - WebRTC応答通知
  - ADBコマンド実行結果通知

### 2. 主要コンポーネント

#### AdbManager
- ADBコマンドの実行とデバイス管理
- エラーハンドリングとタイムアウト制御
- 非同期実行サポート

#### WebRTCSignalingManager
- WebRTCシグナリングセッション管理
- SDP Offer/Answer生成
- ICE Candidate管理

#### VideoCaptureManager
- USBキャプチャデバイスからの映像取得
- 30FPS での連続キャプチャ
- 複数クライアントへの同時配信

#### ClientNotificationManager
- クライアントセッション管理
- 通知の配信とエラーハンドリング
- シングルトンパターンによる状態管理

## サーバー起動

```bash
# 開発モード
./gradlew server:run

# プロダクションビルド
./gradlew server:build
java -jar server/build/libs/server-1.0.0-all.jar
```

## エンドポイント

- **WebSocket RPC**: `ws://localhost:8080/rpc`
- **静的ファイル**: `http://localhost:8080/`

## 前提条件

1. **ADB環境**
   ```bash
   # ADBがインストールされ、PATHに設定されていること
   adb devices
   ```

2. **USBキャプチャデバイス**
   - システムに認識されるUSBキャプチャデバイスが接続されていること
   - webcam-capture ライブラリでアクセス可能であること

3. **Android TV/Chromecast**
   - 開発者向けオプションが有効
   - ネットワークデバッグが有効
   - adb connect で接続済み

## ログ出力例

```
Video capture started successfully
Client registered: client-1703123456789
Received SDP Offer from client client-1703123456789: offer
SDP Answer sent to client client-1703123456789 and video streaming started
Received ICE Candidate from client client-1703123456789
ICE Candidate processed for client client-1703123456789
Received ADB Command from client client-1703123456789: input keyevent KEYCODE_HOME
ADB Command executed for client client-1703123456789: success=true
Video frame captured for session client-1703123456789: 524288 bytes
```

## アーキテクチャ

```
┌─────────────────────┐    ┌──────────────────────┐
│   Ktor Application  │────│  kotlinx-rpc Server │
└─────────────────────┘    └──────────────────────┘
           │                           │
           │                           │
    ┌──────▼──────┐               ┌────▼────┐
    │ WebSockets  │               │   RPC   │
    │   Plugin    │               │ Service │
    └─────────────┘               └─────────┘
           │                           │
    ┌──────▼──────┐               ┌────▼────┐
    │   Routing   │               │  Impl   │
    └─────────────┘               └─────────┘
           │                           │
    ┌──────▼──────┐               ┌────▼────┐
    │   Static    │               │ Manager │
    │    Files    │               │ Classes │
    └─────────────┘               └─────────┘
```

## 開発・デバッグ

### 単体テスト実行
```bash
./gradlew server:test
```

### ログレベル調整
`src/main/resources/logback.xml` で設定可能

### 設定変更
- ポート番号: `Application.kt` の `port = 8080`
- WebSocket設定: `plugins/RPC.kt` の `configureRPC()`
- ビデオ設定: `VideoCaptureManager.kt` の解像度・FPS設定
