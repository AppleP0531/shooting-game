package com.game.shooting_game.gameScene.bullet;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.bullet.types.Ball;
import com.game.shooting_game.gameScene.bullet.types.Laser;
import com.game.shooting_game.gameScene.bullet.types.Straight;
import com.game.shooting_game.gameScene.bullet.types.Tracking;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

public class BulletFactory {
    /**
     * 弾の作成
     * 
     * @param owner 発射者
     * @param kind  弾種類
     * @param id    弾ID
     * @param pos   発射位置
     * @param vec   発射角
     * @return 弾
     */
    public static Bullet createBullet(ShipKinds owner, BulletKinds kind, int id, ColorMode mode, Vector2 pos,
            double vec) {
        return switch (kind) {
            case STRAIGHT -> new Straight(owner, id, mode, pos, vec);
            case TRACKING -> new Tracking(owner, id, mode, pos, vec);
            case LASER -> new Laser(owner, id, mode, pos, vec);
            case BALL -> new Ball(owner, id, mode, pos, vec);
            default -> throw new IllegalArgumentException("不明な弾の種類です: " + kind);
        };
    }
}