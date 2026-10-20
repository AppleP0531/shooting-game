package com.game.shooting_game.command;

import java.util.HashMap;
import java.util.Map;
import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.utils.LogUtils;

/**
 * コマンドをまとめて登録するクラスの共通部分
 * 登録先(rootCommands)とログ欄は、コマンド入力欄(CommandGlassPane)のものを使う。
 */
abstract class CommandGroup {
    protected static final String component = "[Command]";

    protected final CommandGlassPane pane;
    protected final Map<String, CommandNode> rootCommands;

    protected final ShootingGameMain MAIN = ShootingGameMain.getInstance();
    protected final SceneManager SM = SceneManager.getInstance();
    protected final ResourceManager RM = ResourceManager.getInstance();
    protected final NormalStageManager NSM = NormalStageManager.getInstance();
    protected final UpgradeManager UM = UpgradeManager.getInstance();
    protected final GameManager GM = GameManager.getInstance();
    protected final ItemManager IM = ItemManager.getInstance();
    protected final ShipManager SHIP_M = ShipManager.getInstance();
    protected final BulletManager BM = BulletManager.getInstance();
    protected final StageManager STAGE_M = StageManager.getInstance();

    protected String logText;

    CommandGroup(CommandGlassPane pane) {
        this.pane = pane;
        this.rootCommands = pane.rootCommands;
    }

    /**
     * このクラスが持つコマンドをすべて登録する
     */
    abstract void registerAll();

    /**
     * コマンドのログ欄
     */
    protected ChatBoxContext log() {
        return pane.logHistory();
    }

    /**
     * 第一コマンド登録
     * 
     * @param root   親要素
     * @param name   コマンド名
     * @param desc   説明
     * @param admin  管理者権限が必要かどうか
     * @param action 処理
     * @return コマンドノード
     */
    protected CommandNode register(Map<String, CommandNode> root, String name, String desc, boolean admin,
            CommandGlassPane.CommandAction action) {
        CommandNode node = new CommandNode(null, name, desc, admin, action);
        root.put(name, node);
        return node;
    }

    /**
     * サブコマンド登録
     * 
     * @param root   親要素
     * @param name   コマンド名
     * @param desc   説明
     * @param admin  管理者権限が必要かどうか
     * @param action 処理
     * @return コマンドノード
     */
    protected CommandNode register(CommandNode root, String name, String desc, boolean admin,
            CommandGlassPane.CommandAction action) {
        CommandNode node = new CommandNode(root, name, desc, admin, action);
        root.getSubCommands().put(name, node);
        return node;
    }

    /**
     * 現在のシーンで利用できるかどうかのチェック
     * 
     * @param allowedTypes シーンタイプ
     * @return 許可されていればtrue、されていなければfalse
     */
    protected boolean isSceneValid(SceneType... allowedTypes) {
        SceneType current = SM.getSceneName();
        for (SceneType type : allowedTypes) {
            if (current == type)
                return true;
        }
        logText = "Command not available in the current scene.";
        log().addElement(logText);
        LogUtils.debugLog(logText);
        return false;
    }

    /**
     * {}の中身の要素を切り分けてマップにして返す
     * <p>
     * 例: key1=value1,key2=value2 → (key1,value1),(key2,value2)
     * 
     * @param arg {}の中身
     * @return 切り分けたもの
     */
    protected Map<String, String> parseNBT(String arg) {
        Map<String, String> NBT = new HashMap<>();

        String[] parts = arg.split(",", -1);
        for (String part : parts) {
            String[] pair = part.split("=", -1);
            String key = pair[0];
            String value = pair[1];

            NBT.put(key, value);
        }

        return NBT;
    }
}
