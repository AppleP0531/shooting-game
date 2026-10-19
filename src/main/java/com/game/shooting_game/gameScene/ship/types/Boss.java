package com.game.shooting_game.gameScene.ship.types;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.item.ResourceFactory;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.beam.BeamEmitter;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.bullet.barrage.Barrage;
import com.game.shooting_game.gameScene.bullet.barrage.BarrageSpec;
import com.game.shooting_game.gameScene.ship.BossLibrary;
import com.game.shooting_game.gameScene.ship.BossLibrary.BossDefinition;
import com.game.shooting_game.gameScene.ship.BossLibrary.GaugeDefinition;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * ボス
 * 
 * 複数のHPゲージを持ち、ゲージごとに動きと弾幕が切り替わる。
 * 流れ: 入場(無敵) → 戦闘 → ゲージ破壊 → 移行(無敵・弾消し・ドロップ) → 次のゲージの戦闘 … → 最後のゲージを削ると撃破
 */
public class Boss extends Ship {
    private static final String component = "[Ship:Boss]";

    // ゲージの色(ゲージ番号順、足りなければ繰り返す)
    private static final Color[] GAUGE_COLORS = {
            new Color(255, 90, 90),
            new Color(255, 170, 60),
            new Color(240, 230, 80),
            new Color(100, 220, 120),
            new Color(100, 170, 255)
    };

    private enum Phase {
        ENTRANCE, // 入場中(無敵)
        FIGHT, // 戦闘中
        TRANSITION, // ゲージ破壊後の移行中(無敵)
        RETREAT // 撤収中(無敵。上へ去っていく。撃破にはならない)
    }

    private final BossDefinition def; // 戦闘定義
    private final EntranceType entrance; // 入場軌道
    private final double entranceDuration; // 入場にかける時間
    private final Vector2 origin; // 入場開始位置
    private final Vector2 anchor; // 待機位置(戦闘中の動きの中心)
    private final int gaugeCount; // ゲージ数
    private final double gaugeHp; // 1ゲージあたりのHP

    private Phase phase = Phase.ENTRANCE;
    private int currentGauge = 0; // 今削っているゲージ(0始まり)
    private double entranceTimer; // 入場経過時間
    private double phaseTimer; // 現在のフェーズに入ってからの時間
    private double fightTimer; // 現在のゲージの戦闘時間
    private double totalFight; // 戦闘に入ってからの合計時間(撤収の判定用)
    private double coinCarry; // まだコインにしていない受けたダメージ
    private final List<Barrage> barrages = new ArrayList<>(); // 今のゲージの弾幕
    private final List<BeamEmitter> beamEmitters = new ArrayList<>(); // 今のゲージのビーム

    public Boss(String id, ColorMode mode, int level, Vector2 spawnPos, Vector2 stayPos, EntranceType entranceType) {
        super(ShipKinds.BOSS, id, mode, level, spawnPos);

        // ボスは画像を使わず仮描画で描く(色の概念もない)
        if (animationId.get("ship") != null) {
            AM.removeAnimation(animationId.get("ship"));
            animationId.remove("ship");
        }

        // データがなかった時用のデフォルト値
        ShipDefinition shipDef = SHIP_M.getShipConfig().get(owner).get(shipId);
        if (shipDef == null || shipDef.getDatas() == null) {
            collisionSize = 40 * ShootingGameMain.getScreenSizeAdj();
            speed = 100 * ShootingGameMain.getScreenSizeAdj();
            imageSize.put("ship", (int) (120 * ShootingGameMain.getScreenSizeAdj()));
        }
        for (String object : animationId.keySet()) {
            if (animationId.get(object) == null) {
                switch (object) {
                    case "ship" -> {
                        imageSize.put("ship", (int) (120 * ShootingGameMain.getScreenSizeAdj()));
                    }
                }
            }
        }

        rotate = Math.PI;

        // 戦闘定義とHP(ゲージ数 × 1ゲージ分)
        def = BossLibrary.get(id);
        gaugeCount = def.getGauges().size();
        gaugeHp = Math.max(1, def.getGaugeHp());
        maxHp = hp = gaugeHp * gaugeCount;

        // 入場の設定(StageConfigの指定があればそちらを優先)
        entrance = entranceType != null ? entranceType : def.getEntrance();
        entranceDuration = Math.max(0.5, def.getEntranceTime());
        origin = new Vector2(spawnPos);
        anchor = new Vector2(stayPos);
        alpha = 0f;
        animation("admission");

        // 船タイプログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Current shipId set to " + ANSI.YELLOW + shipId + ANSI.RESET + " (" + gaugeCount + " gauges).");
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        phaseTimer += dt;

        switch (phase) {
            case ENTRANCE -> {
                // 入場の序盤でフェードイン
                alpha = (float) Math.min(1.0, entranceTimer / entranceDuration * 3);
            }
            case FIGHT -> {
                alpha = 1f;
                fightTimer += dt;
                for (Barrage barrage : barrages)
                    barrage.update(dt, this);
                if (SHIP_M.getPlayer() != null)
                    for (BeamEmitter emitter : beamEmitters)
                        if (emitter.update(dt, 1.0))
                            BeamManager.getInstance().fireHostile(
                                    emitter.getSpec(), this, emitter.pickColor(SHIP_M.getPlayer().getMode()));

                totalFight += dt;
                checkGaugeBreak();

                // 一定時間で撤収するボス(無限モードのコインボス)
                if (phase == Phase.FIGHT && def.getRetreatTime() > 0 && totalFight >= def.getRetreatTime())
                    startRetreat();
            }
            case RETREAT -> {
                alpha = (float) Math.max(0.0, alpha - dt * 0.6);
            }
            case TRANSITION -> {
                // 無敵中は点滅
                alpha = (float) (0.55 + 0.45 * Math.sin(phaseTimer * 28));
                if (phaseTimer >= def.getTransitionTime())
                    startGauge();
            }
        }
    }

    @Override
    public void move(double dt) {
        switch (phase) {
            case ENTRANCE -> {
                // 入場軌道に沿って待機位置へ
                entranceTimer += dt;
                double t = entranceTimer / entranceDuration;
                if (t >= 1) {
                    pos.setX(anchor.getX());
                    pos.setY(anchor.getY());
                    startGauge();
                } else {
                    Vector2 p = entrance.evaluate(origin, anchor, t);
                    pos.setX(p.getX());
                    pos.setY(p.getY());
                }
            }
            case FIGHT -> {
                GaugeDefinition gauge = def.getGauges().get(currentGauge);
                double adj = ShootingGameMain.getScreenSizeAdj();
                double ampX = gauge.getMoveAmpX() * adj;
                double ampY = gauge.getMoveAmpY() * adj;
                double w = gauge.getMoveSpeed();

                double x = anchor.getX();
                double y = anchor.getY();
                switch (gauge.getMove()) {
                    // 左右に揺れる
                    case "SWAY" -> x += ampX * Math.sin(w * fightTimer);
                    // 8の字に動く
                    case "FIGURE8" -> {
                        x += ampX * Math.sin(w * fightTimer);
                        y += ampY * Math.sin(2 * w * fightTimer);
                    }
                    // それ以外は動かない
                    default -> {
                    }
                }
                pos.setX(x);
                pos.setY(y);
            }
            case RETREAT -> {
                // 上へ去り、画面から出たら取り除く(ドロップなし・撃破にはならない)
                pos.setY(pos.getY() - 380 * ShootingGameMain.getScreenSizeAdj() * dt);
                if (pos.getY() < -90 * ShootingGameMain.getScreenSizeAdj())
                    requestRemoval();
            }
            case TRANSITION -> {
                // 待機位置へ滑らかに戻る
                double k = Math.min(1, 4 * dt);
                pos.setX(pos.getX() + (anchor.getX() - pos.getX()) * k);
                pos.setY(pos.getY() + (anchor.getY() - pos.getY()) * k);
            }
        }
    }

    /**
     * 戦闘中以外(入場・移行中)はダメージを受けない
     */
    @Override
    public void addDamage(double damage) {
        if (phase != Phase.FIGHT)
            return;
        super.addDamage(damage);

        // ダメージを受けるたびにコインを落とすボス(1回に落とす数は少なめに抑える)
        if (def.getCoinStep() > 0) {
            coinCarry += damage;
            int drops = 0;
            while (coinCarry >= def.getCoinStep() && drops < 3) {
                coinCarry -= def.getCoinStep();
                drops++;
                double adj = ShootingGameMain.getScreenSizeAdj();
                Vector2 p = new Vector2(
                        pos.getX() + (Math.random() - 0.5) * 60 * adj,
                        pos.getY() + Math.random() * 30 * adj);
                ItemManager.getInstance().addItem(ResourceFactory.createResource(p, ResourceKinds.COIN));
            }
            if (coinCarry > def.getCoinStep() * 3)
                coinCarry = 0; // たまりすぎた分は捨てる
        }
    }

    /**
     * 撤収を始める(弾とビームを消して、上へ去る)
     */
    private void startRetreat() {
        phase = Phase.RETREAT;
        phaseTimer = 0;
        barrages.clear();
        beamEmitters.clear();
        BeamManager.getInstance().clearHostile();
        BulletManager.getInstance().clearEnemyBullets();

        LogUtils.systemLog("[INFO]", component, "Boss retreats.");
    }

    /**
     * 船体衝突のダメージは1ゲージの10%
     */
    @Override
    public double getRamDamage() {
        return gaugeHp * 0.1;
    }

    /**
     * 今のゲージの戦闘開始
     */
    private void startGauge() {
        phase = Phase.FIGHT;
        phaseTimer = 0;
        fightTimer = 0;

        barrages.clear();
        for (BarrageSpec spec : def.getGauges().get(currentGauge).getBarrages())
            barrages.add(new Barrage(spec));

        beamEmitters.clear();
        for (BeamSpec spec : def.getGauges().get(currentGauge).getBeams())
            beamEmitters.add(new BeamEmitter(spec));
    }

    /**
     * ゲージが破壊されたかのチェック
     */
    private void checkGaugeBreak() {
        // 撃破はShipManagerが処理する
        if (hp <= 0)
            return;

        // 空になったゲージの数
        int broken = gaugeCount - (int) Math.ceil(hp / gaugeHp - 1e-9);
        if (broken > currentGauge) {
            int clearedGauge = currentGauge;

            // 指定のゲージ数を壊されたら、ゲージ破壊のボーナスを落として撤収する
            if (def.getRetreatAfterGauges() > 0 && broken >= def.getRetreatAfterGauges()) {
                SHIP_M.dropBossBonus(this, def.getGauges().get(clearedGauge).getDropRolls());
                startRetreat();
                return;
            }

            // 次のゲージを満タンにして移行
            currentGauge = Math.min(gaugeCount - 1, broken);
            hp = (gaugeCount - currentGauge) * gaugeHp;

            startTransition(clearedGauge);
        }
    }

    /**
     * ゲージ破壊後の移行開始(弾消し・ドロップ・無敵)
     * 
     * @param clearedGauge 破壊したゲージ
     */
    private void startTransition(int clearedGauge) {
        phase = Phase.TRANSITION;
        phaseTimer = 0;
        barrages.clear();
        beamEmitters.clear();
        BeamManager.getInstance().clearHostile(); // ゲージ破壊で撃っていたビームも消える

        BulletManager.getInstance().clearEnemyBullets();
        SHIP_M.dropBossBonus(this, def.getGauges().get(clearedGauge).getDropRolls());

        LogUtils.systemLog(
                "[INFO]",
                component,
                "Gauge " + (clearedGauge + 1) + "/" + gaugeCount + " broken.");
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.draw(g);

        // 仮描画(画像は使わない)
        drawPlaceholder(g2);

        switch (animationName) {
            case "death" -> {
                clear();
            }
        }
    }

    /**
     * ボスの仮描画
     * 暗い機体に、今のゲージの色の縁取りと中心の光を付ける(色の概念はなく、青・赤では描かない)
     */
    private void drawPlaceholder(Graphics2D g2) {
        double s = imageSize.getOrDefault("ship", 120) / 2.0;
        double cx = pos.getX();
        double cy = pos.getY();
        double adj = ShootingGameMain.getScreenSizeAdj();
        Color accent = GAUGE_COLORS[currentGauge % GAUGE_COLORS.length];

        AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        UIBuilder.applyHighQualityRenderSettings(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

        // 機体(下向き)
        double[][] hull = {
                { 0, 0.95 }, { -1.0, -0.1 }, { -0.85, -0.6 }, { -0.35, -0.35 },
                { 0, -0.55 }, { 0.35, -0.35 }, { 0.85, -0.6 }, { 1.0, -0.1 }
        };
        int[] xs = new int[hull.length];
        int[] ys = new int[hull.length];
        for (int i = 0; i < hull.length; i++) {
            xs[i] = (int) (cx + hull[i][0] * s);
            ys[i] = (int) (cy + hull[i][1] * s);
        }
        g2.setColor(new Color(38, 40, 56));
        g2.fillPolygon(xs, ys, hull.length);
        g2.setStroke(new BasicStroke((float) (3 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(accent);
        g2.drawPolygon(xs, ys, hull.length);

        // 内側の模様
        g2.setStroke(new BasicStroke((float) (1.5 * adj)));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.drawLine((int) (cx - 0.7 * s), (int) (cy - 0.15 * s), (int) cx, (int) (cy + 0.6 * s));
        g2.drawLine((int) (cx + 0.7 * s), (int) (cy - 0.15 * s), (int) cx, (int) (cy + 0.6 * s));

        // 中心の光(ゆっくり脈打つ)
        double pulse = 0.85 + 0.15 * Math.sin(phaseTimer * 6);
        double r = 0.22 * s * pulse;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g2.fillOval((int) (cx - r * 1.7), (int) (cy - r * 1.7), (int) (r * 3.4), (int) (r * 3.4));
        g2.setColor(accent);
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
        g2.setColor(Color.WHITE);
        g2.fillOval((int) (cx - r * 0.45), (int) (cy - r * 0.45), (int) (r * 0.9), (int) (r * 0.9));

        g2.setStroke(new BasicStroke(1));
        g2.setTransform(oldTransform);
        g2.setComposite(oldComposite);
    }

    /**
     * HPバーの描画
     * 画面上部に今のゲージを横長で表示し、その下に残りゲージ数を小さな帯で表示する
     */
    @Override
    public void drawHpBar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        double adj = ShootingGameMain.getScreenSizeAdj();

        int margin = (int) (24 * adj);
        int barW = MAIN.getWidth() - margin * 2;
        int barH = (int) (8 * adj);
        int y = (int) (48 * adj); // コイン・スコア・プレイヤーHPバーの下

        // 今のゲージの残り割合(入場中はバーが伸びる演出)
        double ratio = (hp - (gaugeCount - currentGauge - 1) * gaugeHp) / gaugeHp;
        ratio = Math.max(0, Math.min(1, ratio));
        if (phase == Phase.ENTRANCE)
            ratio *= Math.max(0, Math.min(1, entranceTimer / entranceDuration));

        // 枠
        g2.setColor(new Color(40, 40, 40, 170));
        g2.fillRoundRect(margin, y, barW, barH, barH, barH);

        // 今のゲージ
        g2.setColor(GAUGE_COLORS[currentGauge % GAUGE_COLORS.length]);
        g2.fillRoundRect(margin, y, (int) (barW * ratio), barH, barH, barH);

        // 残りのゲージ(次に削るゲージから順に小さな帯で並べる)
        int pipW = (int) (22 * adj);
        int pipH = (int) (4 * adj);
        int gap = (int) (4 * adj);
        int pipY = y + barH + (int) (4 * adj);
        for (int i = currentGauge + 1; i < gaugeCount; i++) {
            int x = margin + (i - currentGauge - 1) * (pipW + gap);
            g2.setColor(GAUGE_COLORS[i % GAUGE_COLORS.length]);
            g2.fillRoundRect(x, pipY, pipW, pipH, pipH, pipH);
        }
    }
}
