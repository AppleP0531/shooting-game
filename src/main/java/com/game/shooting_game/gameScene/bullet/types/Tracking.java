package com.game.shooting_game.gameScene.bullet.types;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.Vector2;

public class Tracking extends Bullet {
    private double maxSpeed;
    private Ship target;
    private final double TURNING_SMOOTHNESS = 5.0;

    public Tracking(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec) {
        super(owner, BulletKinds.TRACKING, id, mode, pos, vec);
        target = setTarget();
        lifeTime = 5; // 存在時間5秒

        maxSpeed = speed;
        speed = 0; // 初速度0に初期化
    }

    @Override
    public void update(double dt) {

        speedAdj(dt);
        if (lifeTimer >= 0.3)
            vecAdj(dt);
        super.update(dt);
    }

    @Override
    public void draw(Graphics g) {
        alpha = Math.min((float) lifeTimer * 4, 0.9f);
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(owner == ShipKinds.PLAYER ? new Color(0, 1, 1, alpha) : new Color(1, 0, 0, alpha));

            int s = 15;
            g2.setStroke(new BasicStroke(4));
            g2.drawLine((int) pos.getX(), (int) pos.getY(),
                    (int) (pos.getX() + Math.cos(vec) * s),
                    (int) (pos.getY() + Math.sin(vec) * s));

            // 線の太さをリセット
            g2.setStroke(new BasicStroke(1));
        }
    }

    /**
     * 時間に基づいた速度調整
     * 
     * @param dt 時間
     */
    private void speedAdj(double dt) {
        speed += 750 * dt;
        speed = Math.clamp(speed, 0, maxSpeed);
    }

    /**
     * 時間に基づいた進行角度調整
     * 
     * @param dt 時間
     */
    private void vecAdj(double dt) {
        // ターゲットが存在しなければ新しいターゲットを設定
        if (target == null || !SHIP_M.findEnemy(target)) {
            target = setTarget();
            return;
        }

        double targetVec = GameUtils.targetVec(this.getPos(), target.getPos());
        double angleDef = (targetVec - vec);
        angleDef = GameUtils.normalizeAngle(angleDef);

        // 一秒あたりの最大旋回角の設定 (プレイヤー:300 , 敵:100)
        double MaxTurnRate = Math.toRadians(owner == ShipKinds.PLAYER ? 300 : 100);

        vec += Math.clamp(angleDef / TURNING_SMOOTHNESS, -MaxTurnRate * dt, MaxTurnRate * dt);

        vec = GameUtils.normalizeAngle(vec);
    }

    /**
     * ターゲットの設定
     * 
     * @return 設定されたターゲット
     */
    private Ship setTarget() {
        // プレイヤーの弾なら最も近い敵を、敵の弾ならプレイヤーをターゲットとする
        return owner == ShipKinds.PLAYER ? SHIP_M.getNearestTargetForPlayer(pos) : SHIP_M.getPlayer();
    }
}