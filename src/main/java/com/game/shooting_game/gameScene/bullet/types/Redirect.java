package com.game.shooting_game.gameScene.bullet.types;

import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * 方向転換弾
 * redirectDistance だけ直進 → pauseTime 秒停止(点滅で予告) → その瞬間のプレイヤー位置を1回だけ記録して向き直り、
 * redirectSpeed 倍まで加速する(その後は追尾せず向き固定)。プレイヤーがいなければそのまま直進する。
 */
public class Redirect extends Bullet {
    public enum Phase {
        OUT, PAUSE, GO
    }

    private static final double GO_ACCEL_TIME = 0.4; // 向き直ってから最高速度に達するまでの時間

    private final double baseSpeed;
    private final double outDistance; // 直進する距離(画面倍率込み)
    private double traveled = 0; // 直進で進んだ距離
    private double pauseTimer = 0;
    private double goTimer = 0;
    private boolean turned = false;

    public Redirect(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec) {
        super(owner, BulletKinds.REDIRECT, id, mode, pos, vec);
        baseSpeed = speed;
        outDistance = config.getRedirectDistance() * ShootingGameMain.getScreenSizeAdj();
    }

    @Override
    public void update(double dt) {
        switch (phaseAt(traveled, outDistance, pauseTimer, config.getPauseTime())) {
            case OUT -> {
                speed = baseSpeed;
                traveled += speed * dt;
            }
            case PAUSE -> {
                speed = 0;
                pauseTimer += dt;
            }
            case GO -> {
                if (!turned) {
                    turned = true;
                    Ship player = SHIP_M.getPlayer();
                    if (player != null && player.getPos() != null)
                        vec = GameUtils.targetVec(pos, player.getPos());
                }
                goTimer += dt;
                speed = goSpeed(baseSpeed, config.getRedirectSpeed(), goTimer, GO_ACCEL_TIME);
            }
        }
        super.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        // 停止中は透明度を点滅させて予告する
        if (phaseAt(traveled, outDistance, pauseTimer, config.getPauseTime()) == Phase.PAUSE)
            alpha = (float) blinkAlpha(pauseTimer);
        else
            alpha = Math.min((float) lifeTimer * 4, 0.9f);
        super.draw(g);
    }

    /**
     * 現在の段階
     *
     * @param traveled   直進で進んだ距離
     * @param distance   直進する距離
     * @param pauseTimer 停止してからの経過時間
     * @param pauseTime  停止する時間
     * @return 段階
     */
    public static Phase phaseAt(double traveled, double distance, double pauseTimer, double pauseTime) {
        if (traveled < distance)
            return Phase.OUT;
        if (pauseTimer < pauseTime)
            return Phase.PAUSE;
        return Phase.GO;
    }

    /**
     * 向き直ったあとの速度(base から base*mult へ time 秒で直線的に加速)
     */
    public static double goSpeed(double base, double mult, double t, double time) {
        double target = base * mult;
        if (time <= 0 || t >= time)
            return target;
        if (t <= 0)
            return base;
        return base + (target - base) * (t / time);
    }

    /**
     * 停止中の点滅(0.5〜0.9)
     */
    public static double blinkAlpha(double t) {
        return 0.7 + 0.2 * Math.sin(t * 24);
    }
}
