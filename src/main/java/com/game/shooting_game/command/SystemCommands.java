package com.game.shooting_game.command;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

/**
 * システム系のコマンド(/fps /admin /help /scene /reload)
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
                usage(node);
                return;
            }

            try {
                Integer fps = GameUtils.tryParse(args[0]);

                // fpsが0以下の場合はエラー
                if (fps == 0) {
                    error("FPS must be greater than 0.");
                    return;
                }

                MAIN.setFps(fps);

                success("Current fps set to " + fps + ".");
            } catch (NumberFormatException ne) {
                error("Unknown param: '" + args[0] + "'");
            } catch (Exception e) {
                LogUtils.warn(component, "Command Error");
            }
        }).setSyntax("<number>");

        // 管理者権限切替 /admin
        register(rootCommands, "/admin", "Toggle admin mode.", false, (node, args) -> {
            if (ShootingGameMain.getOnlineMode()) {
                // 引数が不適切な場合
                if (args.length != 1) {
                    usage(node);
                    return;
                }

                String password = args[0];
                if (!DataManager.authenticate("admin", password)) {
                    info("password is not true.");
                    return;
                }
            } else {
                // 引数が不適切な場合
                if (args.length != 0) {
                    usage(node);
                    return;
                }
            }

            pane.setAdminMode(!pane.isAdminMode());

            success("AdminMode " + (pane.isAdminMode() ? "enabled." : "disabled."));
        }).setSyntax(ShootingGameMain.getOnlineMode() ? "<password>" : "");

        // ヘルプ /help
        register(rootCommands, "/help", "Show available commands.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                usage(node);
                return;
            }

            pane.showHelp();
        });

        // シーン読み込み /scene <sceneName> /scene game <stageId>
        CommandNode loadscene = register(rootCommands, "/scene", "Change scene.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                usage(node);
                return;
            }

            try {
                if (SHIP_M.getPlayer() != null)
                    SHIP_M.getPlayer().removeHandler();

                SM.changeScene(SceneType.valueOf(args[0].toUpperCase()));
            } catch (Exception e) {
                error("Unknown scene: '" + args[0] + "'");
            }
        });
        loadscene.setSyntax("<sceneName>");
        register(loadscene, "game", "Load game stage.", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                usage(node);
                return;
            }

            String param = args[0].toLowerCase().replace("stage", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!pane.isAdminMode()) {
                    // ロックされていた場合は遷移しない
                    int clearedStage = DataManager.getUserData().getProgress().getClearedStage() + 1;
                    if (id > clearedStage) {
                        error("Can't play locked stage.");
                        return;
                    }
                }

                STAGE_M.getStage().setStageId(id);
                SM.changeScene(SceneType.GAME);
            } catch (NumberFormatException ne) {
                error("Unknown param: " + param);
            } catch (Exception e) {
                LogUtils.warn(component, "Command Error");
            }
        }).setSyntax("<stageId>");
        register(loadscene, "test", "Load test stage.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                usage(node);
                return;
            }

            SM.changeScene(SceneType.TEST);
        });

        // リソースの読み込み直し /reload
        register(rootCommands, "/reload", "Reload resource", false, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 0) {
                usage(node);
                return;
            }

            pane.reload();
        });

    }
}
