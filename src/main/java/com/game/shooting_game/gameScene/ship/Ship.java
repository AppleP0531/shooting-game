package com.game.shooting_game.gameScene.ship;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletConfig;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipConfig;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShotConfig;
import com.game.shooting_game.interfaces.Entity;
import com.game.shooting_game.interfaces.GameObject;
import com.game.shooting_game.interfaces.Moveable;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;

public abstract class Ship implements Moveable, GameObject, Entity {
    private static final String component = "[Ship]";

    protected final ShootingGameMain MAIN;
    protected final AnimationManager AM;
    protected final SceneManager SM;
    protected final GameManager GM;
    protected final ShipManager SHIP_M;
    protected final BulletManager BM;

    @Getter
    protected ShipKinds owner; // 弾の発射者
    @Getter
    protected ColorMode mode; // 色のモード
    @Setter
    protected String shipId; // 船ID
    @Setter
    @Getter
    protected int shipLevel; // レベル
    @Getter
    protected String shipState; // 状態
    @Setter
    @Getter
    protected Vector2 pos; // 位置
    protected double vec; // 方向
    protected double rotate; // 回転量(-PI~PI)
    protected double speed; // 速度
    @Getter
    protected double collisionSize; // 当たり判定の大きさ
    @Setter
    @Getter
    protected double hp = 100; // Hp
    @Getter
    protected double maxHp = 100; // 最大Hp
    protected float alpha = 1.0f; // 透明度
    @Getter
    protected double score = 0; // 倒したときに得られるスコア量
    protected String coins = "0~0"; // 倒したときに得られるコイン量
    @Getter
    protected boolean allowDropItem = true; // アイテムをドロップするかどうか
    @Setter
    @Getter
    protected double shieldTimer; // 無敵タイマー
    protected Map<BulletKinds, Map<Integer, Double>> coolDownTimers; // クールダウンタイマー
    protected Map<String, String> animationId = new HashMap<>(); // アニメーションId
    protected Map<String, Integer> imageSize = new HashMap<>(); // 画像の大きさ
    protected Map<String, int[]> useCount = new HashMap<>(); // 使用枚数
    protected int frameAdj = 0; // 使用画像の調整用
    @Getter
    protected String animationName = "none"; // アニメーション名
    protected double animateTimer = 0; // アニメーション用タイマー

    private List<ShotConfig> shotData = new ArrayList<>(); // 発射する弾のデータ
    private Map<BulletKinds, List<Integer>> shotBullet = new HashMap<>(); // 発射した弾のデータ

    public Ship(ShipKinds owner, String id, ColorMode mode, int level, Vector2 pos) {
        MAIN = ShootingGameMain.getInstance();
        AM = AnimationManager.getInstance();
        SM = SceneManager.getInstance();
        GM = GameManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        BM = BulletManager.getInstance();
        this.owner = owner;

        this.shipId = id;
        this.mode = mode;
        this.shipLevel = level;
        this.shipState = "Normal";
        this.pos = pos;
        coolDownTimers = initializeMap();
        ShipDefinition def = SHIP_M.getShipConfig().get(owner).get(shipId);
        if (def != null && def.getDatas() != null) { // 情報があれば上書き
            ShipConfig datas = def.getDatas();

            maxHp = hp = 100;
            collisionSize = datas.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();
            imageSize.put("ship", (int) (datas.getImageSize() * ShootingGameMain.getScreenSizeAdj()));
            speed = datas.getSpeed() * ShootingGameMain.getScreenSizeAdj();
            score = datas.getScore();
            coins = datas.getCoins();

            // 画像の取得とアニメーション登録
            BufferedImage[] shipImage;
            if (owner == ShipKinds.PLAYER)
                shipImage = Optional.ofNullable(SHIP_M.getShipImage())
                        .map(images -> images.get(shipId))
                        .orElse(null);
            else
                shipImage = Optional.ofNullable(SHIP_M.getShipImage())
                        .map(images -> images.get(shipId + "_" + mode.toString().toLowerCase()))
                        .orElse(null);

            if (shipImage != null) {
                double frameRate = datas.getFrameRate();

                // 各状態の画像使用枚数の取得
                int sheetCount = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets().length;
                useCount.put("ship", new int[sheetCount]);
                for (int i = 0; i < sheetCount; i++) {
                    useCount.get("ship")[i] = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets()[i]
                            .getUseCount();
                }

                animationId.put("ship", AM.addAnimation(new Animation(shipImage, imageSize.get("ship"), frameRate)));
            }
        }
    }

    @Override
    public void update(double dt) {
        // ゲームシーンかテストシーンなら移動処理をする
        if (SM.getSceneName() == SceneType.GAME || SM.getSceneName() == SceneType.TEST)
            move(dt);
        updateCooldownTimer(dt);

        // 無敵時間処理(タイマーが0になるまで減らす)
        shieldTimer = Math.max(0, shieldTimer - dt);

        // アニメーション更新処理
        if (animationId.get("ship") != null) {
            Animation shipAnim = AM.getAnimationCache().get(animationId.get("ship"));

            if (owner == ShipKinds.PLAYER)
                shipAnim.setCustomLogic((current, anim) -> {
                    if (anim.getFrameTimer() > anim.getFrameRate()) {
                        anim.setFrameTimer(0);

                        // 通常状態の使用画像フレーム更新
                        if (frameAdj == 0)
                            current = (current + 1) % useCount.get("ship")[0] + frameAdj;

                        // 状態遷移時の使用画像フレーム更新
                        if (shipState.equals("OOC") || useCount.get("ship")[1] < frameAdj) {
                            if (current == 0 || 0 < frameAdj) {
                                frameAdj++;
                                if (Arrays.stream(useCount.get("ship")).sum() < useCount.get("ship")[0] + frameAdj) {
                                    frameAdj = useCount.get("ship")[1] + 1;
                                }
                                current = useCount.get("ship")[0] - 1 + frameAdj;
                            }
                        }

                        // OOC状態→通常状態
                        if (shipState.equals("Normal") && 0 < frameAdj && frameAdj <= useCount.get("ship")[1] + 1) {
                            frameAdj = Math.max(0, frameAdj - 1);
                            current = useCount.get("ship")[0] - 1 + frameAdj;
                        }
                    }

                    return current;
                });
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // ゲームシーンかテストシーンならHPを描画
        if (SM.getSceneName() == SceneType.GAME || SM.getSceneName() == SceneType.TEST)
            drawHpBar(g);

        AffineTransform oldTransform = g2.getTransform();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Vector2 drawPos = new Vector2(pos.getX(), pos.getY());
        // 画像が存在するときは読み込む
        if (animationId.get("ship") != null) {
            BufferedImage image = AM.getCurrentImage(animationId.get("ship"));

            // 回転処理
            g2.translate((int) drawPos.getX(), (int) drawPos.getY());
            g2.rotate(rotate);
            // 描画処理
            UIBuilder.drawImage(g2, Anchor.CENTER, new Vector2(0, 0), image);
        }
        g2.setTransform(oldTransform);
        // 透明度を1に戻す
        g2.setComposite(AlphaComposite.SrcOver);
    }

    @Override
    public void move(double dt) {
        pos.setX(pos.getX() + Math.cos(vec) * speed * dt);
        pos.setY(pos.getY() + Math.sin(vec) * speed * dt);
    }

    /**
     * HPバーの描画
     * 
     * @param g
     */
    public abstract void drawHpBar(Graphics g);

    /**
     * 画面内にいるか
     * 
     * @return いればtrue、いなければfalse
     */
    public boolean inScreen() {
        return 0 < pos.getX()
                && pos.getX() < MAIN.getWidth()
                && 0 < pos.getY()
                && pos.getY() < MAIN.getHeight();
    }

    /**
     * クールダウンタイマーをすべて減らす
     * 
     * @param dt
     */
    private void updateCooldownTimer(double dt) {
        coolDownTimers.forEach((kind, idMap) -> {
            // 0より大きければdtだけ減らす、0より小さければ0にする
            idMap.replaceAll((id, timer) -> {
                if (timer > 0.0)
                    return Math.max(0.0, timer - dt);

                return 0.0;
            });
        });
    }

    /**
     * 弾の追加(発射)
     * 
     * @param level 船レベル
     * @param id    船タイプ
     * @param state 船状態
     */
    protected void addNewBullet(int level, String id, String state) {
        // 打つ弾のデータをすべて保存
        shotData.clear();
        for (int i = 0; i <= level; i++) {
            String levelKey = "Lv" + i;

            ShipManager.ShotConfig[] shotsForLevel = SHIP_M.getShipConfig().get(owner).get(id)
                    .getLevelData(levelKey);
            // 取得したデータがnullではなければ結合
            if (shotsForLevel != null)
                shotData.addAll(Arrays.asList(shotsForLevel));
        }

        shotBullet.clear();
        for (ShipManager.ShotConfig config : shotData) {
            BulletKinds kind = config.getBulletKind();
            int bulletId = config.getBulletId();

            if (coolDownTimers.get(kind).get(bulletId) <= 0.0f) { // クールダウンタイマーが0以下の時
                shotBullet.computeIfAbsent(kind, k -> new ArrayList<>()).add(bulletId); // 発射した弾の種類を登録
                Vector2 bulletPos = new Vector2(
                        pos.getX() + config.getPos().getX(),
                        pos.getY() + config.getPos().getY()); // 弾の生成座標

                // 弾の方向の計算
                double vecRef = 0;
                if (config.getBulletVecRef() != null) {
                    // 弾が飛ぶ方向の基準vecの設定
                    switch (config.getBulletVecRef()) {
                        // 上方向
                        case "up" -> vecRef = -90;
                        // 下方向
                        case "down" -> vecRef = 90;
                        // プレイヤーがいる方向
                        case "target" -> {
                            vecRef = GameUtils.targetVec(this.getPos(), SHIP_M.getPlayer().getPos());
                            vecRef = Math.toDegrees(vecRef);
                        }
                        default -> {
                            // 不明な値ログ
                            LogUtils.systemLog(
                                    "[WARN]",
                                    component,
                                    "Unexpected value assigned to " + ANSI.YELLOW + "bulletVecRef" + ANSI.RESET);
                            return;
                        }
                    }

                    // 弾の新規作成
                    BM.addBullet(BulletFactory.createBullet(
                            owner,
                            config.getBulletKind(),
                            config.getBulletId(),
                            mode,
                            bulletPos,
                            Math.toRadians(vecRef + config.getVecAdj()),
                            config.getDirection()));
                } else
                    // 基準vecの未定義ログ
                    LogUtils.systemLog(
                            "[WARN]",
                            component,
                            "Missing refercence: " + ANSI.YELLOW + id + " - Lv" + shipLevel + " - bulletVecRef"
                                    + ANSI.RESET);

            }
        }

        // 発射した弾種のクールタイムリセット
        shotBullet.forEach((kind, idList) -> {
            for (Integer bulletId : idList) {
                resetTimer(kind, bulletId, shipState);
            }
        });
    }

    /**
     * タイマーのリセット
     * 
     * @param kinds 弾の種類
     * @param state 船状態
     */
    private void resetTimer(BulletKinds kinds, int bulletId, String state) {
        Map<BulletKinds, Map<Integer, BulletConfig>> config = BM.getBulletConfig().get(owner);
        Double cooldownValue = config.get(kinds).get(bulletId).getCooldowns().get(state);

        if (cooldownValue != null)
            coolDownTimers.get(kinds).put(bulletId, cooldownValue);
    }

    /**
     * 同型マップを生成
     * 
     * 最初の弾だけ少し遅延
     * プレイヤーの場合は0.1秒遅延
     * 敵の場合初弾は3秒遅延
     * 
     * @return クールタイム処理用の同型マップ
     */
    private Map<BulletKinds, Map<Integer, Double>> initializeMap() {
        Map<BulletKinds, Map<Integer, Double>> initializedMap = new HashMap<>();

        Map<BulletKinds, Map<Integer, BulletConfig>> config = BM.getBulletConfig().get(owner);
        if (config != null) {
            config.forEach((kind, bulletIdMap) -> {
                Map<Integer, Double> cooldownIdMap = new HashMap<>();

                bulletIdMap.keySet().forEach(id -> {
                    // 最初に弾を発射するまでの時間（プレイヤー:0秒、敵:3秒）
                    if (owner == ShipKinds.PLAYER)
                        cooldownIdMap.put(id, 0.0);
                    else
                        cooldownIdMap.put(id, 3.0);
                });
                initializedMap.put(kind, cooldownIdMap);
            });
        }

        return initializedMap;
    }

    /**
     * アニメーション処理
     * 
     * @param name アニメーション名
     */
    public void animation(String name) {
        if (animationName != name) {
            animateTimer = 0;
            animationName = name;
        }
    }

    /**
     * モード切替
     */
    public void changeMode() {
        if (mode == ColorMode.BLUE) {
            mode = ColorMode.RED;
        } else {
            mode = ColorMode.BLUE;
        }
    }

    /**
     * スコアの加算
     */
    public void addScore() {
        GM.addScore(score);
    }

    /**
     * コインのドロップ
     * 
     * @return ドロップ量
     */
    public int getDropCoinAmount() {
        String range = coins;

        try {
            String[] num = range.split("~");
            int min = Integer.parseInt(num[0]);
            int max = Integer.parseInt(num[1]);

            return ThreadLocalRandom.current().nextInt(min, max + 1);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 回復
     * 
     * @param amount 回復量
     */
    public void heal(double amount) {
        hp += amount;
        hp = Math.min(hp, maxHp);
    }

    /**
     * ダメージの追加
     * 
     * @param damage ダメージ量
     */
    public void addDamage(double damage) {
        // シールドを持っていなければダメージを受ける
        if (shieldTimer <= 0)
            hp -= damage;
    }

    /**
     * 殺す
     */
    public void kill(boolean allowDropItem) {
        this.allowDropItem = allowDropItem;
        setHp(0);
    }

    /**
     * 除去
     */
    public void clear() {
        switch (owner) {
            case ShipKinds.ENEMY -> SHIP_M.getEnemyList().remove(this);
            case ShipKinds.BOSS -> SHIP_M.getBossList().remove(this);
            default -> {
            }
        }
    }

    /**
     * 死んでいるかどうか
     * 
     * @return 死んでいる場合true,生きている場合false
     */
    public boolean isDead() {
        return hp <= 0;
    }
}