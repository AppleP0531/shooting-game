package com.game.shooting_game.gameScene.ship.types;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamEmitter;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;

public class Player extends Ship implements KeyListener {
    private static final String component = "[Ship:Player]";

    // 操作
    private MouseAdapter mouseHandler; // マウス操作
    private Vector2 dragStartPos; // ドラッグ開始マウス位置
    private Vector2 dragStartPlayerPos; // ドラッグ開始時のプレイヤー位置
    private Vector2 dragMousePos; // ドラッグ中のマウス位置
    @Getter
    private boolean up = false, down = false, left = false, right = false; // キー入力
    private String lastHorizontal = ""; // 最後に押された左右
    private String lastVertical = ""; // 最後に押された上下
    private List<SubShip> subShip = new ArrayList<>();
    private double OOCTime = 10; // 暴走状態持続時間
    @Setter
    @Getter
    private double OOCTimer; // 暴走状態タイマー
    private static final double BASE_SHIELD_RATE = 0.15; // 最大HPに対する基本のシールド量
    private static final double SHIELD_REGEN_DELAY = 3.0; // 被弾してから回復が始まるまで(秒)
    private static final double SHIELD_REGEN_RATE = 0.08; // 1秒あたりに回復するシールド量(最大値に対する割合)
    private double shieldPoints; // 今のシールド量
    private double maxShield; // シールドの最大量
    private double shieldRegenWait; // 回復が始まるまでの待ち時間
    @Getter
    private double damagedTimer; // 被弾時の無敵タイマー
    private double breakawaySpeed; // 離脱速度
    private final List<BeamEmitter> beamEmitters = new ArrayList<>(); // 本体のビーム
    private BeamEmitter subBeamEmitter; // サブ機のビーム(左右で同じタイミングで撃つので、1つで管理する)
    private int subBeamTier = 0; // サブ機のビームを作ったときのエミッターのMk

    public Player(String id, ColorMode mode, Vector2 pos) {
        super(ShipKinds.PLAYER, id, mode, 0, pos);

        // 画像の読み込みとアニメーション登録
        BufferedImage[] shieldImages_blue = SHIP_M.getShipImage().get("shield_blue");
        if (shieldImages_blue != null) {
            ShipDefinition def = SHIP_M.getShipConfig().get(ShipKinds.OBJECT).get("shield");
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("shield_blue", AM.addAnimation(new Animation(
                    shieldImages_blue,
                    def.getDatas().getImageSize() * ShootingGameMain.getScreenSizeAdj(),
                    frameRate)));
        }
        BufferedImage[] shieldImages_red = SHIP_M.getShipImage().get("shield_red");
        if (shieldImages_red != null) {
            ShipDefinition def = SHIP_M.getShipConfig().get(ShipKinds.OBJECT).get("shield");
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("shield_red", AM.addAnimation(new Animation(
                    shieldImages_red,
                    def.getDatas().getImageSize() * ShootingGameMain.getScreenSizeAdj(),
                    frameRate)));
        }
        BufferedImage[] explosionImages = SHIP_M.getShipImage().get("explosion");
        if (explosionImages != null) {
            ShipDefinition def = SHIP_M.getShipConfig().get(ShipKinds.OBJECT).get("explosion");
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("explosion", AM.addAnimation(new Animation(
                    explosionImages,
                    def.getDatas().getImageSize() * ShootingGameMain.getScreenSizeAdj(),
                    frameRate)));
        }

        ShipDefinition def = SHIP_M.getShipConfig().get(owner).get(shipId);
        if (def != null && def.getDatas() != null) {
            // 画像が存在すれば読み込む
            for (String object : animationId.keySet()) {
                if (animationId.get(object) != null) {
                    int sheetCount = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets().length;
                    useCount.put(object, new int[sheetCount]);
                    for (int i = 0; i < sheetCount; i++) {
                        useCount.get(
                                object)[i] = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets()[i]
                                        .getUseCount();
                    }

                    imageSize.put(object, (int) (def.getDatas().getImageSize() * ShootingGameMain.getScreenSizeAdj()));
                } else {
                    switch (object) {
                        case "ship" -> {
                            imageSize.put("ship", (int) (30 * ShootingGameMain.getScreenSizeAdj()));
                        }
                    }
                }
            }
        } else {
            // データがなかった時用のデフォルト値
            collisionSize = 80 * ShootingGameMain.getScreenSizeAdj();
            speed = 250 * ShootingGameMain.getScreenSizeAdj();
        }

        rotate = 0;

        // 本体のビーム(機体ごとの設定)
        if (def != null && def.getBeams() != null)
            for (BeamSpec spec : def.getBeams())
                beamEmitters.add(new BeamEmitter(spec));

        // スキルツリーによる強化の反映
        maxHp *= 1 + SkillService.getBonus("MAX_HP");
        maxHp *= UnitService.hpRate(shipId); // 機体レベル・★による耐久力の底上げ
        maxHp = maxHp * (1 + ModuleService.stat(ModuleStat.HP_PCT)) + ModuleService.stat(ModuleStat.HP_FLAT); // モジュール
        maxHp *= StageModifiers.playerHpRate(); // ステージ条件
        hp = maxHp;
        // シールド(HPの前にダメージを受け、被弾から少し経つと回復する。モジュールで増やせる)
        maxShield = maxHp * BASE_SHIELD_RATE * (1 + ModuleService.stat(ModuleStat.SHIELD_PCT))
                + ModuleService.stat(ModuleStat.SHIELD_FLAT);
        if (StageModifiers.noShield())
            maxShield = 0;
        shieldPoints = maxShield;
        speed *= 1 + SkillService.getBonus("MOVE_SPEED");
        collisionSize *= 1 - Math.min(0.15, SkillService.getBonus("HITBOX"));
        OOCTime = 10 + SkillService.getBonus("OOC_TIME");

        mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPos = new Vector2(e.getX(), e.getY());
                dragStartPlayerPos = new Vector2(pos.getX(), pos.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPos = null;
                dragMousePos = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragMousePos = new Vector2(e.getX(), e.getY());
            }
        };
        mode = ColorMode.BLUE;

        // 船タイプログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Current shipId set to " + ANSI.YELLOW + shipId + "." + ANSI.RESET);

        // サブ機の生成(必須パーツが揃っていて、装備中のときだけ両端に付く)
        double shipDist = SubShipService.sideOffset() * ShootingGameMain.getScreenSizeAdj(); // 大きいフレームほど外側
        double yDiff = 14 * ShootingGameMain.getScreenSizeAdj(); // 自機より少し後ろに付く
        if (SubShipService.isEquipped()) {
            subShip.add(
                    new SubShip(this, "SubShip1_L", mode, new Vector2(pos.getX() - shipDist, pos.getY() + yDiff)));
            subShip.add(
                    new SubShip(this, "SubShip1_R", mode, new Vector2(pos.getX() + shipDist, pos.getY() + yDiff)));
        }

        MAIN.addKeyListener(this);
        addHandler();
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        switch (animationName) {
            case "none" -> {
                // 弾
                addNewBullet(shipLevel, shipId, shipState);

                // 船がレベル5の時は暴走モードにする
                if (shipLevel >= 5) {
                    OOCTimer = Math.max(0, OOCTimer - dt);
                    shipState = "OOC";

                    if (OOCTimer <= 0)
                        shipLevel = 4;
                } else
                    shipState = "Normal";

                // 被弾無敵時間処理(タイマーが0になるまで減らす)
                damagedTimer = Math.max(0, damagedTimer - dt);

                // シールドの回復(被弾から少し経ったら、モジュールの回復速度ぶん速く回復する)
                shieldRegenWait = Math.max(0, shieldRegenWait - dt);
                if (shieldRegenWait <= 0 && shieldPoints < maxShield)
                    shieldPoints = Math.min(maxShield, shieldPoints
                            + maxShield * SHIELD_REGEN_RATE * (1 + ModuleService.stat(ModuleStat.REPAIR_PCT)) * dt);

                // 本体のビーム(必要な機体レベルに達していれば自動で撃つ。暴走中は間隔が短くなる)
                double beamIntervalScale = (shipLevel >= 5 ? 0.6 : 1.0) / (1 + SkillService.getBonus("BEAM_RATE"));
                boolean inLobby = SM.getSceneName() == SceneType.LOBBY;
                // ロビーのプレビューでは、短い間隔で繰り返し見せる
                if (inLobby)
                    beamIntervalScale *= 0.4;
                boolean inBattle = SM.getSceneName() == SceneType.GAME || SM.getSceneName() == SceneType.TEST
                        || inLobby; // ロビーのプレビューでも撃つ
                // 狙える敵が真上にいるときだけ撃つ(撃ちっぱなしで画面がうるさくならないように)
                boolean hasTarget = BeamManager.getInstance().hasTargetAhead(pos,
                        60 * ShootingGameMain.getScreenSizeAdj());
                for (BeamEmitter emitter : beamEmitters) {
                    if (!inBattle || shipLevel < emitter.getSpec().getMinLevel())
                        continue;
                    if (emitter.update(dt, beamIntervalScale, hasTarget))
                        BeamManager.getInstance().firePlayer(
                                emitter.getSpec(),
                                () -> new Vector2(pos.getX(), pos.getY() - 10 * ShootingGameMain.getScreenSizeAdj()),
                                () -> mode,
                                1 + SkillService.getBonus("BEAM_POWER"));
                }
            }
        }

        subShip.forEach(s -> s.update(dt));
        if ("none".equals(animationName))
            updateSubBeams(dt);
    }

    /**
     * サブ機のビーム(左右同時。どちらかの真上に敵がいれば撃つ。エミッターのMkが変わったら作り直す)
     */
    private void updateSubBeams(double dt) {
        if (subShip.isEmpty() || !subShip.get(0).canBeam())
            return;

        int tier = SubShipService.getTier("EMITTER");
        if (subBeamEmitter == null || subBeamTier != tier) {
            subBeamEmitter = new BeamEmitter(SubShip.createBeamSpec());
            subBeamTier = tier;
        }

        boolean hasTarget = subShip.stream().anyMatch(SubShip::hasTargetAhead);
        if (subBeamEmitter.update(dt, subShip.get(0).beamIntervalScale(), hasTarget))
            subShip.forEach(s -> s.fireBeam(subBeamEmitter.getSpec()));
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        alpha = (damagedTimer > 0) ? GameUtils.calcFlashAlpha(damagedTimer, 4, 0.8) : 1;
        super.draw(g);

        // 画像が存在しなければ仮描画
        // 船
        if (animationId.get("ship") == null) {
            g2.rotate(rotate, pos.getX(), pos.getY());
            g2.setColor(new Color(0, 1, 0, alpha));
            g.fillPolygon(
                    new int[] {
                            (int) pos.getX(),
                            (int) pos.getX() - imageSize.get("ship") / 2,
                            (int) pos.getX() + imageSize.get("ship") / 2
                    },
                    new int[] {
                            (int) pos.getY() - imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2
                    },
                    3);
            // 回転リセット
            g2.rotate(-rotate, pos.getX(), pos.getY());
        }

        // ロビー以外では描画
        if (SM.getSceneName() != SceneType.LOBBY) {
            // シールド画像の描画(存在するとき)
            if (animationId.get("shield_blue") != null && mode == ColorMode.BLUE) {
                BufferedImage image = AM.getCurrentImage(animationId.get("shield_blue"));

                UIBuilder.drawImage(g2, Anchor.CENTER, pos, image);
            }
            if (animationId.get("shield_red") != null && mode == ColorMode.RED) {
                BufferedImage image = AM.getCurrentImage(animationId.get("shield_red"));

                UIBuilder.drawImage(g2, Anchor.CENTER, pos, image);
            }
        }

        switch (animationName) {
            case "death" -> {
                GM.result();
            }
        }

        subShip.forEach(s -> s.draw(g));
    }

    @Override
    public void move(double dt) {
        switch (animationName) {
            case "admission" -> {
                animateTimer += dt;

                double flySpeed = 100 * ShootingGameMain.getScreenSizeAdj();
                if (pos.getY() >= MAIN.getHeight() * 6 / 7)
                    pos.setY(pos.getY() - flySpeed * dt);
            }
            case "none" -> {
                // キー移動処理
                Vector2 direction = new Vector2(0, 0);
                // 上下
                int y = (up && down) ? (lastVertical.equals("up") ? -1 : 1) : (up ? -1 : (down ? 1 : 0));
                direction.setY(y);
                // 左右
                int x = (left && right) ? (lastHorizontal.equals("left") ? -1 : 1) : (left ? -1 : (right ? 1 : 0));
                direction.setX(x);
                // キーが何かしら押されてるときに移動
                if (up || down || left || right) {
                    vec = GameUtils.targetVec(new Vector2(0, 0), direction);
                    super.move(dt);
                }

                // マウス移動処理
                if (dragStartPos != null && dragMousePos != null) {
                    Vector2 targetPos = new Vector2(
                            dragStartPlayerPos.getX() + dragMousePos.getX() - dragStartPos.getX(),
                            dragStartPlayerPos.getY() + dragMousePos.getY() - dragStartPos.getY());
                    double distSq = GameUtils.distSq(pos, targetPos);
                    // 目標地点との距離が近いときは止まる（ガタつき防止）
                    if (distSq > Math.pow(5 * ShootingGameMain.getScreenSizeAdj(), 2)) {
                        vec = GameUtils.targetVec(pos, targetPos);
                        super.move(dt);
                    }

                }

                // 画面内に収まるように移動
                pos.setX(Math.clamp(pos.getX(), 0, MAIN.getWidth()));
                pos.setY(Math.clamp(pos.getY(), 0, MAIN.getHeight()));
            }
            case "breakAway" -> {
                animateTimer += dt;
                // 1秒経過後に上に飛び始める
                if (animateTimer > 1) {
                    double acceleration = 500 * ShootingGameMain.getScreenSizeAdj();
                    breakawaySpeed += acceleration * dt;
                    pos.setY(pos.getY() - breakawaySpeed * dt);

                    if (pos.getY() < -50)
                        GM.result();
                }
            }
        }

        subShip.forEach(s -> s.move(dt));
    }

    public double getShieldPoints() {
        return shieldPoints;
    }

    public double getMaxShield() {
        return maxShield;
    }

    @Override
    public void drawHpBar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Vector2 barSize = new Vector2(
                MAIN.getWidth() - 60 * ShootingGameMain.getScreenSizeAdj(),
                15 * ShootingGameMain.getScreenSizeAdj());
        int skewX = (int) (10 * ShootingGameMain.getScreenSizeAdj()); // 上辺のずらす量
        Vector2 spacing = new Vector2(
                5 * ShootingGameMain.getScreenSizeAdj(),
                25 * ShootingGameMain.getScreenSizeAdj()); // 画面端からのスペース
        Vector2 barPos = new Vector2(
                MAIN.getWidth() - barSize.getX() - skewX - spacing.getX(),
                spacing.getY());

        // HPバーの枠
        g2.setColor(new Color(50, 50, 50, 150));
        g.fillPolygon(
                new int[] {
                        (int) barPos.getX() + skewX,
                        (int) barPos.getX() + (int) barSize.getX() + skewX,
                        (int) barPos.getX() + (int) barSize.getX(),
                        (int) barPos.getX()
                },
                new int[] {
                        (int) barPos.getY(),
                        (int) barPos.getY(),
                        (int) barPos.getY() + (int) barSize.getY(),
                        (int) barPos.getY() + (int) barSize.getY()
                },
                4);

        // 最大HPに対する今のHPの割合を求めてその分を塗りつぶす
        double hpRathio = hp / maxHp;
        int currentHpWidth = (int) (barSize.getX() * hpRathio);
        g2.setColor(Color.GREEN);
        g.fillPolygon(
                new int[] {
                        (int) barPos.getX() + skewX,
                        (int) barPos.getX() + currentHpWidth + skewX,
                        (int) barPos.getX() + currentHpWidth,
                        (int) barPos.getX()
                },
                new int[] {
                        (int) barPos.getY(),
                        (int) barPos.getY(),
                        (int) barPos.getY() + (int) barSize.getY(),
                        (int) barPos.getY() + (int) barSize.getY()
                },
                4);

        // シールドのバー(HPバーの下に細く)
        if (maxShield > 0) {
            int sh = Math.max(3, (int) (4 * ShootingGameMain.getScreenSizeAdj()));
            int sy = (int) barPos.getY() + (int) barSize.getY() + 2;
            g2.setColor(new Color(50, 50, 50, 150));
            g2.fillRect((int) barPos.getX() + skewX / 2, sy, (int) barSize.getX(), sh);
            g2.setColor(new Color(90, 200, 255));
            g2.fillRect((int) barPos.getX() + skewX / 2, sy, (int) (barSize.getX() * (shieldPoints / maxShield)), sh);
        }
    }

    /*
     * キー関連
     * 論理値変数を押したときにtrue,離れた時にfalse
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                up = true;
                lastVertical = "up";
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                down = true;
                lastVertical = "down";
            }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                left = true;
                lastHorizontal = "left";
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                right = true;
                lastHorizontal = "right";
            }
            case KeyEvent.VK_SPACE -> {
                changeMode();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                up = false;
                if (down)
                    lastVertical = "down";
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                down = false;
                if (up)
                    lastVertical = "up";
            }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                left = false;
                if (right)
                    lastHorizontal = "right";
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                right = false;
                if (left)
                    lastHorizontal = "left";
            }
        }
    }

    @Override
    public void addDamage(double damage) {
        // シールドを持っていなかった場合はダメージを負って、1秒のダメージクールタイムを設ける
        if (shieldTimer <= 0) {
            // シールドが先にダメージを受け、残りがHPに入る
            double rest = damage;
            if (shieldPoints > 0) {
                double absorbed = Math.min(shieldPoints, rest);
                shieldPoints -= absorbed;
                rest -= absorbed;
            }
            shieldRegenWait = SHIELD_REGEN_DELAY;
            if (rest > 0)
                super.addDamage(rest);
            shieldTimer = damagedTimer = 1 + SkillService.getBonus("INVINCIBLE");
        }
    }

    /**
     * レベルアップ
     */
    public void levelUp() {
        // 常時暴走状態の場合処理をスキップ
        if (OOCTimer == Double.POSITIVE_INFINITY)
            return;

        if (shipLevel < 5)
            shipLevel++;

        if (shipLevel == 5)
            setOOCTimer(OOCTime);
    }

    /**
     * シールド時間の設定
     * 
     * @param time 時間
     */
    public void setShieldTimer(double time) {
        // 与えられた数字が0でないかつ、常時シールド状態の場合処理をスキップ
        if (shieldTimer == Double.POSITIVE_INFINITY && time != 0)
            return;

        shieldTimer = time;
    }

    /**
     * ハンドラの追加
     */
    public void addHandler() {
        MAIN.addMouseListener(mouseHandler);
        MAIN.addMouseMotionListener(mouseHandler);
    }

    /**
     * ハンドラの削除
     */
    public void removeHandler() {
        MAIN.removeMouseListener(mouseHandler);
        MAIN.removeMouseMotionListener(mouseHandler);
    }
}