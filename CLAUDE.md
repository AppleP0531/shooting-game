# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

縦画面の Java Swing 製シューティングゲーム(Spring Boot 4 + JPA/H2 でセーブデータを持つ)。コメント・ログ・UI文言・TODOは日本語。Java 25 / Maven。

## コマンド

JAVA_HOME は JDK 25(README 参照)。Windows では `.\mvnw.cmd`、Git Bash では `./mvnw`。

```bash
./mvnw -q -o compile                 # 軽い確認(下の注意のとおり、これだけだと再コンパイルされないことがある)
./mvnw clean spring-boot:run         # 起動(Swing のウィンドウが開く)
./mvnw test -Dtest=クラス名 -Djava.awt.headless=true   # テスト(-o は付けない: オフラインだと surefire が無く失敗する)
```

- **「BUILD SUCCESS」だけでは、コンパイルできたと言えない。** 差分コンパイルが何も再コンパイルせずに成功することがあり、実際にエラーを見逃した。確実に確認するには、`target/classes/com`・`target/test-classes`・`target/maven-status` を消してから実行し、出力に `Compiling N source files` が出ることを確認する(または `clean` を使う)。
- ゲームを起動したままだと `target` 内の画像が掴まれて `clean` が失敗する。その場合は上のフォルダだけを消し、`-Dmaven.resources.skip=true` を付けてコンパイル・テストする(リソースは前のコピーを使う)。
- 起動時に Spring Boot で DB(H2 のファイル。実行フォルダの `data/` にできる。接続は `src/main/resources/application.properties`(git 管理外)の `spring.datasource.*`)へつなぐ。テーブルは `ddl-auto=update` で自動作成される。**つながらなければ自動でオフラインモード**(`DataManager.load("guest", "create")`、ログインなし)になる。DB なしの動作確認はこれで足りる。
- `ShootingGameApplicationTests`(コンテキスト読み込み)は DB が必要。単体テストは DB・画面なしで動く純ロジックだけを対象にする。
- 公式のリンタ・フォーマッタ設定はない。

## アーキテクチャ(複数ファイルを読まないと分からない部分)

### 起動とシーン

- `ShootingGameMain`(JPanel + ゲームループ + CardLayout)が起動を担当。別スレッドで「Spring 起動 → DataManager → `ResourceManager` → 各シーンの `initComponents()` → 全シーンを CardLayout へ登録」を行い、完了までは `LoadProgress` の進捗バーだけを描く。
- 画面は `SceneManager.SceneType`(TITLE/LOBBY/BAG/UPGRADE/SHOP/SELECTSTAGE/NORMALSTAGE/INFINITYSTAGE/EXTRA\_\*/SETTING/GAME/GAMERESULT/TEST/ENCYCLOPEDIA…)ごとに、`Manager` インターフェース(`setup` / `update(dt)` / `draw(g)` / `initComponents`)を実装したシングルトンが1つずつ。`changeScene` が遷移履歴を積み、`changeToPrevScene` で戻る。シーンに入るたびに `setup()` が呼ばれるので、画面の状態はそこで作り直す。
- シーンを足すときは、`SceneType` に追加し、`SceneManager` のコンストラクタで登録する(`initComponents` と CardLayout への登録は自動)。
- 画面サイズ依存の数値はすべて `ShootingGameMain.getScreenSizeAdj()`(倍率)を掛ける。ハードコードのピクセル値は崩れる原因になる。
- 全画面の上に重なるコマンド/チャット欄は `CommandGlassPane`(GlassPane)。

### 設定駆動のゲーム内容

敵・ボス・弾・ステージ・アイテム・スキル等は `src/main/resources/static/configs/*.json`(ShipConfig / StageConfig / BulletConfig / BossConfig / ItemConfig …)で定義され、`ShipManager` / `StageManager` / `BulletManager` などが Jackson で読む。画像は `static/img/**` にあり、**スプライトシートは `grids` と `useCount` が画像の実際のコマ数と合っていること**(合わないとシート全体が1枚で描かれる)。`/reload` で設定と画像を読み込み直せる。

- ボスの見た目は `BossLook`(形・色・形態ごとに増える部品)が定義。`img/ship/boss/*_f0〜f4.png` はこの定義から描いた4x2・6コマのシート(画像がなければ `Boss.drawPlaceholder` が図形で描く)。
- ステージのギミック(ビームを撃つブロック `BlockSpec`、ビーム `BeamSpec`)は StageConfig の `blocks` / `gimmicks`。ブロックは `spin` で回転し、ビーム・当たり判定・押し出しも回転に追従する。

### 画面の部品(ボタン・アイコン)

- ボタン画像は `static/img/UI/**` に置く。`UIBuilder.createImageBtn(component, 画像パス, サイズ, 代替の文字)` は、画像があれば画像だけのボタンにし(**文字は消えるので、ラベルは画像に描き込む**)、`<名前>_rollover.png` / `<名前>_pressed.png` があれば状態に応じて切り替える。画像がなければ `ShooterButtonUI`(全ボタン共通の文字ボタンの見た目)で代替の文字を出す。
- 画像ボタンは、透明なところは押せない。中が透明なアイコンは、中を押しても反応しないので、不透明な板の上に描くこと。
- 戻るボタンは `UIBuilder.createBackBtn`(左下の隅の正方形の矢印)。画面によってサイズ・配置を変えず、これを使う。
- スキルツリーのボタンは、`img/UI/upgradeScene/<ノード名>.png`(模様だけ)に、`SkillIconComposer` が枠とレベルの札を重ねて作る(最大レベルは金の枠)。配置は `SkillTreeLayout` が機体ごとの模様(花・渦巻き・蜂の巣・星・同心円)で決める純ロジックで、枝が交差しないことをテストで確認している。
- 設定ファイルの値が「減る効果」のスキルは `reduce: true`(表示を「-」にするだけ。値は正のまま)。

### ゲーム内

- `GameManager` が一連のマネージャー(`StageManager` → `BulletManager` → `BeamManager` → `ShipManager` → `ItemManager` → `DamagePopupManager`)を**この順で**更新・描画する(描画順が重なり順)。船は `Ship`(Player / Enemy / Boss / SubShip)。ダメージは `Ship.addDamage` が起点(ここからダメージ数字が出る)。
- クリア/ゲームオーバーの精算は `GameManager.result()`(報酬・初クリア・難易度ミッション・ハイスコア)。

### データとセーブ

- `DataManager`(static)が現在の `UserData`(進捗 `Progress`、資源、船、モジュール…)を持ち、`UserDataMapper` で JPA の `UserEntity` と相互変換する。**保存はウィンドウを閉じる時(`windowClosing`)にまとめて1回**で、各サービスは操作のたびに保存しない(クラッシュ・強制終了では直近の操作が失われる)。
- 追加のフラグ類は DB のテーブルを増やさず、`Progress.highScores`(`Map<String,Integer>`)に接頭辞つきキーで相乗りさせる流儀がある(`daily:`、`endless:wave`、`seen:<船ID>`=図鑑)。
- 端末ごとの設定(FPS、ダメージ表記)は `GameSettings` が実行フォルダの `settings.properties`(git 管理外)に保存する。ユーザーデータとは別。
- ゲームのルール計算は `domain/*Service`(スキル・ユニット・モジュール・サブ機・変換・コスト・図鑑・ショップ・★報酬・解放)の static メソッドに置く流儀。UI(`*Panel`)はそれを呼ぶだけ。
- **レベル解放**: `UnlockService`(解放レベルは `UnlockConfig.json`)。未開放のボタンは `guard(button, Feature)` で登録し、画面に入るたびの `setup()` で `refreshAll()` を呼ぶとグレーアウトされる(押すと案内)。経験値表 `Exp.json` は Lv50 まで。
- **サブ機**: パーツ制作は廃止。`SubShipTypeService`(タイプ一覧とレベルごとの撃ち方は `SubShipTypeConfig.json`。先頭が標準)と `SubShipService`(タイプごとのレベルは `userData.subShipParts` の `LV:<タイプID>`)。レベルが上がると撃つ弾の数と種類が増える。
- **ボスのゲージ**: `BossLibrary.forFight(bossId, stageId)` が、ステージに応じて戦うゲージを絞る(無限モードは後ろ3つ、ノーマルは10の倍数が5・5の倍数が3)。
- 保存は、読み込みを別々の select にし(`@Fetch(SELECT)`)、書き込みをバッチ(`ShootingGameMain` の既定値)にして、遠い DB でも固まりにくくしている。閉じる操作では先にウィンドウを隠す。

### コマンド

`command/` に `CommandGroup` のサブクラス(System / Game / PlayerData)があり、`register(...)` で `rootCommands` に木構造で登録する。4つ目の引数 `true` が「管理者のみ」。出力は `info` / `success` / `error` / `usage` を使う(ログ欄=`LogLine` の種別と、コンソールの両方へ出る)。コマンド一覧は `command.md`。

### 開発メモの置き場

`todo_list.md`(完了したら行頭に「〇」を表記)、`UML.puml`(クラス図。構成を変えたら更新)、`command.md`、`shootingGame.md`(環境メモ)。

## 作業上の注意

- **作業が終わるごとに、コミットして push する。** 手順:
  - まず、上の方法で確実に再コンパイルし、単体テストが通ることを確認する(通らなければコミットしない)。
  - 変更は、テーマごとに分けてコミットする(会心、サブ機、UI など)。1つのファイルに複数のテーマが混ざる場合は、主なテーマのコミットに入れる。メッセージは日本語で、1行目に要約、本文に理由と範囲を書く。
  - `git add` は、パスを指定して行う(`git add -A` で `application.properties` や `settings.properties` など git 管理外のものや、生成物を巻き込まない)。削除したファイルのパスを `git add` に渡すと全体が失敗するので、`git add -A -- <パス>` を使う。
  - **コミットメッセージに `Co-Authored-By`(Claude を共作者にする行)や「Generated with Claude Code」の行を付けない。** GitHub の履歴に Claude の名前が出ないようにするため。PR の説明にも付けない。
  - コミットのあとは、今のブランチを `git push` する。force push はしない。
  - 見た目の確認ができていない変更は、コミットメッセージや報告に「未確認」と書く。
- 大きな機能は、実装前に方針をユーザーに確認する運用になっている(選択肢を出して決めてもらう)。
- 大きな変更は、サブエージェントに分けて並行で進める運用になっている(ユーザーが依頼したとき)。分け方の決まり:
  - 担当ファイルが重ならないように分ける(同じファイルを2つのエージェントに触らせない)。
  - 先に自分が共通の土台(新しい型や API の骨組み)を作ってコンパイルを通し、各エージェントにはそのシグネチャを正確に伝える。
  - 各エージェントには「触ってよいファイル」「コミットしない(コミットと push は、全員の終了後に親が行う)」「他の人が同時に別のファイルを編集している」ことを明記する。
  - 同時に Maven を実行すると `target` の操作が衝突する。Maven を使うのは1人だけにし、ほかのエージェントには「Maven は実行しない」と伝える。
  - 全員の終了後に、全体の再コンパイル(上の「確実に確認する方法」)とテストと、触った範囲が重なっていないかを自分で確認する。エージェントの報告は鵜呑みにしない。
  - **`UML.puml` の更新と、テストコード(`src/test`)の作成は、サブエージェントに任せる**(機能の実装が終わったら、それぞれ別のエージェントに、並行して依頼する)。`command.md` などのドキュメントも、必要なら同じように任せる。
    - 依頼文には、変更した内容(クラス・メソッド・設定ファイル・仕様)と、実際の差分を見られること(`git diff` / `git status`)を書き、「実コードを読んで確認してから書く(推測で書かない)」と伝える。
    - UML のエージェントは `UML.puml` だけ、テストのエージェントは `src/test` だけを触る。本体(`src/main`)は直さず、テストが落ちたら報告させる。
    - テストは、DB・画面なしで動く純ロジックだけを対象にする。テストを実行するのはテストのエージェント1人にして、UML のエージェントには Maven を実行させない。
    - 終わったら、自分で再コンパイルとテストを流し、UML の波括弧の数と、触った範囲が重なっていないかを確認してから、コミットと push を行う。
    - 実装そのものを分ける場合のほかに、このように「実装は自分、UML とテストはサブエージェント」という分け方を、ふだんの標準にする。
- 見た目・描画まわりの変更は、コンパイルが通っても実際の見た目は保証されない。起動して確認するか、未確認と明記する。
- ファイルの編集で、複数行の正規表現の置換(perl や sed)は、改行コード(CRLF/LF)の違いや文字列の中のエスケープで、黙って失敗したり、文字列の途中に改行が入ったりした。編集のあとは必ず該当箇所を読み直して反映を確認し、小さな編集は Edit ツールを使う。
- UML(`UML.puml`)は、構成を変えたら更新し、波括弧の数が合うことを確認する(PlantUML での描画確認はまだしていない)。
- ボタン画像や模様の絵は、スクリプト(Java)で描いて生成してきた。生成スクリプトはリポジトリに入っていないので、絵を直すときは、同じ作りの画像を描き直すか、画像を直接差し替える。
