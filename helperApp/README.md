# ATV Remote Helper App

リモートAPKインストール用のヘルパーアプリケーション

## 概要

このアプリは、CI/CDパイプラインから配信される開発版APKを自動的に受信し、インストールを補助するヘルパーアプリケーションです。テスト端末に常駐し、Firebase Cloud Messaging (FCM)を介して新しいビルドの通知を受け取ります。

## 主な機能

### FR-H-01: デバイス登録
- アプリ起動時にFCMトークンを取得
- バックエンドサーバーに自動登録

### FR-H-02: バックグラウンド動作
- フォアグラウンドサービスとして常時待機
- FCMデータメッセージを受信

### FR-H-03: 権限管理
- 「不明なアプリのインストール」権限の要求
- 設定画面への誘導

### FR-H-04: APKダウンロード
- S3署名付きURLからAPKをダウンロード
- アプリ内部ストレージ（キャッシュ）に保存

### FR-H-05: ダウンロード進捗表示
- OS通知バーに進捗表示
- 完了時の通知

### FR-H-06: インストール処理
- 通知タップでインストール開始
- FileProviderを使用

### FR-H-07: システムダイアログ
- `Intent.ACTION_INSTALL_PACKAGE`を発行
- OSのインストール確認ダイアログを表示

### FR-H-08: ビルド履歴（任意機能）
- 受信したビルドの履歴を表示
- ブランチ名、バージョン、インストール日時

## セットアップ

### 1. Firebase設定

`google-services.json`ファイルを`helperApp/`ディレクトリに配置してください。

### 2. ビルド

```bash
./gradlew :helperApp:assembleDebug
```

### 3. インストール

初回のみ手動インストールが必要です：

```bash
adb install helperApp/build/outputs/apk/debug/helperApp-debug.apk
```

### 4. アプリ設定

1. アプリを開く
2. 設定アイコンをタップ
3. サーバーURLを入力（例: `https://your-server.com`）
4. 保存
5. 「Check Install Permission」をタップして権限を許可

## 使用方法

### 初回セットアップ

1. アプリをインストール
2. 通知権限を許可
3. サーバーURLを設定
4. インストール権限を許可

### 通常使用

1. 新しいビルドがプッシュされると通知を受信
2. 通知をタップしてダウンロード進捗を確認
3. ダウンロード完了後、通知をタップしてインストール
4. システムダイアログで「インストール」をタップ

## 画面構成

### メイン画面（ビルド履歴）

- 受信したビルドの一覧
- バージョン、ブランチ、コミットハッシュ
- インストール状態

### 設定画面

- サーバーURL設定
- デバイスID表示
- FCMトークン表示
- インストール権限確認

## 技術仕様

### 使用ライブラリ

- **Firebase Cloud Messaging**: プッシュ通知
- **Ktor Client**: APKダウンロード
- **Jetpack Compose**: UI
- **kotlinx.serialization**: データシリアライゼーション

### 権限

- `INTERNET`: ネットワーク通信
- `POST_NOTIFICATIONS`: プッシュ通知（Android 13+）
- `REQUEST_INSTALL_PACKAGES`: APKインストール
- `FOREGROUND_SERVICE`: フォアグラウンドサービス
- `FOREGROUND_SERVICE_DATA_SYNC`: データ同期サービス
- `WAKE_LOCK`: スリープ中の受信

### ファイル構成

```
helperApp/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── kotlin/tokyo/isseikuzumaki/atvremote/helper/
│   │   ├── HelperApplication.kt          # アプリケーションクラス
│   │   ├── MainActivity.kt               # メインUI
│   │   ├── model/
│   │   │   └── Models.kt                 # データモデル
│   │   ├── service/
│   │   │   ├── ApkInstallMessagingService.kt  # FCMサービス
│   │   │   └── ApkDownloadService.kt          # ダウンロードサービス
│   │   ├── repository/
│   │   │   └── DeviceRepository.kt       # デバイス管理
│   │   ├── utils/
│   │   │   └── ApkDownloader.kt          # ダウンロードユーティリティ
│   │   └── ui/
│   │       └── theme/
│   │           └── Theme.kt              # Composeテーマ
│   └── res/
│       ├── values/
│       │   └── strings.xml               # 文字列リソース
│       └── xml/
│           └── file_paths.xml            # FileProvider設定
```

## トラブルシューティング

### 通知が届かない

- FCMトークンが登録されているか確認
- 通知権限が許可されているか確認
- バックエンドサーバーのログを確認

### ダウンロードが失敗する

- インターネット接続を確認
- S3 URLの有効期限を確認
- ストレージ容量を確認

### インストールができない

- インストール権限が許可されているか確認
- APKファイルが完全にダウンロードされたか確認
- ストレージ容量を確認

## セキュリティ

- すべての通信はHTTPSで暗号化
- APKはプライベートS3バケットに保存
- ダウンロードURLは時限付き署名URL
- インストールにはユーザーの明示的な承認が必要

## ライセンス

このアプリケーションはATV-Remoteプロジェクトの一部であり、同じライセンスに従います。
