package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

/**
 * 分裂弾
 * まっすぐ進み、一定時間たつと扇状に広がる小さな弾(STRAIGHT)に分裂する。
 * 敵に当たったときは分裂せずにそのまま消える。
 */
public class Split extends Straight {
    private boolean split = false;

    public Split(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec) {
        super(owner, BulletKinds.SPLIT, id, mode, pos, vec);
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        if (!split && lifeTimer >= config.getSplitTime()) {
            split = true;
            burst();
            destroySelf();
        }
    }

    /**
     * 扇状に分裂する
     */
    private void burst() {
        int count = Math.max(1, config.getSplitCount());
        double spread = Math.toRadians(config.getSplitSpread());

        for (int i = 0; i < count; i++) {
            double offset = count == 1 ? 0 : spread * (i / (double) (count - 1) - 0.5);
            Bullet child = BulletFactory.createBullet(
                    owner, BulletKinds.STRAIGHT, config.getChildId(), mode,
                    new Vector2(pos), vec + offset, null);
            child.setDamageScale(damageScale * config.getChildRate());
            BM.addBullet(child);
        }
    }
}
