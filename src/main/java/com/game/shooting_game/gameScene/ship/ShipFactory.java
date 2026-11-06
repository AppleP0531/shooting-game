package com.game.shooting_game.gameScene.ship;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Boss;
import com.game.shooting_game.gameScene.ship.types.Enemy;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.gameScene.ship.types.SmallEnemy;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public class ShipFactory {
    private static final String component = "[ShipFactory]";
    private static final Set<String> warned = new HashSet<>(); // 警告を出した船(同じIDで何度も出さない)

    /**
     * 船の設定があるか(設定ファイルに載っていない船は作らない)
     * 
     * @param config 船の設定(ShipManager.getShipConfig())
     * @param owner  種類
     * @param shipId ID
     */
    static boolean isDefined(Map<ShipKinds, Map<String, ShipDefinition>> config, ShipKinds owner, String shipId) {
        if (config == null || owner == null || shipId == null)
            return false;
        Map<String, ShipDefinition> byId = config.get(owner);
        return byId != null && byId.get(shipId) != null;
    }

    /**
     * 設定のない船を作ろうとしたときの警告(同じIDでは1回だけ)
     */
    private static boolean checkDefined(ShipKinds owner, String shipId) {
        if (isDefined(ShipManager.getInstance().getShipConfig(), owner, shipId))
            return true;
        if (warned.add(owner + ":" + shipId))
            LogUtils.warn(component, "Unknown ship: " + ANSI.YELLOW + owner + "/" + shipId
                    + ANSI.RESET + ". Skipping spawn.");
        return false;
    }

    /**
     * 船の新規作成
     * 
     * @param owner    種類
     * @param shipId   ID
     * @param level    レベル
     * @param spawnPos スポーン位置
     * @param stayPos  待機位置
     */
    public static Ship createShip(ShipKinds owner, String shipId, ColorMode mode, int level, Vector2 spawnPos,
            Vector2 stayPos) {
        return createShip(owner, shipId, mode, level, spawnPos, stayPos, null);
    }

    /**
     * 船の新規作成(入場軌道指定)
     * 
     * @param owner    種類
     * @param shipId   ID
     * @param level    レベル
     * @param spawnPos スポーン位置
     * @param stayPos  待機位置
     * @param entrance 入場軌道(nullの場合、敵は直線、ボスは設定ファイルの指定)
     */
    public static Ship createShip(ShipKinds owner, String shipId, ColorMode mode, int level, Vector2 spawnPos,
            Vector2 stayPos, EntranceType entrance) {
        // 設定のない船は作らない(作ると、弾の設定を引く処理で止まる)
        if (!checkDefined(owner, shipId))
            return null;

        return switch (owner) {
            case PLAYER -> new Player(shipId, mode, spawnPos);
            case ENEMY -> new Enemy(shipId, mode, level, spawnPos, stayPos, entrance);
            case BOSS -> new Boss(shipId, mode, level, spawnPos, stayPos, entrance);
            default -> throw new IllegalArgumentException("不明な機体の種類です: " + owner);
        };
    }

    /**
     * 経路を通る小敵の新規作成
     * 
     * @param shipId     ID(ShipConfigのENEMY系)
     * @param level      レベル
     * @param pathId     経路ID
     * @param mirror     経路を左右反転するか
     * @param delay      動き出すまでの時間
     * @param speedScale 速度倍率
     */
    public static Ship createPathEnemy(String shipId, ColorMode mode, int level, String pathId, boolean mirror,
            double delay, double speedScale) {
        if (!checkDefined(ShipKinds.ENEMY, shipId))
            return null;

        return new SmallEnemy(shipId, mode, level, pathId, mirror, delay, speedScale);
    }
}
