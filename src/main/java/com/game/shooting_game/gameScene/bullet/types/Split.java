package com.game.shooting_game.gameScene.bullet.types;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * 分裂弾
 * まっすぐ進み、一定時間(または一定距離)たつと小さな弾(STRAIGHT)に分裂する。
 * 形は splitShape で FAN(扇) / RING(全周) / AIM(プレイヤーへ向けた扇)。
 * 敵に当たったときは分裂せずにそのまま消える。
 */
public class Split extends Straight {
    private boolean split = false;
    private final Vector2 startPos; // 発射位置(距離で分裂するときの基準)
    private final double splitDistance; // 分裂する距離(画面倍率込み。0なら時間で分裂)

    public Split(ShipKinds owner, int id, ColorMode mode, Vector2 pos, double vec) {
        super(owner, BulletKinds.SPLIT, id, mode, pos, vec);
        startPos = new Vector2(pos);
        splitDistance = config.getSplitDistance() * ShootingGameMain.getScreenSizeAdj();
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        if (!split && shouldSplit(lifeTimer, config.getSplitTime(), traveled(), splitDistance)) {
            split = true;
            burst();
            destroySelf();
        }
    }

    private double traveled() {
        return Math.hypot(pos.getX() - startPos.getX(), pos.getY() - startPos.getY());
    }

    /**
     * 分裂するか(distance が0より大きければ距離で、そうでなければ時間で判定)
     */
    public static boolean shouldSplit(double lifeTimer, double splitTime, double traveled, double distance) {
        return distance > 0 ? traveled >= distance : lifeTimer >= splitTime;
    }

    /**
     * 分裂後の各弾の角度
     *
     * @param shape       FAN(親の向きを中心に扇) / RING(親の向きから全周を等分) / AIM(狙い角を中心に扇)
     * @param count       弾の数
     * @param spread      扇の広がり(ラジアン、全体)
     * @param parentAngle 親の向き
     * @param aimAngle    プレイヤーへの角度(AIM用。NaNなら親の向きにフォールバック)
     * @return 角度の配列
     */
    public static double[] childAngles(String shape, int count, double spread, double parentAngle,
            double aimAngle) {
        count = Math.max(1, count);
        String s = shape == null ? "FAN" : shape.trim().toUpperCase();
        double[] angles = new double[count];
        if (s.equals("RING")) {
            for (int i = 0; i < count; i++)
                angles[i] = parentAngle + Math.PI * 2 * i / count;
            return angles;
        }
        double center = s.equals("AIM") && !Double.isNaN(aimAngle) ? aimAngle : parentAngle;
        for (int i = 0; i < count; i++)
            angles[i] = center + (count == 1 ? 0 : spread * (i / (double) (count - 1) - 0.5));
        return angles;
    }

    /**
     * 分裂する
     */
    private void burst() {
        int count = Math.max(1, config.getSplitCount());
        double spread = Math.toRadians(config.getSplitSpread());
        double aim = Double.NaN;
        Ship player = SHIP_M.getPlayer();
        if (player != null && player.getPos() != null)
            aim = GameUtils.targetVec(pos, player.getPos());

        for (double angle : childAngles(config.getSplitShape(), count, spread, vec, aim)) {
            Bullet child = BulletFactory.createBullet(
                    owner, BulletKinds.STRAIGHT, config.getChildId(), mode,
                    new Vector2(pos), angle, null);
            child.setDamageScale(damageScale * config.getChildRate());
            BM.addBullet(child);
        }
    }
}
