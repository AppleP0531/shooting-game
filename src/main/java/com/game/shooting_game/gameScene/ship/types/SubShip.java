package com.game.shooting_game.gameScene.ship.types;

import java.awt.Graphics;
import java.awt.Graphics2D;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.domain.subship.SubShipRenderer;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.domain.subship.SubShipTypeService;
import com.game.shooting_game.domain.subship.SubShipTypeService.BeamDef;
import com.game.shooting_game.domain.subship.SubShipTypeService.LevelDef;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

/**
 * サブ機(自機の両端に付く)
 * 性能は制作したパーツ(SubShipService)で決まる。被弾せず、当たり判定も持たない。
 */
public class SubShip extends Ship {
    private static final String component = "[Ship:SubShip]";
    private final Player player;

    private final Vector2 posDiff; // 自機からの相対位置
    private double gunTimer; // 主砲の発射間隔タイマー
    private double missileTimer; // ミサイルの発射間隔タイマー
    private double time; // 経過時間(見た目の揺れ用)
    private double clock = 0; // 追従の遅れ計算用の時計
    private final java.util.LinkedList<double[]> history = new java.util.LinkedList<>(); // 自機の位置の記録(時刻, x, y)

    public SubShip(Player player, String id, ColorMode mode, Vector2 pos) {
        super(ShipKinds.PLAYER, id, mode, 0, pos);
        this.player = player;

        posDiff = new Vector2(
                pos.getX() - player.getPos().getX(),
                pos.getY() - player.getPos().getY());

        speed = 250 * ShootingGameMain.getScreenSizeAdj();
        collisionSize = 0;
        rotate = 0;
        // 見た目はSubShipRendererが描く(フレームのMkの画像 + 付けているパーツ)
        imageSize.put("ship", (int) (30 * ShootingGameMain.getScreenSizeAdj()));

        // 左右は同じタイミングで撃つ
        gunTimer = 0;
        missileTimer = 0;

        LogUtils.info(component,
                "Sub ship " + ANSI.YELLOW + id + ANSI.RESET + " attached.");
    }

    @Override
    public void update(double dt) {
        time += dt;
        // 自機と同じ色にする
        mode = player.getMode();

        if (!"none".equals(player.getAnimationName()))
            return;

        // 暴走状態(Lv5)の自機に合わせて連射が速くなる
        double rate = (player.getShipLevel() >= 5 ? 1.5 : 1.0) / SubShipService.rateScale();
        LevelDef def = SubShipService.def();

        // 通常弾
        if (def.straight() > 0) {
            gunTimer -= dt;
            if (gunTimer <= 0) {
                fireCannon(def);
                gunTimer = def.interval() / rate;
            }
        }

        // 追尾弾
        if (def.tracking() > 0) {
            missileTimer -= dt;
            if (missileTimer <= 0) {
                fireMissiles(def);
                missileTimer = def.trackInterval() / rate;
            }
        }
    }

    // ----- ビーム(左右で同じタイミングで撃つため、発射の管理は自機側(Player)が1つで行う) -----
    /**
     * ビームを撃てる場面か(エミッターのパーツを付けていて、ステージ中かロビーのプレビュー)
     */
    public boolean canBeam() {
        return SubShipService.beam() != null && (SM.getSceneName() == SceneType.GAME
                || SM.getSceneName() == SceneType.TEST || SM.getSceneName() == SceneType.LOBBY);
    }

    /**
     * ビームの定義(エミッターのMkに応じた性能)
     */
    public static BeamSpec createBeamSpec() {
        BeamSpec spec = new BeamSpec();
        spec.setOrigin(BeamSpec.Origin.SHIP_UP);
        spec.setColor("PLAYER");
        spec.setWarn(0.5);
        spec.setFade(0.2);
        BeamDef b = SubShipService.beam();
        if (b == null) // 撃てないレベルでは呼ばれない(念のため)
            b = new BeamDef(20, 14, 0.8, 8);
        spec.setDuration(b.duration());
        spec.setWidth(b.width());
        spec.setDamage(b.dps());
        spec.setInterval(b.interval());
        spec.setDelay(b.interval() * 0.25);
        return spec;
    }

    /**
     * ビームの発射間隔の倍率(スキルで短くなる。ロビーのプレビューでは短い間隔で繰り返し見せる)
     */
    public double beamIntervalScale() {
        double scale = 1.0 / (1 + SkillService.getBonus("BEAM_RATE"));
        if (SM.getSceneName() == SceneType.LOBBY)
            scale *= 0.4;
        return scale;
    }

    /**
     * 真上に狙える敵がいるか
     */
    public boolean hasTargetAhead() {
        return BeamManager.getInstance().hasTargetAhead(pos, 60 * ShootingGameMain.getScreenSizeAdj());
    }

    /**
     * ビームを撃つ
     */
    public void fireBeam(BeamSpec spec) {
        BeamManager.getInstance().firePlayer(
                spec,
                () -> new Vector2(pos.getX(), pos.getY() - 8 * ShootingGameMain.getScreenSizeAdj()),
                () -> player.getMode(),
                SubShipService.beamScale());
    }

    /**
     * 通常弾を撃つ(弾数に応じて扇状に広がる)
     */
    private void fireCannon(LevelDef def) {
        int shots = def.straight();
        int bulletId = 2;

        for (int i = 0; i < shots; i++) {
            double adj = (i - (shots - 1) / 2.0) * def.spread();
            shoot(BulletKinds.STRAIGHT, bulletId, -90 + adj, 1.0);
        }
    }

    /**
     * 追尾弾を撃つ(数に応じて扇状に広がる。あとは敵を追う)
     */
    private void fireMissiles(LevelDef def) {
        int count = def.tracking();

        for (int i = 0; i < count; i++) {
            double adj = (i - (count - 1) / 2.0) * 24;
            shoot(BulletKinds.TRACKING, 1, -90 + adj, 1.0);
        }
    }

    /**
     * 弾を撃つ
     * 
     * @param kind     弾種
     * @param bulletId 弾ID
     * @param deg      発射角(度)
     * @param dmgRate  ダメージ倍率
     */
    private void shoot(BulletKinds kind, int bulletId, double deg, double dmgRate) {
        Vector2 bulletPos = new Vector2(pos.getX(), pos.getY() - 8 * ShootingGameMain.getScreenSizeAdj());
        Bullet bullet = BulletFactory.createBullet(
                ShipKinds.PLAYER, kind, bulletId, mode, bulletPos, Math.toRadians(deg), null);
        bullet.setDamageScale(SubShipService.damageScale() * dmgRate);
        BM.addBullet(bullet);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        float alpha = player.getDamagedTimer() > 0 ? 0.6f : 1f;

        int[] look = SubShipService.look(SubShipTypeService.current(), SubShipService.level());
        SubShipRenderer.draw(g2, pos.getX(), pos.getY(), shipId.endsWith("_L"),
                look[0], look[1], look[2], look[3], look[4],
                SubShipService.typeIndex(SubShipTypeService.current()),
                mode, time, alpha, SubShipService.WORLD_SCALE);
    }

    @Override
    public void move(double dt) {
        clock += dt;

        // 自機の位置を記録し、少し前の位置を目標にする(自機の動きから遅れてついてくる)
        history.addLast(new double[] { clock, player.getPos().getX(), player.getPos().getY() });
        double delay = SubShipService.followDelay();
        double want = clock - delay;
        // 目標時刻より前の記録は、直前の1件だけ残して捨てる
        while (history.size() > 2 && history.get(1)[0] <= want)
            history.removeFirst();

        // 目標時刻をはさむ2件の記録の間を補間する
        double[] a = history.getFirst();
        double[] b = history.size() > 1 ? history.get(1) : a;
        double t = b[0] > a[0] ? Math.max(0, Math.min(1, (want - a[0]) / (b[0] - a[0]))) : 1;
        double targetX = a[1] + (b[1] - a[1]) * t + posDiff.getX();
        double targetY = a[2] + (b[2] - a[2]) * t + posDiff.getY();

        // カクつかないよう、さらにやわらかく寄せる
        double k = Math.min(1, 16 * dt);
        pos.setX(pos.getX() + (targetX - pos.getX()) * k);
        pos.setY(pos.getY() + (targetY - pos.getY()) * k);
    }

    @Override
    public void drawHpBar(Graphics g) {
        // 何もしない
    }
}
