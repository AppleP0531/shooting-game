# コマンド一覧

ゲーム中に `/` キーでコマンド入力欄を開き、`/コマンド名 引数…` の形で入力する(`t` キーはチャット欄)。
ログインが終わるまでは、コマンド入力欄・チャット欄は開けない。

- **管理者**の列が「要」のコマンドは、管理者モード(`/admin`)のときだけ使える。`/help` も、管理者モードのときだけ「要」のコマンドを表示する。
- 引数の `<>` は必須、`[]` は省略可能。
- **使えるシーン**が書かれているコマンドは、そのシーン以外では「Command not available in the current scene.」と出て何も起きない。

実装は [src/main/java/com/game/shooting_game/command/](src/main/java/com/game/shooting_game/command/) の3ファイルにある。

| ファイル | 内容 |
| --- | --- |
| `SystemCommands.java` | システム系(`/fps` `/admin` `/help` `/scene` `/reload`) |
| `GameCommands.java` | ゲーム操作・デバッグ系(`/kill` `/summon` `/beam` `/despawn` `/effect` `/gameclear` `/gameover` `/stageclear`) |
| `PlayerDataCommands.java` | プレイヤーデータ系(`/setship` `/unlockship` `/exp` `/shiplevel` `/skill` `/give` `/deleteuser` `/changepassword`) |

---

## システム系

| コマンド | 管理者 | 説明 |
| --- | --- | --- |
| `/fps <number>` | - | FPSを変更する(例: `/fps 60`、`/fps 120`)。0は指定できない |
| `/admin [password]` | - | 管理者モードの切り替え。オンラインモードでは、`admin` アカウントのパスワードが必要(`/admin <password>`)。オフラインモードでは引数なし |
| `/help` | - | 使えるコマンドの一覧を表示する |
| `/scene <sceneName>` | - | シーンを移動する(例: `/scene lobby`、`/scene title`)。シーン名は `SceneManager.SceneType` の名前(大文字小文字は区別しない) |
| `/scene game <stageId>` | - | ステージを選んでゲームを始める(例: `/scene game 3`、`/scene game stage3`)。ロックされたステージは、管理者モード以外では入れない |
| `/scene test` | 要 | テストシーンへ移動する |
| `/reload` | - | リソース(設定ファイルと画像)を読み込み直す |

## ゲーム操作・デバッグ系(管理者のみ)

| コマンド | 使えるシーン | 説明 |
| --- | --- | --- |
| `/kill <target>` | GAME / TEST | 対象を倒す(ドロップや撃破演出あり)。`target`: `player` `enemy` `boss` `all` |
| `/despawn <target>` | GAME / TEST | 対象を消す(撃破扱いにならない)。`target`: `player` `enemy` `boss` `all` |
| `/summon <enemyId> <x> <y> [mode] [level]` | GAME / TEST | 敵・ボスを呼び出す。座標は画面中央が(0, 0)、単位は基準サイズ。`mode`: `BLUE`(既定)または `RED`。`level`: 敵レベル(既定0)。`enemyId` は大文字小文字を区別しない |
| `/beam <origin> [color]` | GAME / TEST | ビームの試射。`origin`: `TOP_PLAYER` `TOP_X` `TOP_RANDOM` `WALL_LEFT` `WALL_RIGHT` `WALL_RANDOM` `SHIP_UP`。`SHIP_UP` は自機のビームを撃つ。`color`: `BLUE` または `RED`(既定 `RED`) |
| `/gameclear` | GAME | 今すぐクリアにする |
| `/gameover` | GAME | 今すぐゲームオーバーにする |
| `/stageclear <stageId>` | どこでも | そのステージまでをクリア済みにする(すでにクリア済みなら何もしない)。ステージ選択のボタンも更新する |

### `/effect`(管理者のみ・GAME / TEST)

自機に効果を与える。

| コマンド | 説明 |
| --- | --- |
| `/effect heal <amount>` | HPを回復する。回復するHPの量を指定する |
| `/effect levelup [amount]` | 機体レベルを上げる。回数の省略時は1。入力欄のヒントは出ない |
| `/effect setlevel <amount>` | 機体レベルを設定する。5以上を指定すると、暴走状態(レベル5)になる |
| `/effect ooc [seconds]` | 暴走状態にする。省略するとオンオフを切り替える(オンのときは無制限)。秒数を指定すると、その時間だけ暴走する |

## プレイヤーデータ系(管理者のみ)

| コマンド | 説明 |
| --- | --- |
| `/setship <shipId>` | 使う船を変えて、プレイヤーを作り直す(GAME / TEST のみ)。`Ship2` でも `2` でも指定できる |
| `/unlockship <shipId\|all>` | 船を解放する。`Ship2` でも `2` でも指定できる。すでに持っている船を指定すると、予備機が1つ増える。`all` は、持っていない船をすべて解放する(予備機は増えない)。船の設定(ShipConfig)にない船名は「Unknown ship」で断る |
| `/exp <add\|set> <amount>` | 累計の経験値を増やす(`add`)か、その値にする(`set`)。0未満にはならない。実行後の経験値とレベルをログに出す |
| `/shiplevel <add\|set> <shipId\|all> <amount>` | 船のレベルを増やす(`add`)か、その値にする(`set`)。1〜50に収める。コストは見ない。`Ship2` でも `2` でも指定できる。`all` は持っている船すべて。持っていない船は「Ship not owned」で断る |
| `/skill <add\|set> <skillName\|all> <amount>` | 使っている船のスキルツリーのレベルを増やす(`add`)か、その値にする(`set`)。0〜そのスキルの最大レベルに収める。コスト・研究時間・解放条件は見ない。名前は大文字小文字を区別しない。`all` はすべてのスキル。研究中のスキルはそのまま続く |
| `/give <resourceName>[{rarity=<レア度>}] <amount>` | 資源を入手する。レア度の省略時は `COMMON`(例: `/give resource_1 100`、`/give resource_1{rarity=epic} 5`)。コインを指定するとコインが増える。個数が負で、所持数が0以下になると、その資源が消える |
| `/deleteuser <username> <password>` | ユーザーを削除する(オンラインモードのみ)。パスワードに `force` を渡すと、パスワードなしで削除する。`admin` は削除できない |
| `/changepassword <username> <oldPassword> <newPassword>` | パスワードを変更する(オンラインモードのみ)。古いパスワードに `force` を渡すと、強制的に変更する |

---

## 補足

- コマンドを追加するときは、`CommandGroup.register(...)` で登録する。4つ目の引数 `true` が「管理者のみ」の指定になる。
- `/scene` のように、サブコマンドを持つコマンドは、サブコマンドごとに管理者の指定を持つ。
