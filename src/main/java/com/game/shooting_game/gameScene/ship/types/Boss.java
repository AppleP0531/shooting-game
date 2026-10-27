package com.game.shooting_game.gameScene.ship.types;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
import com.game.shooting_game.gameScene.ship.BossLook;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * ボス
 * 
 * 複数のHPゲージを持ち、ゲージごとに動きと弾幕が切り替わる。
 * 流れ: 入場(無敵) → 戦闘 → ゲージ破壊 → 移行(無敵・弾消し・ドロップ) → 次のゲージの戦闘 … → 最後のゲージを削ると撃破
 */
public class Boss extends Ship {
    @Override
    protected double statusResist(com.game.shooting_game.gameScene.status.StatusEffect effect) {
        // ボスは効きにくい(感電は効かない。ほかは時間が半分)
        return effect == com.game.shooting_game.gameScene.status.StatusEffect.SHOCK ? 0 : 0.5;
    }

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
    private final Vector2 anchor; // 待機位置(最初の動きの中心。以降は、ゲージが変わった場所が動きの中心になる)
    private final Vector2 moveBase = new Vector2(0, 0); // いまのゲージの動きの中心
    private final int gaugeCount; // ゲージ数
    private final double gaugeHp; // 1ゲージあたりのHP

    private Phase phase = Phase.ENTRANCE;
    private int currentGauge = 0; // 今削っているゲージ(0始まり)
    private double entranceTimer; // 入場経過時間
    private double phaseTimer; // 現在のフェーズに入ってからの時間
    private double fightTimer; // 現在のゲージの戦闘時間
    private double totalFight; // 戦闘に入ってからの合計時間(撤収の判定用)
    private static final int MAX_COIN_DROPS_PER_FRAME = 4; // 1フレームに落とすコインの上限
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
        def = BossLibrary.forFight(id, com.game.shooting_game.gameScene.stage.StageManager.getInstance().getStageId());
        gaugeCount = def.getGauges().size();
        gaugeHp = Math.max(1, def.getGaugeHp())
                * com.game.shooting_game.gameScene.stage.StageModifiers.enemyHpRate(); // 難易度などの硬さはゲージのHPで表す
        maxHp = hp = gaugeHp * gaugeCount;
        // 無限モードのコインボスは、前の登場で削ったHPを引き継ぐ(再登場で満タンに戻らない)
        if (isEndlessCoinBoss(id)) {
            hp = Math.max(1, maxHp * com.game.shooting_game.gameScene.stage.CoinBossMemory.remainingRatio());
            currentGauge = Math.min(gaugeCount - 1, Math.max(0, gaugeCount - (int) Math.ceil(hp / gaugeHp - 1e-9)));
        }

        // 入場の設定(StageConfigの指定があればそちらを優先)
        entrance = entranceType != null ? entranceType : def.getEntrance();
        entranceDuration = Math.max(0.5, def.getEntranceTime());
        origin = new Vector2(spawnPos);
        anchor = new Vector2(stayPos);
        alpha = 0f;
        animation("admission");

        // 船タイプログ
        LogUtils.info(component,
                "Current shipId set to " + ANSI.YELLOW + shipId + ANSI.RESET + " (" + gaugeCount + " gauges).");
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        phaseTimer += dt;
        bodyClock += dt;
        payCoins();

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
                // 無敵中。点滅はせず、機体の震え・光・衝撃波で形態の変化を見せる(描画側)
                alpha = 1f;
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

                double x = moveBase.getX();
                double y = moveBase.getY();
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
                // その場で次の形態に変わる(動かない)
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
        // どんなに大きいダメージでも、一度に削れるのは今のゲージの残りまで(余りは捨てる)。
        // コマンドの /kill は addDamage を通らないので、ゲージに関係なく倒せる
        double floorHp = (gaugeCount - currentGauge - 1) * gaugeHp;
        double allowed = Math.max(0, hp - floorHp);
        if (damage >= allowed) {
            if (allowed <= 0)
                return;
            damage = allowed;
            double before = hp;
            super.addDamage(damage);
            if (hp < before) // シールド中は減っていない
                hp = floorHp; // 誤差で次のゲージにはみ出したり、わずかに残ったりしないようにする
        } else {
            super.addDamage(damage);
        }

        // ダメージを受けたぶんだけコインを落とす(被弾の回数ではなく、受けたダメージの合計に比例する)
        if (def.getCoinStep() > 0)
            coinCarry += damage;
    }

    /**
     * たまったダメージをコインにして落とす
     * 1フレームに落とす数は抑え、残りは次のフレーム以降に持ち越す(捨てないので、合計はダメージに比例する)
     */
    private void payCoins() {
        double step = def.getCoinStep();
        if (step <= 0)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        int drops = 0;
        while (coinCarry >= step && drops < MAX_COIN_DROPS_PER_FRAME) {
            coinCarry -= step;
            drops++;
            Vector2 p = new Vector2(
                    pos.getX() + (Math.random() - 0.5) * 60 * adj,
                    pos.getY() + Math.random() * 30 * adj);
            ItemManager.getInstance().addItem(ResourceFactory.createResource(p, ResourceKinds.COIN));
        }
    }

    /**
     * 形態変化の最中か(ゲージ破壊後の移行中。この間、自機の暴走のカウントは止まる)
     */
    public boolean isTransitioning() {
        return phase == Phase.TRANSITION;
    }

    /** 無限モードのコインボスか(再登場でHPを引き継ぐ対象) */
    public static boolean isEndlessCoinBoss(String bossId) {
        return BossLibrary.COIN_BOSS_ID.equals(bossId)
                && com.game.shooting_game.gameScene.stage.StageManager.getInstance().getStageId()
                        == com.game.shooting_game.gameScene.stage.EndlessWaves.STAGE_ID;
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

        // 撤収するコインボスは、残りHPの割合を覚えておく(次の登場に引き継ぐ)
        if (isEndlessCoinBoss(shipId))
            com.game.shooting_game.gameScene.stage.CoinBossMemory.save(hp / maxHp);

        LogUtils.info(component, "Boss retreats.");
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

        // ゲージが変わった場所から、そのまま次の形態の動きを始める(中央へは戻らない)。
        // 何度も続けて離れていかないよう、待機位置からの距離には上限を付ける
        double adj = ShootingGameMain.getScreenSizeAdj();
        moveBase.setX(Math.max(anchor.getX() - 90 * adj, Math.min(anchor.getX() + 90 * adj, pos.getX())));
        moveBase.setY(Math.max(anchor.getY() - 40 * adj, Math.min(anchor.getY() + 40 * adj, pos.getY())));

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

        LogUtils.info(component,
                "Gauge " + (clearedGauge + 1) + "/" + gaugeCount + " broken.");
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.draw(g);

        // 形態が変わる間は、ためて(縮んで震える)→ 弾けて(膨らむ)→ 落ち着く、の動きを機体に付ける
        AffineTransform oldTransform = g2.getTransform();
        if (phase == Phase.TRANSITION)
            applyTransitionTransform(g2);

        // 種類・形態ごとのPNG画像で描く。画像がなければ図形の仮描画にする
        if (!drawBodyImage(g2))
            drawPlaceholder(g2);

        g2.setTransform(oldTransform);
        if (phase == Phase.TRANSITION)
            drawTransitionEffect(g2);

        switch (animationName) {
            case "death" -> {
                clear();
            }
        }
    }

    // ---- 形態変化の演出 ----
    private static final double BURST_AT = 0.45; // 移行の進み具合のうち、ためが終わって弾ける位置(0〜1)

    /**
     * 移行の進み具合(0〜1。移行中でなければ1)
     */
    private double transitionProgress() {
        return phase == Phase.TRANSITION ? Math.min(1, phaseTimer / Math.max(0.1, def.getTransitionTime())) : 1;
    }

    /**
     * 前の形態から新しい形態へ切り替わる具合(0〜1)。弾ける瞬間のあたりで滑らかに入れ替わる。
     */
    private double formBlend() {
        double x = Math.clamp((transitionProgress() - BURST_AT + 0.08) / 0.3, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /**
     * 機体の動き: ためる間は少し縮んで震えが強まり、弾ける瞬間に膨らんでから元の大きさに落ち着く
     */
    private void applyTransitionTransform(Graphics2D g2) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double p = transitionProgress();
        double dx = 0;
        double dy = 0;
        double scale;
        if (p < BURST_AT) {
            double c = p / BURST_AT; // ため(0→1)
            double amp = c * c * 3 * adj;
            dx = Math.sin(phaseTimer * 47) * amp;
            dy = Math.cos(phaseTimer * 39) * amp * 0.6;
            scale = 1 - 0.06 * c;
        } else {
            double q = (p - BURST_AT) / (1 - BURST_AT); // 弾けたあと(0→1)
            scale = 0.94 + 0.06 * q + 0.14 * Math.sin(Math.PI * Math.min(1, q * 2)) * (1 - q);
        }
        g2.translate(pos.getX() + dx, pos.getY() + dy);
        g2.scale(scale, scale);
        g2.translate(-pos.getX(), -pos.getY());
    }

    /**
     * 光と衝撃波: ためる間は光が集まり、弾けた瞬間に光がふくらんで輪が広がる
     */
    private void drawTransitionEffect(Graphics2D g2) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double p = transitionProgress();
        double r = imageSize.getOrDefault("ship", 120) / 2.0;
        Color accent = BossLook.of(shipId).accent();
        Color bright = new Color((accent.getRed() + 255 * 2) / 3, (accent.getGreen() + 255 * 2) / 3,
                (accent.getBlue() + 255 * 2) / 3);
        double cx = pos.getX();
        double cy = pos.getY();

        var oldComposite = g2.getComposite();
        var oldPaint = g2.getPaint();
        var oldStroke = g2.getStroke();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // 機体のまわりの光(ためる間に強まり、弾けたあとゆっくり消える)
        double glow = p < BURST_AT ? p / BURST_AT : 1 - (p - BURST_AT) / (1 - BURST_AT);
        double glowR = r * (1.0 + 0.9 * glow);
        fillGlow(g2, cx, cy, glowR, accent, 0.55 * glow * glow);

        // 集まってくる光の粒(ためる間だけ)
        if (p < BURST_AT) {
            double c = p / BURST_AT;
            g2.setColor(new Color(bright.getRed(), bright.getGreen(), bright.getBlue(), (int) (200 * Math.min(1, c * 2))));
            int n = 14;
            for (int i = 0; i < n; i++) {
                double a = i * (2 * Math.PI / n) + phaseTimer * 2.2;
                double dist = r * (2.4 - 1.9 * c) * (0.8 + 0.2 * Math.sin(i * 1.7));
                double d = Math.max(2, 3.5 * adj * (1 - 0.4 * c));
                g2.fill(new java.awt.geom.Ellipse2D.Double(cx + Math.cos(a) * dist - d / 2,
                        cy + Math.sin(a) * dist - d / 2, d, d));
            }
        }

        // 弾ける瞬間の核の光(一瞬だけ、色をにじませたやわらかい光)
        double burst = Math.max(0, 1 - Math.abs(p - BURST_AT) / 0.1);
        if (burst > 0)
            fillGlow(g2, cx, cy, r * 1.5, bright, 0.7 * burst * burst);

        // 衝撃波の輪(2本、少しずらして広がる)
        if (p >= BURST_AT) {
            double q = (p - BURST_AT) / (1 - BURST_AT);
            for (int i = 0; i < 2; i++) {
                double lq = Math.clamp((q - 0.12 * i) / (1 - 0.12 * i), 0, 1);
                if (lq <= 0)
                    continue;
                double ease = 1 - Math.pow(1 - lq, 3);
                double ringR = r * (0.7 + 3.2 * ease);
                float w = (float) Math.max(1, (9 - 5 * i) * adj * (1 - lq));
                g2.setStroke(new BasicStroke(w));
                g2.setColor(new Color(bright.getRed(), bright.getGreen(), bright.getBlue(),
                        (int) (230 * (1 - lq) * (1 - 0.35 * i))));
                g2.draw(new java.awt.geom.Ellipse2D.Double(cx - ringR, cy - ringR, ringR * 2, ringR * 2));
            }
        }

        g2.setComposite(oldComposite);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }

    private static void fillGlow(Graphics2D g2, double cx, double cy, double radius, Color c, double a) {
        if (radius < 1 || a <= 0)
            return;
        int alpha = (int) Math.clamp(a * 255, 0, 255);
        g2.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Double(cx, cy), (float) radius,
                new float[] { 0f, 0.45f, 1f },
                new Color[] { new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 0) }));
        g2.fill(new java.awt.geom.Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    // ---- PNG画像での描画 ----
    private static final String BODY_IMAGE_PATH = "/static/img/ship/boss/%s_f%d.png";
    private static final int BODY_IMAGE_MAX_FORM = 4; // 画像がある形態の最大(それ以降は同じ画像)
    private static final int BODY_FRAMES = 6; // 1枚の画像のコマ数(4x2の格子)
    private static final double BODY_FRAME_TIME = 0.1; // 1コマの時間(秒)
    private static final double BODY_IMAGE_UNIT = 100; // 画像の中の「機体の半分の大きさ」(画素)
    private static final double BODY_IMAGE_CX = 200; // 1コマの中の機体の中心
    private static final double BODY_IMAGE_CY = 185;
    private static final java.util.Map<String, BufferedImage[]> BODY_IMAGES = new java.util.HashMap<>();

    private double bodyClock = 0; // アニメーション用の時計

    /**
     * 種類・形態ごとのアニメーションの各コマ(なければnull)
     */
    private static BufferedImage[] bodyFrames(String bossId, int form) {
        int f = Math.min(Math.max(form, 0), BODY_IMAGE_MAX_FORM);
        String key = bossId + "_f" + f;
        if (!BODY_IMAGES.containsKey(key)) {
            BufferedImage[] frames = null;
            try {
                BufferedImage sheet = UIBuilder.loadImage(String.format(BODY_IMAGE_PATH, bossId, f));
                frames = UIBuilder.cutSpriteSheet(sheet, new Vector2(4, 2), BODY_FRAMES);
            } catch (java.io.IOException e) {
                LogUtils.warn(component, "Boss image not found: " + ANSI.YELLOW + key + ANSI.RESET
                        + ". Using placeholder.");
            }
            BODY_IMAGES.put(key, frames);
        }
        return BODY_IMAGES.get(key);
    }

    /**
     * 機体をPNG画像で描く。形態が変わる間は、前の形態の画像から新しい形態の画像へ切り替える(部品が現れる)。
     * 
     * @return 描けたらtrue(画像がなければfalse)
     */
    private boolean drawBodyImage(Graphics2D g2) {
        BufferedImage[] current = bodyFrames(shipId, currentGauge);
        if (current == null || current.length == 0)
            return false;

        double s = imageSize.getOrDefault("ship", 120) / 2.0;
        double k = s / BODY_IMAGE_UNIT;
        double base = Math.max(0f, Math.min(1f, alpha));
        double grow = formBlend();
        int frame = (int) (bodyClock / BODY_FRAME_TIME);

        Composite oldComposite = g2.getComposite();
        if (grow < 1 && currentGauge >= 1) {
            BufferedImage[] prev = bodyFrames(shipId, currentGauge - 1);
            if (prev != null && prev.length > 0)
                paintBodyImage(g2, prev[frame % prev.length], k, (float) (base * (1 - grow)));
        }
        paintBodyImage(g2, current[frame % current.length], k, (float) (base * (currentGauge >= 1 ? grow : 1)));
        g2.setComposite(oldComposite);
        return true;
    }

    private void paintBodyImage(Graphics2D g2, BufferedImage img, double k, float alpha) {
        if (alpha <= 0)
            return;
        BufferedImage scaled = UIBuilder.cachedResize(img, img.getWidth() * k);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
        g2.drawImage(scaled, (int) Math.round(pos.getX() - BODY_IMAGE_CX * k),
                (int) Math.round(pos.getY() - BODY_IMAGE_CY * k), null);
    }

    /**
     * ボスの仮描画
     * 暗い機体に、縁取りと中心の光を付ける。色は変わらず、ゲージを壊すたびに「形」が変わる:
     * 部品が増えていく(2ゲージ目: 肩の双砲 / 3: 外側の翼と中央の主砲 / 4: 背中のトゲ / 5以降: 砲台ポッド)。
     * 形態が変わる間(無敵の点滅中)は、新しい部品がせり出してくる。
     */
    private void drawPlaceholder(Graphics2D g2) {
        // 今の形態に入った直後は、新しい部品が伸びてくる(0→1)
        double grow = formBlend();
        drawBody(g2, pos.getX(), pos.getY(), imageSize.getOrDefault("ship", 120) / 2.0, BossLook.of(shipId),
                currentGauge, grow, phaseTimer, alpha);
    }

    /**
     * ボスの機体を描く(種類ごとの形と、形態ごとに増える部品)
     * 
     * @param s     機体の半分の大きさ
     * @param look  ボスの種類ごとの見た目
     * @param form  形態(0始まり。壊したゲージの数)
     * @param grow  今の形態で増えた部品のせり出し具合(0〜1)
     * @param t     脈打ちの時間
     * @param alpha 不透明度
     */
    static void drawBody(Graphics2D g2, double cx, double cy, double s, BossLook look, int form, double grow,
            double t, float alpha) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        Color accent = look.accent(); // 本体の色は種類ごとに決まり、形態が変わっても変わらない
        Color plate = new Color(38, 40, 56);
        Color plateDark = new Color(26, 28, 40);

        AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        UIBuilder.applyHighQualityRenderSettings(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        g2.setStroke(new BasicStroke((float) (3 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 機体の後ろに付く部品 → 機体 → 機体の前に付く部品
        drawParts(g2, cx, cy, s, look, form, grow, false, plateDark, accent);

        double[][] hull = look.hull();
        int[] xs = new int[hull.length];
        int[] ys = new int[hull.length];
        for (int i = 0; i < hull.length; i++) {
            xs[i] = (int) (cx + hull[i][0] * s);
            ys[i] = (int) (cy + hull[i][1] * s);
        }
        g2.setColor(plate);
        g2.fillPolygon(xs, ys, hull.length);
        g2.setColor(accent);
        g2.drawPolygon(xs, ys, hull.length);

        // 内側の模様(機体を一回り小さくした輪郭)
        int[] ixs = new int[hull.length];
        int[] iys = new int[hull.length];
        for (int i = 0; i < hull.length; i++) {
            ixs[i] = (int) (cx + hull[i][0] * s * 0.7);
            iys[i] = (int) (cy + hull[i][1] * s * 0.7);
        }
        g2.setStroke(new BasicStroke((float) (1.5 * adj)));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
        g2.drawPolygon(ixs, iys, hull.length);
        g2.setStroke(new BasicStroke((float) (3 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        drawParts(g2, cx, cy, s, look, form, grow, true, plateDark, accent);

        // 中心の光(ゆっくり脈打つ)
        double pulse = 0.85 + 0.15 * Math.sin(t * 6);
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
     * 今の形態までに増えた部品を描く(今の形態で増えた部品は、せり出し具合 grow で描く)
     */
    private static void drawParts(Graphics2D g2, double cx, double cy, double s, BossLook look, int form,
            double grow, boolean front, Color fill, Color edge) {
        for (BossLook.Part p : look.parts()) {
            if (p.front() != front || p.minForm() > form)
                continue;
            double g = p.minForm() == form ? grow : 1;
            part(g2, cx, cy, s, p.pts(), p.ax(), p.ay(), g, fill, edge);
        }
    }

    /**
     * 部品(多角形)を描く
     * 
     * @param pts  頂点(ボス中心からの位置。機体の半分の大きさ s を1とする)
     * @param ax   根もとのx(同じ単位)。せり出すとき、ここを動かさずに伸びる
     * @param ay   根もとのy
     * @param grow せり出し具合(0〜1)
     */
    private static void part(Graphics2D g2, double cx, double cy, double s, double[][] pts, double ax, double ay,
            double grow, Color fill, Color edge) {
        int[] px = new int[pts.length];
        int[] py = new int[pts.length];
        for (int i = 0; i < pts.length; i++) {
            px[i] = (int) (cx + (ax + (pts[i][0] - ax) * grow) * s);
            py[i] = (int) (cy + (ay + (pts[i][1] - ay) * grow) * s);
        }
        g2.setColor(fill);
        g2.fillPolygon(px, py, pts.length);
        g2.setColor(edge);
        g2.drawPolygon(px, py, pts.length);
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
