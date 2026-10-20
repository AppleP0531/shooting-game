# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

縦画面の Java Swing 製シューティングゲーム(Spring Boot 4 + JPA/MySQL でセーブデータを持つ)。コメント・ログ・UI文言・TODOは日本語。Java 25 / Maven。

## コマンド

JAVA_HOME は JDK 25(README 参照)。Windows では `.\mvnw.cmd`、Git Bash では `./mvnw`。

```bash
./mvnw -q -o compile                 # コンパイル確認(-o はオフライン。普段これで足りる)
./mvnw clean spring-boot:run         # 起動(Swing のウィンドウが開く)
./mvnw test -Dtest=クラス名 -Djava.awt.headless=true   # テスト(-o は付けない: オフラインだと surefire が無く失敗する)
```

- 起動時に Spring Boot で DB(AWS RDS / MySQL。`src/main/resources/application.properties` は git 管理外)へつなぐ。**つながらなければ自動でオフラインモード**(`DataManager.load("guest", "create")`、ログインなし)になる。DB なしの動作確認はこれで足りる。
- `ShootingGameApplicationTests`(コンテキスト読み込み)は DB が必要。単体テストは DB・画面なしで動く純ロジックだけを対象にする。
- 公式のリンタ・フォーマッタ設定はない。

## アーキテクチャ(複数ファイルを読まないと分からない部分)

### 起動とシーン
- `ShootingGameMain`(JPanel + ゲームループ + CardLayout)が起動を担当。別スレッドで「Spring 起動 → DataManager → `ResourceManager` → 各シーンの `initComponents()` → 全シーンを CardLayout へ登録」を行い、完了までは `LoadProgress` の進捗バーだけを描く。
- 画面は `SceneManager.SceneType`(TITLE/LOBBY/BAG/UPGRADE/SHOP/SELECTSTAGE/NORMALSTAGE/INFINITYSTAGE/EXTRA_*/SETTING/GAME/GAMERESULT/TEST/ENCYCLOPEDIA…)ごとに、`Manager` インターフェース(`setup` / `update(dt)` / `draw(g)` / `initComponents`)を実装したシングルトンが1つずつ。`changeScene` が遷移履歴を積み、`changeToPrevScene` で戻る。シーンに入るたびに `setup()` が呼ばれるので、画面の状態はそこで作り直す。
- シーンを足すときは、`SceneType` に追加し、`SceneManager` のコンストラクタで登録する(`initComponents` と CardLayout への登録は自動)。
- 画面サイズ依存の数値はすべて `ShootingGameMain.getScreenSizeAdj()`(倍率)を掛ける。ハードコードのピクセル値は崩れる原因になる。
- 全画面の上に重なるコマンド/チャット欄は `CommandGlassPane`(GlassPane)。

### 設定駆動のゲーム内容
敵・ボス・弾・ステージ・アイテム・スキル等は `src/main/resources/static/configs/*.json`(ShipConfig / StageConfig / BulletConfig / BossConfig / ItemConfig …)で定義され、`ShipManager` / `StageManager` / `BulletManager` などが Jackson で読む。画像は `static/img/**` にあり、**スプライトシートは `grids` と `useCount` が画像の実際のコマ数と合っていること**(合わないとシート全体が1枚で描かれる)。`/reload` で設定と画像を読み込み直せる。
- ボスの見た目は `BossLook`(形・色・形態ごとに増える部品)が定義。`img/ship/boss/*_f0〜f4.png` はこの定義から描いた4x2・6コマのシート(画像がなければ `Boss.drawPlaceholder` が図形で描く)。
- ステージのギミック(ビームを撃つブロック `BlockSpec`、ビーム `BeamSpec`)は StageConfig の `blocks` / `gimmicks`。ブロックは `spin` で回転し、ビーム・当たり判定・押し出しも回転に追従する。

### ゲーム内
- `GameManager` が一連のマネージャー(`StageManager` → `BulletManager` → `BeamManager` → `ShipManager` → `ItemManager` → `DamagePopupManager`)を**この順で**更新・描画する(描画順が重なり順)。船は `Ship`(Player / Enemy / Boss / SubShip)。ダメージは `Ship.addDamage` が起点(ここからダメージ数字が出る)。
- クリア/ゲームオーバーの精算は `GameManager.result()`(報酬・初クリア・難易度ミッション・ハイスコア)。

### データとセーブ
- `DataManager`(static)が現在の `UserData`(進捗 `Progress`、資源、船、モジュール…)を持ち、`UserDataMapper` で JPA の `UserEntity` と相互変換する。**保存はウィンドウを閉じる時(`windowClosing`)にまとめて1回**で、各サービスは操作のたびに保存しない(クラッシュ・強制終了では直近の操作が失われる)。
- 追加のフラグ類は DB のテーブルを増やさず、`Progress.highScores`(`Map<String,Integer>`)に接頭辞つきキーで相乗りさせる流儀がある(`daily:`、`endless:wave`、`seen:<船ID>`=図鑑)。
- 端末ごとの設定(FPS、ダメージ表記)は `GameSettings` が実行フォルダの `settings.properties`(git 管理外)に保存する。ユーザーデータとは別。
- ゲームのルール計算は `domain/*Service`(スキル・ユニット・モジュール・サブ機・変換・コスト・図鑑)の static メソッドに置く流儀。UI(`*Panel`)はそれを呼ぶだけ。

### コマンド
`command/` に `CommandGroup` のサブクラス(System / Game / PlayerData)があり、`register(...)` で `rootCommands` に木構造で登録する。4つ目の引数 `true` が「管理者のみ」。出力は `info` / `success` / `error` / `usage` を使う(ログ欄=`LogLine` の種別と、コンソールの両方へ出る)。コマンド一覧は `command.md`。

### 開発メモの置き場
`todo_list.md`(完了は行頭の「〇」)、`UML.puml`(クラス図。構成を変えたら更新)、`command.md`、`shootingGame.md`(環境メモ)。

## 作業上の注意
- 大きな機能は、実装前に方針をユーザーに確認する運用になっている(選択肢を出して決めてもらう)。
- 大きな変更は、サブエージェントに分けて並行で進める運用になっている(ユーザーが依頼したとき)。分け方の決まり:
  - 担当ファイルが重ならないように分ける(同じファイルを2つのエージェントに触らせない)。
  - 先に自分が共通の土台(新しい型や API の骨組み)を作ってコンパイルを通し、各エージェントにはそのシグネチャを正確に伝える。
  - 各エージェントには「触ってよいファイル」「コミットしない」「他の人が同時に別のファイルを編集している」ことを明記する。
  - 全員の終了後に、全体のコンパイルと、触った範囲が重なっていないかを自分で確認する。エージェントの報告は鵜呑みにしない。
  - UML・テスト・ドキュメント(`UML.puml` / `src/test` / `command.md`)の更新は、機能の実装と並行して別エージェントに任せてよい。
- 見た目・描画まわりの変更は、コンパイルが通っても実際の見た目は保証されない。起動して確認するか、未確認と明記する。
