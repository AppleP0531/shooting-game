package src.gameScene.ship;

import src.gameScene.ship.ShipManager.ShipKinds;
import src.gameScene.ship.types.Boss;
import src.gameScene.ship.types.Enemy;
import src.gameScene.ship.types.Player;
import src.utils.Vector2;

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
    public static Ship createShip(ShipKinds owner, String shipId, int level, Vector2 spawnPos, Vector2 stayPos) {
        return switch (owner) {
            case PLAYER -> new Player(shipId, spawnPos);
            case ENEMY -> new Enemy(shipId, level, spawnPos, stayPos);
            case BOSS -> new Boss(shipId, level, spawnPos, stayPos);
            default -> throw new IllegalArgumentException("不明な機体の種類です: " + owner);
        };
    }
}
