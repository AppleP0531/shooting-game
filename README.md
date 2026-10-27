# shooting-game

縦画面の Java Swing 製シューティングゲームです。セーブデータは Spring Boot + JPA で H2 に保存します。
(開発中)

<!-- ここにスクリーンショットや GIF を貼る: ![gameplay](docs/gameplay.png) -->

## 主な機能

- **ステージ**: 通常ステージ、無限ステージ、EXTRA ステージ。ボス戦、ステージギミック(回転するビームブロックなど)
- **育成**: スキルツリーによる強化、機体・サブ機・モジュール、ショップ、鞄、図鑑
- **進行**: レベルによる機能解放、難易度別のミッションと星報酬
- **コマンド**: ゲーム中に `/` キーでコマンド入力欄を開き、補完付きのデバッグ・管理コマンドを実行 → [command.md](command.md)
- **オフライン動作**: DB に接続できなければ、自動でオフラインモード(ゲストでログインなし)に切り替え

## 技術スタック

| 分類 | 内容 |
| --- | --- |
| 言語 | Java 25 |
| UI / 描画 | Swing(自前のゲームループ、`CardLayout` による画面切り替え) |
| フレームワーク | Spring Boot 4(JPA、Security) |
| DB | H2(ファイル) |
| ビルド | Maven(Maven Wrapper) |
| テスト | JUnit(DB・画面なしの純ロジックを対象) |

## 設計のポイント

- **シーン管理**: 画面ごとに `Manager` インターフェース(`setup` / `update(dt)` / `draw(g)` / `initComponents`)を実装したシングルトンを持ち、`SceneManager` が遷移履歴を積んで画面を切り替えます。
- **設定駆動**: 敵・ボス・弾・ステージ・アイテム・スキルなどは `src/main/resources/static/configs/*.json` で定義し、コードを変えずに調整できます。
- **ルールと UI の分離**: スキル・報酬・難易度・図鑑などのルール計算は `domain/*Service` の static メソッドにまとめ、UI(`*Panel`)は呼び出すだけにしています。
- **描画順の管理**: `GameManager` が `Stage → Bullet → Beam → Ship → Item → DamagePopup` の順に更新・描画します(描画順が重なり順になります)。
- **画面サイズ対応**: 画面サイズに依存する数値はすべて倍率(`getScreenSizeAdj()`)を掛けて計算します。
- **保存の工夫**: 保存はウィンドウを閉じるときにまとめて1回行い、読み込みは個別の select、書き込みはバッチにして、遠い DB でも固まりにくくしています。

## 動かし方

JDK 25 が必要です(例: [Eclipse Adoptium](https://adoptium.net/ja))。

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean spring-boot:run
```

Swing のウィンドウが開きます。セーブデータは実行フォルダの `data/shooting_game_db.mv.db`(H2)にできます。バックアップはこのファイルをコピーしてください。

## テスト

```powershell
.\mvnw.cmd test "-Djava.awt.headless=true"
```

DB を使わない単体テストだけを回す場合は、`-Dtest=GameUtilsTest,BeamBlockTest,DexServiceTest,DamageFormulaTest` のようにクラス名を指定します。

## ディレクトリ構成

```
src/main/java/com/game/shooting_game/
├─ ShootingGameMain.java      起動・ゲームループ
├─ SceneManager.java          画面遷移
├─ *Scene/                    画面(title / lobby / shop / upgrade / stage / game / ...)
├─ domain/                    ルール計算(*Service)
├─ command/                   ゲーム内コマンド
├─ entity / repository/       JPA(セーブデータ)
└─ ui / utils                 共通の UI 部品・ユーティリティ
src/main/resources/static/    configs/*.json(ゲーム内容の定義)、img/(画像)
```

## ドキュメント

- [command.md](command.md): ゲーム内コマンド一覧
- [UML.puml](UML.puml): クラス図(PlantUML)
- [todo_list.md](todo_list.md): 開発メモ・今後の予定
