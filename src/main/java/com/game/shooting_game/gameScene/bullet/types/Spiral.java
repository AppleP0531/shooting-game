package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.Vector2;

/**
 * らせん弾
 * 進行方向の軸に沿って進みながら、軸の左右に振れる。
 * 方向(Direction)で最初に振れる側が決まるので、RIGHTとLEFTを同時に撃つと二重らせんになる。
 * 振れ幅は撃った直後から少しずつ大きくなり、広がるように見える。
 */
public class Spiral extends Straight {
    private static final double RAMP_TIME = 0.25; // 振れ幅が最大になるまでの時間(秒)

    private final double amplitude; // 振れ幅
    private final double frequency; // 振れの速さ(rad/秒)
    private final double sign; // 最初に振れる側(右=+1、左=-1)
    private final double baseVec; // 軸の方向
    private final Vector2 axis; // 軸上の現在位置

    public Spiral(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec, Direction direction) {
        super(owner, BulletKinds.SPIRAL, id, mode, pos, vec);

        this.amplitude = config.getAmplitude() * ShootingGameMain.getScreenSizeAdj();
        this.frequency = config.getFrequency();
        this.sign = direction == Direction.LEFT ? -1 : 1;
        this.baseVec = vec;
        this.axis = new Vector2(pos);
    }

    @Override
    public void move(double dt) {
        // 軸に沿って進む
        axis.setX(axis.getX() + Math.cos(baseVec) * speed * dt);
        axis.setY(axis.getY() + Math.sin(baseVec) * speed * dt);

        // 軸の横方向への振れ
        double ramp = Math.min(1, lifeTimer / RAMP_TIME);
        double phase = frequency * lifeTimer;
        double lateral = amplitude * ramp * Math.sin(phase) * sign;
        double perp = baseVec + Math.PI / 2;
        pos.setX(axis.getX() + Math.cos(perp) * lateral);
        pos.setY(axis.getY() + Math.sin(perp) * lateral);

        // 弾の向きは、軸方向と横方向の速度を合わせた向きにする
        double lateralSpeed = amplitude * ramp * frequency * Math.cos(phase) * sign;
        vec = baseVec + Math.atan2(lateralSpeed, Math.max(1, speed));
    }
}
