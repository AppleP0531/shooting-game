package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

/**
 * 加速弾
 * speed * accelStart で始まり、accelTime 秒かけて speed * accelEnd まで直線的に加速する。向きは固定。
 */
public class Accel extends Straight {
    private final double baseSpeed;

    public Accel(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec) {
        super(owner, BulletKinds.ACCEL, id, mode, pos, vec);
        baseSpeed = speed;
        speed = speedAt(baseSpeed, config.getAccelStart(), config.getAccelEnd(), config.getAccelTime(), 0);
    }

    @Override
    public void update(double dt) {
        speed = speedAt(baseSpeed, config.getAccelStart(), config.getAccelEnd(), config.getAccelTime(), lifeTimer);
        super.update(dt);
    }

    /**
     * 経過時間における速度
     *
     * @param base  基準速度
     * @param start 初速の倍率
     * @param end   最終速度の倍率
     * @param time  加速時間(秒)
     * @param t     経過時間(秒)
     * @return 速度
     */
    public static double speedAt(double base, double start, double end, double time, double t) {
        if (time <= 0 || t >= time)
            return base * end;
        if (t <= 0)
            return base * start;
        return base * (start + (end - start) * (t / time));
    }
}
