package com.game.shooting_game.gameScene.bullet.barrage;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * 弾幕の実行
 * 
 * 従来の「クールタイムごとに決まった方向へ撃つ」方式ではなく、
 * 経過時間から発射角・速度・色を計算して、円・渦巻き・花形などの模様を描く。
 */
public class Barrage {
    private static final double MIN_INTERVAL = 0.02; // 発射間隔の下限
    private static final int MAX_CATCH_UP = 4; // 1フレームで追いつく最大発射回数

    private final BarrageSpec spec;
    private final BulletManager BM;

    private double elapsed; // 開始してからの経過時間
    private double timer; // 発射間隔用タイマー
    private int volley; // 発射した回数

    public Barrage(BarrageSpec spec) {
        this.spec = spec;
        this.BM = BulletManager.getInstance();
    }

    /**
     * 更新
     * 
     * @param dt      経過時間
     * @param shooter 撃つ船
     */
    public void update(double dt, Ship shooter) {
        elapsed += dt;
        if (elapsed < spec.getDelay())
            return;

        double t = elapsed - spec.getDelay(); // 弾幕としての経過時間
        double interval = Math.max(MIN_INTERVAL, spec.getInterval());

        // 撃つ/休むの繰り返し(休み中は次に撃つ準備だけしておく)
        if (spec.getOnTime() > 0 && spec.getOffTime() > 0) {
            double cycle = spec.getOnTime() + spec.getOffTime();
            if (t % cycle >= spec.getOnTime()) {
                timer = interval;
                return;
            }
        }

        timer += dt;
        int guard = 0;
        while (timer >= interval && guard++ < MAX_CATCH_UP) {
            timer -= interval;
            fire(shooter, t);
            volley++;
        }
        // 重い処理で大きく遅れた場合は溜めずに捨てる
        if (timer >= interval)
            timer = 0;
    }

    /**
     * 1回分の発射
     */
    private void fire(Ship shooter, double t) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        Vector2 offset = spec.getOffset() == null ? new Vector2(0, 0) : spec.getOffset();
        Vector2 origin = new Vector2(
                shooter.getPos().getX() + offset.getX() * adj,
                shooter.getPos().getY() + offset.getY() * adj);

        // 時間とともに回転する基準角(ラジアン)
        double base = Math.toRadians(spec.getStartAngle() + spec.getSpin() * t);

        switch (spec.getType()) {
            case RING, SPIRAL -> fireRing(origin, base);
            case FAN -> fireFan(origin, t);
            case FLOWER -> fireFlower(origin, base);
            case POLYGON -> firePolygon(origin, base);
            case CURTAIN -> fireCurtain(origin, t);
            case ANCHOR -> fireAnchor(origin, base, t);
        }
    }

    /**
     * 照準点を予告して、予告が終わったらそこを中心に弾を爆発させる
     * 照準する点は撃った瞬間に決まり、予告の間に自機が動いても変わらない(スナップショット)。
     */
    private void fireAnchor(Vector2 origin, double base, double t) {
        Vector2 target = anchorPoint(origin);
        com.game.shooting_game.gameScene.bullet.barrage.AnchorMarkers.getInstance().add(target, spec.getWarnTime(),
                () -> burstAt(target, base, t));
    }

    /**
     * 照準する点(画面座標)
     */
    private Vector2 anchorPoint(Vector2 origin) {
        ShootingGameMain main = ShootingGameMain.getInstance();
        double adj = ShootingGameMain.getScreenSizeAdj();
        String rule = spec.getAnchor() == null ? "player" : spec.getAnchor().toLowerCase();
        switch (rule) {
            case "fixed" -> {
                Vector2 p = spec.getAnchorPos() == null ? new Vector2(0, 200) : spec.getAnchorPos();
                return new Vector2(main.getWidth() / 2.0 + p.getX() * adj, p.getY() * adj);
            }
            case "random" -> {
                java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
                return new Vector2(rnd.nextDouble(main.getWidth() * 0.1, main.getWidth() * 0.9),
                        rnd.nextDouble(main.getHeight() * 0.15, main.getHeight() * 0.55));
            }
            default -> {
                Ship player = ShipManager.getInstance().getPlayer();
                return player != null ? new Vector2(player.getPos()) : new Vector2(main.getWidth() / 2.0, main.getHeight() * 0.7);
            }
        }
    }

    /**
     * 照準点を中心にした爆発(burst の形で、既存の模様をそのまま使う)
     */
    private void burstAt(Vector2 target, double base, double t) {
        switch (spec.getBurst() == null ? BarrageSpec.Type.RING : spec.getBurst()) {
            case FLOWER -> fireFlower(target, base);
            case POLYGON -> firePolygon(target, base);
            case FAN -> fireFan(target, t);
            default -> fireRing(target, base);
        }
        volley++;
    }

    /**
     * 全周に等間隔
     */
    private void fireRing(Vector2 origin, double base) {
        int n = Math.max(1, spec.getCount());
        for (int i = 0; i < n; i++) {
            double angle = base + Math.PI * 2 * i / n;
            emit(origin, angle, spec.getSpeed(), colorOf(i, n, n));
        }
    }

    /**
     * 扇状(狙いと左右の振り付き)
     */
    private void fireFan(Vector2 origin, double t) {
        double aim = Math.PI / 2; // 下
        if ("target".equalsIgnoreCase(spec.getAim()) && ShipManager.getInstance().getPlayer() != null)
            aim = GameUtils.targetVec(origin, ShipManager.getInstance().getPlayer().getPos());

        double center = aim
                + Math.toRadians(spec.getStartAngle())
                + Math.toRadians(spec.getSweepAmp()) * Math.sin(spec.getSweepSpeed() * t);
        double spread = Math.toRadians(spec.getSpread());

        int n = Math.max(1, spec.getCount());
        for (int i = 0; i < n; i++) {
            double angle = n == 1 ? center : center + spread * ((double) i / (n - 1) - 0.5);
            emit(origin, angle, spec.getSpeed(), colorOf(i, n, n));
        }
    }

    /**
     * 花形
     * 角度ごとに速度を変えると、同時に撃った弾が作る輪郭が相似形のまま広がる
     */
    private void fireFlower(Vector2 origin, double base) {
        int n = Math.max(6, spec.getCount());
        int petals = Math.max(1, spec.getPetals());
        double depth = Math.max(0, Math.min(0.9, spec.getDepth()));

        for (int i = 0; i < n; i++) {
            double rel = Math.PI * 2 * i / n;
            double scale = (1 - depth) + depth * (0.5 + 0.5 * Math.cos(petals * rel));
            emit(origin, base + rel, spec.getSpeed() * scale, colorOf(i, n, petals));
        }
    }

    /**
     * 多角形
     * 頂点方向を最速、辺の中央を遅くして、広がる多角形の輪郭を描く
     */
    private void firePolygon(Vector2 origin, double base) {
        int sides = Math.max(3, spec.getSides());
        int n = Math.max(sides, spec.getCount());
        double sector = Math.PI * 2 / sides;

        for (int i = 0; i < n; i++) {
            double rel = Math.PI * 2 * i / n;
            double phi = rel % sector;
            double scale = Math.cos(Math.PI / sides) / Math.cos(phi - Math.PI / sides);
            emit(origin, base + rel, spec.getSpeed() * scale, colorOf(i, n, sides));
        }
    }

    /**
     * 弾の幕(隙間が左右に動く)
     */
    private void fireCurtain(Vector2 origin, double t) {
        double width = ShootingGameMain.getInstance().getWidth();
        int n = Math.max(2, spec.getCount());
        double gapHalf = Math.max(0, spec.getGap()) * width / 2;
        double gapX = width / 2 + (width / 2 - gapHalf) * Math.sin(spec.getSweepSpeed() * t);
        double angle = Math.PI / 2 + Math.toRadians(spec.getStartAngle());

        for (int i = 0; i < n; i++) {
            double x = (i + 0.5) * width / n;
            // 隙間の範囲は撃たない
            if (Math.abs(x - gapX) < gapHalf)
                continue;

            emit(new Vector2(x, origin.getY()), angle, spec.getSpeed(), colorOf(i, n, n));
        }
    }

    /**
     * 弾の色の決定
     * 
     * @param index    今回の発射内での番号
     * @param total    今回の発射数
     * @param segments ALT_SEGMENT用の区切り数
     */
    private ColorMode colorOf(int index, int total, int segments) {
        String rule = spec.getColor() == null ? "ALT_ARM" : spec.getColor();
        return switch (rule) {
            case "BLUE" -> ColorMode.BLUE;
            case "RED" -> ColorMode.RED;
            case "ALT_VOLLEY" -> volley % 2 == 0 ? ColorMode.BLUE : ColorMode.RED;
            case "ALT_SEGMENT" -> ((int) Math.floor(index * (double) Math.max(1, segments) / total)) % 2 == 0
                    ? ColorMode.BLUE
                    : ColorMode.RED;
            default -> index % 2 == 0 ? ColorMode.BLUE : ColorMode.RED;
        };
    }

    /**
     * 弾を1発撃つ
     */
    private void emit(Vector2 pos, double angle, double speedScale, ColorMode mode) {
        Bullet bullet = BulletFactory.createBullet(
                ShipKinds.BOSS,
                spec.getBulletKind(),
                spec.getBulletId(),
                mode,
                new Vector2(pos), // 弾ごとに別の位置オブジェクトにする(共有すると互いの移動が打ち消し合う)
                angle,
                spec.getDirection());
        bullet.scaleSpeed(speedScale);
        BM.addBullet(bullet);
    }
}
