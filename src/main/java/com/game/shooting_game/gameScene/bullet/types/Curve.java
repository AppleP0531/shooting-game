package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

public class Curve extends Straight {
    private final double DIFF; // 時間経過ごとの変角量
    private final Direction DIRECTION;

    public Curve(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec, Direction direction) {
        super(owner, BulletKinds.CURVE, id, mode, pos, vec);

        this.DIFF = BM.getBulletConfig().get(owner).get(BulletKinds.CURVE).get(id).getDiff();
        this.DIRECTION = direction;
    }

    @Override
    public void move(double dt) {
        vec += DIFF * DIRECTION.getSign() * dt;

        super.move(dt);
    }
}
