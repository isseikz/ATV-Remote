# 映像転送機能 変更設計メモ (2025-09-22)

## 1. 背景 / ゴール
Android TV を遠隔操作しつつ、サーバー側で取得した映像をクライアント (Compose Multiplatform) に WebRTC で配信する仕組みを拡張。従来は単純な SDP オファー/アンサーと ADB 制御中心だったが、今回以下を導入・整理:
- セッション (映像配信待ちキュー) 管理
- サーバー内 WebRTC クライアント実装 (カメラ/キャプチャデバイス束ね)
- TURN/STUN 経由の NAT Traversal 設計基盤
- Signaling 抽象化 (Offer/Answer/Candidate を段階的にストリーミング)
- クライアント UI/VM のデバイス選択とセッション選択の分離

## 2. 変更概要サマリ
| 領域 | 主な変更 |
|------|-----------|
| Signaling モデル | SdpOffer/SdpAnswer から SignalingOffer/Answer/Candidate (クライアント側) へ拡張。サーバー側 WebRTCClientImpl は Flow<SdpAnswer> を返却し、初回 Answer + 追加 ICE Candidate 群を段階配信。|
| TURN/STUN | TurnServerService (Cloudflare Realtime TURN API) 追加。IceServer リスト生成で STUN + 動的 TURN を組み合わせる設計。|
| ViewModel | adb / signaling / session サービス分離。ADB デバイス選択と映像セッション選択を個別 StateFlow 管理。collectAsStateWithLifecycle へ移行。|
| UI | 2 種類のドロップダウン (Video / ADB) 追加。ボタン簡素化。|
| WebRTC (Server) | WebRTCClientImpl 新規: PeerConnection 構築、CandidateCollector で ICE 動的収集、callbackFlow で Answer 生成後 emitAll により ICE 追送。|
| セッション管理 | SessionManager.register: capabilities 初期化ロジック変更 (現状空 list 初期化 TODO)。|
| ビルド/構成 | server module に Ktor Client + JSON Serialization + .env ローダタスク追加。生成 DotEnv オブジェクトをソースセットへ注入。|

## 3. コンポーネント詳細
### 3.1 TurnServerService (Cloudflare)
- .env: CLOUDFLARE_REALTIME_TURN_APP_ID / CLOUDFLARE_REALTIME_TURN_API_TOKEN 追加。
- Ktor Client (cio + content-negotiation + kotlinx-json) で認証付き API 呼出しし、TURN 資格情報 (username, credential, urls) を取得し RTCConfiguration に反映予定。
- 既存エラーログ: username / credential 欠落で kotlinx.serialization MissingFieldException -> API レスポンスの一部が STUN Only の場合フィールドオプショナル化必要 (後述課題)。

### 3.2 WebRTCClientImpl
責務:
1. RTCConfiguration 構築 (STUN 静的 + TURN 動的)。
2. PeerConnectionFactory 生成 & VideoDeviceSource からトラック追加。
3. 受信 Offer を remoteDescription に設定し Answer を生成。
4. 生成直後の SDP Answer を即 emit。
5. ICE Gathering の進行に応じ CandidateCollector.candidates Flow を drop(1) して差分 SdpAnswer(answer, candidates追加分) を emitAll で逐次送出。
6. handlePutIceCandidates でリモート候補を適用。

パターン:
- answerToOffer: callbackFlow + Observer パターンを Flow 化。
- emitAll + map を併用し一つの Flow で Answer + Candidate バッチを段階提供。
- CandidateCollector: MutableStateFlow<List<RTCIceCandidate>> を蓄積。Gathering 完了時 State.Complete へ遷移 (今後: 完了通知で最終送信打ち切りなど拡張余地)。

### 3.3 AppViewModel (クライアント)
- rpcClient.withService による IAtvControlService / ISignalingService / ISessionService の遅延初期化。
- activeVideo -> selectedVideo, _activeDevice を _videoSession / _adbDevice に分離。
- openVideo(): PeerConnection 設定 (STUN リストのみ) + signaling.offer(SignalingOffer) Flow collect で Answer/Candidate を分岐適用。
- ICE Candidate ローカル収集時 SignalingCandidate 経由で putIceCandidates 呼び出し。

### 3.4 SessionManager
- register() で capabilities を引数から採用せず空 mutableList に差し替え (※現状仕様不明確: 受領値を失っているため再検討必要)。

### 3.5 UI 層
- ExpandableDropdownMenu: label 追加, Button 常時表示, expanded 中は disabled。
- 2 つのドロップダウンでセッション選択 / ADB デバイス選択を独立。

### 3.6 ビルド & 環境変数取り込み
- loadVariables Gradle タスク: .env を読み込み DotEnv.kt 自動生成。
- main SourceSet に build/generated/source/dotEnv 追加。
- 課題: .env 自体が今回 diff に含まれておりシークレット露出。即時無効化/再発行推奨。リポジトリ履歴からの完全削除 (git filter-repo 等) も検討。

## 4. シーケンス (概略)
(1) クライアント: ユーザーが Video セッション選択 -> AppViewModel.openVideo()
(2) クライアント: PeerConnection 構築 + Offer 作成 + signaling.offer(SignalingOffer) 開始
(3) サーバー: WebRTCClientImpl.handleOffer -> PeerConnection 構築, answer 生成, Flow 最初の要素 emit
(4) サーバー: ICE Gathering 進行 -> CandidateCollector が candidates 更新 -> Flow に追加 SdpAnswer(answer, 新規候補群) emit
(5) クライアント: collect 中 SignalingAnswer.Answer を setRemoteDescription, Candidate を addIceCandidate
(6) 双方向: クライアント側 ICE 生成時 SignalingCandidate を送信
(7) メディア / DataChannel (未実装) 伝送開始

## 5. Flow / emitAll 設計ポイント
- handleOffer 内: answer (初回) + 追加 ICE を単一 Flow でストリーミングし、上位 Signaling 層の複雑度を低減。
- emitAll + drop(1): 初回状態(空 or 初期バッチ)を分離しその後の増分のみ送る典型パターン。
- 改善余地: CandidateCollector 側で channelClose トリガ (Complete 時) を発火し上流 cancel を容易化。

## 6. セキュリティ / 運用上の注意
| 項目             | 内容                                         |
|----------------|--------------------------------------------|
| TURN 資格情報キャッシュ | 現状 getTurnCredentials() 失敗時リトライ/フォールバックなし。 |
| 広範なログ          | ICE Candidate / SDP 全体ログはマスク検討。            |
| ADB 実行         | 任意 shell 実行可能なためアクセス制御未整備。                 |

## 7. 既知課題 / TODO
| 区分                   | 内容                                                                                             |
|----------------------|------------------------------------------------------------------------------------------------|
| Serialization        | Cloudflare TURN API レスポンス: username / credential 欠落ケースを optional に (MissingFieldException 発生)。 |
| SessionManager       | capabilities 入力無視 (仕様明確化 / 復元)。                                                                |
| Connection Lifecycle | 既存接続切替時のクリーンアップ未実装 (track stop / PeerConnection close)。                                        |
| エラーハンドリング            | signaling.offer collect 中の例外伝播 / 再試行戦略未定義。                                                     |
| ICE 完了検知             | CandidateCollector 完了後 Flow cancel して不要送信抑制。                                                   |
| TURN 未使用時 fallback   | TURN 取得失敗で STUN のみ継続する明示処理。                                                                    |
| UI                   | 接続状態 (Connecting/Gathering/Connected) の視覚化未対応。                                                 |
| 帯域制御                 | Video キャプチャ解像度/ビットレートの調整なし。                                                                    |
| DataChannel          | 入力制御 (DPAD 等) を WebRTC DataChannel 化検討余地。                                                      |

## 8. 今後の拡張案
1. Answer/Candidate を統一イベント (Sealed Interface) にし JSON-RPC/WS 双方向 Push 最適化。
2. Candidate Trickle 終了通知 (end-of-candidates) 対応。
3. 映像エンコードパラメータ (VP8/VP9/H264) 設定追加。
4. 指標収集 (RTCP stats) で品質モニタリング。
5. Multi-session / Multi-track (音声追加) サポート。
6. ADB 経由スクリーンキャプチャも WebRTC Video Track (Virtual Source) 化。

## 9. まとめ
今回の差分で映像転送の骨格 (Signaling + WebRTC PeerConnection + STUN/TURN 下準備) が整備された。接続ライフサイクル管理・セキュリティ・可観測性・エラーハンドリングを次段で補強することで安定化が可能。特に TURN 資格情報の optional 化と既存接続クリーンアップが早期課題。
