package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

public class Wave extends Straight {
    private static final double WAVE_FREQUENCY = 5.0; // 波の速さ
    private static final double WAVE_AMPLITUDE = Math.toRadians(30); // 振れ角

    private final double BASE_VEC;
    private final Direction DIRECTION;

    public Wave(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec, Direction direction) {
        super(owner, BulletKinds.WAVE, id, mode, pos, vec);

        this.BASE_VEC = vec;
        this.DIRECTION = direction;
    }

    @Override
    public void move(double dt) {
        vec = BASE_VEC + Math.sin(lifeTimer * WAVE_FREQUENCY) * WAVE_AMPLITUDE * DIRECTION.getSign();

        super.move(dt);
    }
}
