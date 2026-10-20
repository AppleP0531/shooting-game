package com.game.shooting_game.command;

import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.bagScene.BagManager;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.data.UserData.Resources;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.lobbyScene.LobbyManager;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;

/**
 * プレイヤーデータ系のコマンド(/setship /unlockship /exp /give /deleteuser /changepassword)
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
                usage(node);
                return;
            }

            String param = args[0].toLowerCase().replace("ship", "");
            try {
                Integer id = GameUtils.tryParse(param);

                if (!isSceneValid(SceneType.GAME, SceneType.TEST))
                    return;

                // 存在しない船には変えない
                if (!SHIP_M.getShipConfig().get(ShipKinds.PLAYER).containsKey("Ship" + id)) {
                    error("Unknown ship: Ship" + id);
                    return;
                }

                SHIP_M.reloadPlayer(id);
            } catch (NumberFormatException ne) {
                error("Unknown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId>");

        // 船の解法 /unlockship
        register(rootCommands, "/unlockship", "Unlock ship.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 1) {
                usage(node);
                return;
            }

            String param = args[0].toLowerCase().replace("ship", "");
            try {
                Map<String, ?> playerShips = SHIP_M.getShipConfig().get(ShipKinds.PLAYER);

                // all: 未所持の船をすべて解放する(予備機は増やさない)
                if (args[0].equalsIgnoreCase("all")) {
                    int unlocked = 0;
                    for (String shipId : playerShips.keySet()) {
                        if (DataManager.getUserData().getShips().containsKey(shipId))
                            continue;
                        DataManager.getUserData().unlockShip(shipId);
                        unlocked++;
                    }
                    LobbyManager.getInstance().updateBtnStates();
                    success("Unlocked " + unlocked + " ship(s).");
                    return;
                }

                Integer id = GameUtils.tryParse(param);

                String sid = "Ship" + id;
                // 存在しない船は解放しない
                if (!playerShips.containsKey(sid)) {
                    error("Unknown ship: " + sid);
                    return;
                }
                if (DataManager.getUserData().getShips().containsKey(sid))
                    com.game.shooting_game.domain.unit.UnitService.addCopy(sid); // 所持済みなら予備機が増える
                DataManager.getUserData().unlockShip(sid);
                LobbyManager.getInstance().updateBtnStates();
            } catch (NumberFormatException ne) {
                error("Unknown param: " + param);
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<shipId|all>");

        // 経験値の加算・設定 /exp
        register(rootCommands, "/exp", "Add or set exp.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 2 || !(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("set"))) {
                usage(node);
                return;
            }

            try {
                int amount = GameUtils.tryParse(args[1]);
                UserData.Progress progress = DataManager.getUserData().getProgress();
                // 累計の経験値は0未満にしない
                int newExp = args[0].equalsIgnoreCase("add") ? progress.getExp() + amount : amount;
                progress.setExp(Math.max(0, newExp));

                success("Exp is now " + progress.getExp() + " (Lv " + DataManager.getUserLevel().level() + ").");
            } catch (NumberFormatException ne) {
                error("Unknown param: '" + args[1] + "'");
            }
        }).setSyntax("<add|set>", "<amount>");

        // 資源入手 /give
        register(rootCommands, "/give", "Give resources.", true, (node, args) -> {
            // 引数が不適切な場合
            if (args.length != 2) {
                usage(node);
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
                                usage(node);
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

                    success("Added " + amount + " coins. Current coins: " + resData.getCoins() + ".");
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
                error("Unknown param: '" + args[1] + "'");
            } catch (IllegalArgumentException iae) {
                error("Unknown resName: '" + args[0] + "'");
            } catch (Exception e) {
                LogUtils.systemLog("[WARN]", component, "Command Error");
            }
        }).setSyntax("<resourceName>", "<amount>");

        // データリセット /deleteuser
        register(rootCommands, "/deleteuser", "Reset data.", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                error("This command is unavailable in offline mode.");
                return;
            }

            // 引数が不適切な場合
            if (args.length != 2) {
                usage(node);
                return;
            }

            String username = args[0];
            String password = args[1];

            // adminアカウントの削除防止
            if (username.equals("admin")) {
                error("Can't delete admin account.");
                return;
            }

            // forceオプションが渡された場合強制削除
            if (password.equals("force")) {
                if (DataManager.forceDelete(username)) {
                    success("Delete successful.");
                    return;
                }
            }

            // 通常モードでデータ削除
            if (DataManager.delete(username, password)) {
                success("Delete successful.");
            } else {
                error("Deletion failed.");
            }
        }).setSyntax("<username>", "<password>");

        // パスワード変更 /changepassword
        register(rootCommands, "/changepassword", "Change user password", true, (node, args) -> {
            // オフラインモードの場合
            if (!ShootingGameMain.getOnlineMode()) {
                error("This command is unavailable in offline mode.");
                return;
            }

            // 引数が不適切な場合
            if (args.length != 3) {
                usage(node);
                return;
            }

            String username = args[0];
            String oldPassword = args[1];
            String newPassword = args[2];

            // forceオプションが渡された場合パスワード強制変更
            if (oldPassword.equals("force")) {
                if (DataManager.forceChangePassword(username, newPassword)) {
                    success("Change password successful.");
                    return;
                }
            }

            // 通常モードでパスワード変更
            if (DataManager.changePassword(username, oldPassword, newPassword)) {
                success("Change password successful.");
            } else {
                error("Change password failed.");
            }
        }).setSyntax("<username>", "<oldPassword>", "<newPassword>");
    }
}
