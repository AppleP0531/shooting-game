package com.game.shooting_game.gameScene.bullet;

import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.bullet.types.*;

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
    public static Bullet createBullet(ShipKinds owner, BulletKinds kind, int id, Vector2 pos,
            double vec) {
        return switch (kind) {
            case STRAIGHT -> new Straight(owner, id, pos, vec);
            case TRACKING -> new Tracking(owner, id, pos, vec);
            case LASER -> new Laser(owner, id, pos, vec);
            case BALL -> new Ball(owner, id, pos, vec);
            default -> throw new IllegalArgumentException("不明な弾の種類です: " + kind);
        };
    }
}