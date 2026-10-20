package com.game.shooting_game.command;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * ゲーム操作・デバッグ系のコマンド(/kill /summon /beam /clear /effect /gameclear /gameover /clearstage)
 */
final class GameCommands extends CommandGroup {
    GameCommands(CommandGlassPane pane) {
        super(pane);
    }

    @Override
    void registerAll() {
        // 殺害 /kill <target>
        register(rootCommands, "/kill", "Kill targets.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            SHIP_M.kill(args[0].toLowerCase());

            logText = args[0] + " killed.";
            log().addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        }).setSyntax("<target>");

        // 召喚 /summon <enemyId> <x> <y> [mode] [level] (座標は画面中央が0,0)
        register(rootCommands, "/summon", "Summon an enemy at (x, y). Origin is the screen center.", true,
                (node, args) -> {
                    // 引数が不適切な場合
                    if (args.length < 3 || args.length > 5) {
                        logText = node.getUseage();
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                        return;
                    }
                    // シーンが不適切な場合
                    if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                        return;

                    try {
                        String enemyId = args[0];
                        double x = Double.parseDouble(args[1]);
                        double y = Double.parseDouble(args[2]);
                        ColorMode mode = args.length >= 4 ? ColorMode.valueOf(args[3].toUpperCase()) : ColorMode.BLUE;
                        int level = args.length == 5 ? Integer.parseInt(args[4]) : 0;

                        // 船の種類(敵かボス)をIDから判断(大文字小文字は区別しない)
                        ShipKinds owner = null;
                        for (ShipKinds kind : new ShipKinds[] { ShipKinds.ENEMY, ShipKinds.BOSS }) {
                            for (String id : SHIP_M.getShipConfig().get(kind).keySet()) {
                                if (owner == null && id.equalsIgnoreCase(args[0])) {
                                    owner = kind;
                                    enemyId = id;
                                }
                            }
                        }
                        if (owner == null) {
                            logText = "Unknown enemy: " + enemyId;
                            log().addElement(logText);
                            LogUtils.debugLog(logText);
                            return;
                        }

                        // 画面中央を(0,0)とした座標を画面座標に変換
                        double adj = ShootingGameMain.getScreenSizeAdj();
                        Vector2 pos = new Vector2(
                                MAIN.getWidth() / 2.0 + x * adj,
                                MAIN.getHeight() / 2.0 + y * adj);

                        SHIP_M.addShip(ShipFactory.createShip(owner, enemyId, mode, level, pos, new Vector2(pos), null));

                        logText = enemyId + " summoned at (" + x + ", " + y + ").";
                        log().addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    } catch (NumberFormatException ne) {
                        logText = "Invalid number: " + ne.getMessage();
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                    } catch (IllegalArgumentException ie) {
                        logText = "Invalid mode (BLUE or RED).";
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                    } catch (Exception e) {
                        LogUtils.systemLog("[WARN]", component, "Command Error");
                    }
                }).setSyntax("<enemyId>", "<x>", "<y>", "[mode]", "[level]");

        // ビームの試射 /beam <origin> [color]  (SHIP_UPだけは自機のビームを撃つ)
        register(rootCommands, "/beam", "Fire a test beam. Origins: TOP_PLAYER, TOP_X, TOP_RANDOM, WALL_LEFT, WALL_RIGHT, WALL_RANDOM, SHIP_UP.",
                true, (node, args) -> {
                    // 引数が不適切な場合
                    if (args.length < 1 || args.length > 2) {
                        logText = node.getUseage();
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                        return;
                    }
                    // シーンが不適切な場合
                    if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                        return;

                    try {
                        com.game.shooting_game.gameScene.beam.BeamSpec spec = new com.game.shooting_game.gameScene.beam.BeamSpec();
                        spec.setOrigin(com.game.shooting_game.gameScene.beam.BeamSpec.Origin.valueOf(args[0].toUpperCase()));
                        ColorMode mode = args.length == 2 ? ColorMode.valueOf(args[1].toUpperCase()) : ColorMode.RED;
                        var player = SHIP_M.getPlayer();

                        if (spec.getOrigin() == com.game.shooting_game.gameScene.beam.BeamSpec.Origin.SHIP_UP) {
                            // 自機のビーム(自機の色に追従)
                            if (player == null)
                                return;
                            spec.setWarn(0.2);
                            spec.setDuration(1.5);
                            spec.setWidth(30);
                            spec.setDamage(40);
                            com.game.shooting_game.gameScene.beam.BeamManager.getInstance().firePlayer(
                                    spec,
                                    () -> new Vector2(player.getPos().getX(), player.getPos().getY() - 10),
                                    () -> player.getMode(), 1.0);
                        } else {
                            // 敵のビーム(予兆1.2秒 → 照射1秒)
                            spec.setDuration(1.0);
                            spec.setX(0);
                            com.game.shooting_game.gameScene.beam.BeamManager.getInstance().fireHostile(spec, null, mode);
                        }

                        logText = "Beam fired: " + spec.getOrigin() + " " + mode;
                        log().addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    } catch (IllegalArgumentException ie) {
                        logText = "Invalid origin or color (BLUE or RED).";
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                    } catch (Exception e) {
                        LogUtils.systemLog("[WARN]", component, "Command Error");
                    }
                }).setSyntax("<origin>", "[color]");

        // 消去 /clear <target>
        register(rootCommands, "/clear", "Kill targets.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            SHIP_M.clear(args[0].toLowerCase());

            logText = args[0] + " cleared.";
            log().addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        }).setSyntax("<target>");

        // 効果付与 /effect
        CommandNode effect = register(rootCommands, "/effect", "Apply effect.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length == 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
        });
        effect.setSyntax("<effectName>", "<seconds>");
        // 回復
        register(effect, "heal", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            try {
                Integer num = args.length > 0 ? GameUtils.tryParse(args[0]) : null;

                SHIP_M.getPlayer().heal(num);

                logText = "Healed " + num + "HP.";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<seconds>");
        // レベルアップ
        register(effect, "levelup", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            // 指定がなければ1レベル上げる、指定があればその回数レベルアップ
            int amount = 1;
            try {
                Integer num = GameUtils.tryParse(args[0]);
                amount = num > 0 ? num : 1;
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                log().addElement(logText);
                LogUtils.debugLog(logText);
            }
            // レベルアップ処理
            for (int i = 0; i < amount; i++) {
                SHIP_M.getPlayer().levelUp();
            }

            logText = "Ship leveled up to level " + SHIP_M.getPlayer().getShipLevel() + ".";
            log().addElement(logText);
            LogUtils.systemLog("[INFO]", component, logText);
        });
        // レベル設定
        register(effect, "setlevel", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            try {
                Integer num = GameUtils.tryParse(args[0]);

                if (num >= 5) {
                    SHIP_M.getPlayer().setShipLevel(4);
                    SHIP_M.getPlayer().levelUp();
                } else
                    SHIP_M.getPlayer().setShipLevel(num);

                logText = "Ship level set to " + SHIP_M.getPlayer().getShipLevel() + ".";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<amount>");
        // 暴走
        register(effect, "ooc", null, true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length > 2) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                return;

            boolean isInfinityOOC = SHIP_M.getPlayer().getOOCTimer() == Double.POSITIVE_INFINITY;
            // 指定がなければ暴走モードのトグル、指定があれば暴走時間を設定
            if (args.length == 0) {
                double time = isInfinityOOC ? 0 : Double.POSITIVE_INFINITY;
                int level = isInfinityOOC ? 4 : 5;
                isInfinityOOC = !isInfinityOOC;

                SHIP_M.getPlayer().setOOCTimer(time);
                SHIP_M.getPlayer().setShipLevel(level);

                logText = "OOC " + (isInfinityOOC ? "enabled." : "disabled.");
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                try {
                    Integer time = GameUtils.tryParse(args[0]);

                    if (isInfinityOOC) {
                        logText = "The ship is already OOC.";
                        log().addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    } else {
                        SHIP_M.getPlayer().setShipLevel(5);
                        SHIP_M.getPlayer().setOOCTimer(time);

                        logText = "The ship became OOC.";
                        log().addElement(logText);
                        LogUtils.systemLog("[INFO]", component, logText);
                    }
                } catch (NumberFormatException ne) {
                    logText = "Unknnown param: " + args[0];
                    log().addElement(logText);
                    LogUtils.debugLog(logText);
                } catch (Exception e) {
                    LogUtils.systemLog("[WARN]", component, "Command Error");
                }
            }
        }).setSyntax("<seconds>");

        // ゲームクリア/ゲームオーバー /gameclear /gameover
        register(rootCommands, "/gameclear", "Instantly clear.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(false);
            GM.result();
        });
        register(rootCommands, "/gameover", "Instantly gameover.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }
            // シーンが不適切な場合
            if (!isSceneValid(SceneType.GAME))
                return;

            GM.gameover(true);
            GM.result();
        });
        register(rootCommands, "/clearstage", "Force clear a specific stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                Integer stageId = GameUtils.tryParse(args[0]);

                UserData data = DataManager.getUserData();
                // 与えられたステージがクリア済みかどうか
                boolean isCleared = stageId <= data.getProgress().getClearedStage();
                if (!isCleared) {
                    data.getProgress().setClearedStage(stageId);
                    NSM.getStageBtnPanel().setup();

                    // クリア進捗更新ログ
                    logText = "Progress reached " + ANSI.YELLOW + "Stage" + stageId + ANSI.RESET + ".";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                } else {
                    logText = "Specified stage has already been completed.";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                }
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + args[0];
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<stageId>");

    }
}
