package com.game.shooting_game.gameScene.ship;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Boss;
import com.game.shooting_game.gameScene.ship.types.Enemy;
import com.game.shooting_game.gameScene.ship.types.Player;
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
        return switch (owner) {
            case PLAYER -> new Player(shipId, mode, spawnPos);
            case ENEMY -> new Enemy(shipId, mode, level, spawnPos, stayPos);
            case BOSS -> new Boss(shipId, mode, level, spawnPos, stayPos);
            default -> throw new IllegalArgumentException("不明な機体の種類です: " + owner);
        };
    }
}
