package com.game.shooting_game.command;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

/**
 * システム系のコマンド(/fps /admin /help /loadscene /reload)
 */
final class SystemCommands extends CommandGroup {
    SystemCommands(CommandGlassPane pane) {
        super(pane);
    }

    @Override
    void registerAll() {
        // fps変更 /fps <num>
        register(rootCommands, "/fps", "Change fps.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                Integer fps = GameUtils.tryParse(args[0]);

                // fpsが0以下の場合はエラー
                if (fps == 0) {
                    logText = "FPS must be greater than 0.";
                    log().addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }

                MAIN.setFps(fps);

                logText = "Current fps set to " + fps + ".";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } catch (NumberFormatException ne) {
                logText = "Unknown param: '" + args[0] + "'";
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<number>");

        // 管理者権限切替 /admin
        register(rootCommands, "/admin", "Toggle admin mode.", false, (node, args) -> {
            if (ShootingGameMain.getOnlineMode()) {
                // 引数が不適切な場合
                if (args.length != 1) {
                    logText = node.getUseage();
                    log().addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }

                String password = args[0];
                if (!DataManager.authenticate("admin", password)) {
                    logText = "password is not true.";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            } else {
                // 引数が不適切な場合
                if (args.length != 0) {
                    logText = node.getUseage();
                    log().addElement(logText);
                    LogUtils.debugLog(logText);
                    return;
                }
            }

            pane.setAdminMode(!pane.isAdminMode());

            logText = "AdminMode " + (pane.isAdminMode() ? "enabled." : "disabled.");
            log().addElement(logText);
            LogUtils.systemLog("[INFO]", component, ANSI.YELLOW + logText + ANSI.RESET);
        }).setSyntax(ShootingGameMain.getOnlineMode() ? "<password>" : "");

        // ヘルプ /help
        register(rootCommands, "/help", "Show available commands.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            pane.showHelp();
        });

        // シーン読み込み /loadscene <sceneName> /loadscene game <stageId>
        CommandNode loadscene = register(rootCommands, "/loadscene", "Change scene.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                if (SHIP_M.getPlayer() != null)
                    SHIP_M.getPlayer().removeHandler();

                SM.changeScene(SceneType.valueOf(args[0].toUpperCase()));
            } catch (Exception e) {
                logText = "Unknown scene: '" + args[0] + "'";
                log().addElement(logText);
                LogUtils.debugLog(logText);
            }
        });
        loadscene.setSyntax("<sceneName>");
        register(loadscene, "game", "Load game stage.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].toLowerCase().replace("stage", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!pane.isAdminMode()) {
                    // ロックされていた場合は遷移しない
                    int clearedStage = DataManager.getUserData().getProgress().getClearedStage() + 1;
                    if (id > clearedStage) {
                        logText = "Can't play locked stage.";
                        log().addElement(logText);
                        LogUtils.debugLog(logText);
                        return;
                    }
                }

                STAGE_M.getStage().setStageId(id);
                SM.changeScene(SceneType.GAME);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<stageId>");
        register(loadscene, "test", "Load test stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            SM.changeScene(SceneType.TEST);
        });

        // リソースの読み込み直し /reload
        register(rootCommands, "/reload", "Reload resource", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            pane.reload();
        });

    }
}
