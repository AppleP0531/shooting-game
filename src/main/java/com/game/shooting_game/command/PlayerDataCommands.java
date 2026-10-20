package com.game.shooting_game.command;

import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData.Resources;
import com.game.shooting_game.lobbyScene.LobbyManager;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

/**
 * プレイヤーデータ系のコマンド(/setship /unlockship /get /deleteuser /changepassword)
 */
final class PlayerDataCommands extends CommandGroup {
    PlayerDataCommands(CommandGlassPane pane) {
        super(pane);
    }

    @Override
    void registerAll() {
        // 船タイプの変更 /setship
        register(rootCommands, "/setship", "Set now ship.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].toLowerCase().replace("ship", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                    return;

                SHIP_M.reloadPlayer(id);
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId>");

        // 船の解法 /unlockship
        register(rootCommands, "/unlockship", "Unlock ship.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String param = args[0].toLowerCase().replace("ship", "");
            try {
                Integer id = GameUtils.tryParse(args[0].toLowerCase().replace("ship", ""));

                String sid = "Ship" + id;
                if (DataManager.getUserData().getShips().containsKey(sid))
                    com.game.shooting_game.domain.unit.UnitService.addCopy(sid); // 所持済みなら予備機が増える
                DataManager.getUserData().unlockShip(sid);
                LobbyManager.getInstance().updateBtnStates();
            } catch (NumberFormatException ne) {
                logText = "Unknnown param: " + param;
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId>");

        // 資源入手 /give
        register(rootCommands, "/get", "get resource", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 2) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            try {
                String nameStr = args[0];
                Rarity rarity = Rarity.COMMON; // デフォルトはコモン

                // 指定値の設定
                int openBraceIdx = args[0].indexOf("{");
                int closeBraceIdx = args[0].indexOf("}");
                if (openBraceIdx != -1 && closeBraceIdx != -1 && openBraceIdx < closeBraceIdx) {
                    nameStr = args[0].substring(0, openBraceIdx);

                    Map<String, String> NBT = parseNBT(args[0].substring(openBraceIdx + 1, closeBraceIdx));
                    for (Map.Entry<String, String> entry : NBT.entrySet()) {
                        switch (entry.getKey()) {
                            case "rarity" -> {
                                rarity = Rarity.fromString(entry.getValue());
                            }
                            default -> {
                                // NBTが不適切な場合
                                logText = node.getUseage();
                                log().addElement(logText);
                                LogUtils.debugLog(logText);
                                return;
                            }
                        }
                    }
                }

                ResourceKinds resKind = new ResourceKinds(ResourceName.fromString(nameStr), rarity);
                Integer amount = GameUtils.tryParse(args[1]);
                Resources resData = DataManager.getUserData().getResources();
                if (resKind == ResourceKinds.COIN) {
                    int amountOwned = resData.getCoins();
                    resData.setCoins(amountOwned + amount);

                    logText = "Added " + amount + " coins. Current coins: " + resData.getCoins() + ".";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                } else {
                    int amountOwned = resData.getEquipments().getOrDefault(resKind, 0);
                    int newAmount = amountOwned + amount;
                    if (newAmount <= 0) {
                        resData.getEquipments().remove(resKind);
                    } else {
                        resData.getEquipments().put(resKind, newAmount);
                    }
                }
                BagManager.getInstance().setup();

            } catch (NumberFormatException ne) {
                logText = "Unknown param: '" + args[1] + "'";
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (IllegalArgumentException iae) {
                logText = "Unknown resName: '" + args[0] + "'";
                log().addElement(logText);
                LogUtils.debugLog(logText);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<resourceName>", "<amount>");

        // データリセット /deleteuser
        register(rootCommands, "/deleteuser", "Reset data.", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                logText = "This command is unavailable in offline mode.";
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            // 引数が不適切な場合
            if (args.length != 2) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String username = args[0];
            String password = args[1];

            // adminアカウントの削除防止
            if (username.equals("admin")) {
                logText = "Can't delete admin account.";
                log().addElement(logText);
                LogUtils.systemLog("[WARN]", component, logText);
                return;
            }

            // forceオプションが渡された場合強制削除
            if (password.equals("force")) {
                if (DataManager.forceDelete(username)) {
                    logText = "Delete successful.";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            }

            // 通常モードでデータ削除
            if (DataManager.delete(username, password)) {
                logText = "Delete successfull.";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                logText = "Deletion failed.";
                log().addElement(logText);
                LogUtils.systemLog("[WARN]", component, logText);
            }
        }).setSyntax("<username>", "<password>");

        // パスワード変更 /changepassword
        register(rootCommands, "/changepassword", "Change user password", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                logText = "This command is unavailable in offline mode.";
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            // 引数が不適切な場合
            if (args.length != 3) {
                logText = node.getUseage();
                log().addElement(logText);
                LogUtils.debugLog(logText);
                return;
            }

            String username = args[0];
            String oldPassword = args[1];
            String newPassword = args[2];

            // forceオプションが渡された場合パスワード強制変更
            if (oldPassword.equals("force")) {
                if (DataManager.forceChangePassword(username, newPassword)) {
                    logText = "Change password successful.";
                    log().addElement(logText);
                    LogUtils.systemLog("[INFO]", component, logText);
                    return;
                }
            }

            // 通常モードでパスワード変更
            if (DataManager.changePassword(username, oldPassword, newPassword)) {
                logText = "Change password successful.";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            } else {
                logText = "Change password failed.";
                log().addElement(logText);
                LogUtils.systemLog("[INFO]", component, logText);
            }
        }).setSyntax("<username>", "<oldPassword>", "<newPassword>");
    }
}
