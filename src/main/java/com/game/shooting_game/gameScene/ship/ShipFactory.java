package com.game.shooting_game.gameScene.ship;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Boss;
import com.game.shooting_game.gameScene.ship.types.Enemy;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.gameScene.ship.types.SmallEnemy;
import com.game.shooting_game.utils.Vector2;

public class ShipFactory {
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
        return new SmallEnemy(shipId, mode, level, pathId, mirror, delay, speedScale);
    }
}
