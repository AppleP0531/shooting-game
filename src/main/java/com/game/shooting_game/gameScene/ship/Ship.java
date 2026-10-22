package com.game.shooting_game.gameScene.ship;

import java.awt.AlphaComposite;
import java.awt.Color;
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
import com.game.shooting_game.gameScene.damage.DamagePopupManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletFactory;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletConfig;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipConfig;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.ShipManager.ShotConfig;
import com.game.shooting_game.interfaces.Entity;
import com.game.shooting_game.interfaces.GameObject;
import com.game.shooting_game.interfaces.Moveable;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
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
    @Getter
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
    @Getter
    private EnemyAffix affix; // ナイトメアで付くエフェクト(なければnull)
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
    @Getter
    protected boolean removeRequested = false; // 除去要求(更新中にリストを触らないためのフラグ)

    private static final int HIT_ALPHA = 64; // 当たり判定に使う画像の不透明度(0~255)

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

            maxHp = hp = datas.getHp() > 0 ? datas.getHp() : 100; // 未設定なら100
            if (owner == ShipKinds.ENEMY)
                maxHp = hp *= StageModifiers.enemyHpRate(); // 難易度・ステージ条件による敵の硬さはHPで表す(防御力は変えない)
            collisionSize = datas.getCollisionSize() * ShootingGameMain.getScreenSizeAdj() * sizeScale();
            imageSize.put("ship", (int) (datas.getImageSize() * ShootingGameMain.getScreenSizeAdj() * sizeScale()));
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
        updateAffix(dt);

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
        drawAffixRing(g2);
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
    /**
     * エフェクトを付ける(出現した直後に1度だけ呼ぶ)
     * 
     * @param affix 付けるエフェクト(nullなら何もしない)
     */
    public void applyAffix(EnemyAffix affix) {
        if (affix == null || this.affix != null)
            return;
        this.affix = affix;
        switch (affix) {
            case SWIFT -> speed *= EnemyAffix.SWIFT_SPEED_RATE;
            case ARMORED -> {
                maxHp *= EnemyAffix.ARMORED_HP_RATE;
                hp *= EnemyAffix.ARMORED_HP_RATE;
            }
            default -> {
            }
        }
    }

    /**
     * エフェクトによる毎フレームの効果(再生・連射)
     */
    private void updateAffix(double dt) {
        if (affix == EnemyAffix.REGEN && hp > 0)
            hp = Math.min(maxHp, hp + maxHp * EnemyAffix.REGEN_RATE_PER_SEC * dt);
        else if (affix == EnemyAffix.RAPID)
            updateCooldownTimer(dt * (1 / EnemyAffix.RAPID_COOLDOWN_RATE - 1)); // 余分に減らして、間隔を短くする
    }

    /**
     * エフェクトの輪(機体のまわりに、種類ごとの色でうっすら光る)
     */
    private void drawAffixRing(Graphics2D g2) {
        if (affix == null)
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        double r = imageSize.getOrDefault("ship", 30) / 2.0 + 4 * adj;
        double pulse = 0.6 + 0.4 * Math.sin(System.nanoTime() / 1.5e8);
        Color c = affix.getColor();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new java.awt.BasicStroke((float) (3 * adj)));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (60 * pulse)));
        g2.draw(new java.awt.geom.Ellipse2D.Double(pos.getX() - r - 2 * adj, pos.getY() - r - 2 * adj, (r + 2 * adj) * 2, (r + 2 * adj) * 2));
        g2.setStroke(new java.awt.BasicStroke((float) (1.5 * adj)));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (200 * pulse)));
        g2.draw(new java.awt.geom.Ellipse2D.Double(pos.getX() - r, pos.getY() - r, r * 2, r * 2));
    }

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
        // 設定のない船は弾を撃たない(毎フレーム呼ばれるので、ここで止まらないようにする)
        ShipManager.ShipDefinition definition = SHIP_M.getShipConfig().get(owner).get(id);
        for (int i = 0; definition != null && i <= level; i++) {
            String levelKey = "Lv" + i;

            ShipManager.ShotConfig[] shotsForLevel = definition.getLevelData(levelKey);
            // 取得したデータがnullではなければ結合
            if (shotsForLevel != null)
                shotData.addAll(Arrays.asList(shotsForLevel));
        }

        shotBullet.clear();
        Map<BulletKinds, Map<Integer, Double>> cooldownOverride = new HashMap<>(); // 弾ごとの発射間隔の指定
        for (ShipManager.ShotConfig config : shotData) {
            BulletKinds kind = config.getBulletKind();
            int bulletId = config.getBulletId();

            if (coolDownTimers.get(kind).get(bulletId) <= 0.0f) { // クールダウンタイマーが0以下の時
                shotBullet.computeIfAbsent(kind, k -> new ArrayList<>()).add(bulletId); // 発射した弾の種類を登録
                if (config.getCooldown() > 0)
                    cooldownOverride.computeIfAbsent(kind, k -> new HashMap<>()).put(bulletId, config.getCooldown());
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
                            LogUtils.warn(component,
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
                    LogUtils.warn(component,
                            "Missing refercence: " + ANSI.YELLOW + id + " - Lv" + shipLevel + " - bulletVecRef"
                                    + ANSI.RESET);

            }
        }

        // 発射した弾種のクールタイムリセット
        shotBullet.forEach((kind, idList) -> {
            for (Integer bulletId : idList) {
                Double override = cooldownOverride.getOrDefault(kind, Map.of()).get(bulletId);
                if (override != null)
                    coolDownTimers.get(kind).put(bulletId, override * fireRateScale());
                else
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
            coolDownTimers.get(kinds).put(bulletId, cooldownValue * fireRateScale());
    }

    /**
     * 発射間隔の倍率(自機とサブ機はスキルの連射で短くなる)
     */
    private double fireRateScale() {
        return owner == ShipKinds.PLAYER ? 1.0 / (1 + SkillService.getBonus("FIRE_RATE")) : 1.0;
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
        return rollCoinAmount(coins);
    }

    /**
     * コインの範囲("最小~最大")から、ドロップ量を決める。読めない書き方なら0。
     */
    public static int rollCoinAmount(String range) {
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
        if (shieldTimer <= 0) {
            hp -= damage;
            DamagePopupManager.getInstance().add(this, damage, critHit);
        }
    }

    private boolean critHit; // addCritDamage の処理中だけtrue

    /**
     * 会心のダメージの追加(ダメージ数字を会心表示にする)
     */
    public void addCritDamage(double damage) {
        critHit = true;
        try {
            addDamage(damage);
        } finally {
            critHit = false;
        }
    }

    /**
     * 色の概念がないか(ボスは色に関係なくダメージを受け、色に関係なく接触する)
     * 
     * @return 色の概念がなければtrue
     */
    public boolean isColorless() {
        return owner == ShipKinds.BOSS;
    }

    /**
     * 円との当たり判定
     * 通常の敵は画像の不透明な部分(見た目)と判定し、それ以外は円(collisionSize)で判定する
     * 
     * @param center 円の中心
     * @param radius 円の半径
     * @return 当たっていればtrue
     */
    public boolean collidesWithCircle(Vector2 center, double radius) {
        BufferedImage image = null;
        if (owner == ShipKinds.ENEMY && animationId.get("ship") != null)
            image = AM.getCurrentImage(animationId.get("ship"));

        double dx = center.getX() - pos.getX();
        double dy = center.getY() - pos.getY();

        // 画像がなければ円で判定
        if (image == null)
            return dx * dx + dy * dy < Math.pow(collisionSize + radius, 2);

        // 円の中心を船の画像の座標系(中心基準、回転前)に変換
        double cos = Math.cos(rotate);
        double sin = Math.sin(rotate);
        double lx = dx * cos + dy * sin + image.getWidth() / 2.0;
        double ly = -dx * sin + dy * cos + image.getHeight() / 2.0;

        // 円が画像の範囲外なら当たっていない
        if (lx < -radius || lx > image.getWidth() + radius || ly < -radius || ly > image.getHeight() + radius)
            return false;

        // 円の範囲内に不透明なピクセルがあるか
        int minX = Math.max(0, (int) Math.floor(lx - radius));
        int maxX = Math.min(image.getWidth() - 1, (int) Math.ceil(lx + radius));
        int minY = Math.max(0, (int) Math.floor(ly - radius));
        int maxY = Math.min(image.getHeight() - 1, (int) Math.ceil(ly + radius));
        double rSq = radius * radius;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double ddx = x + 0.5 - lx;
                double ddy = y + 0.5 - ly;
                if (ddx * ddx + ddy * ddy <= rSq && ((image.getRGB(x, y) >>> 24) & 0xFF) >= HIT_ALPHA)
                    return true;
            }
        }
        return false;
    }

    /**
     * 船体衝突時に受けるダメージ量
     * 
     * @return ダメージ量
     */
    public double getRamDamage() {
        return maxHp * 0.1;
    }

    /**
     * 除去要求(ShipManagerが更新後に安全に取り除く)
     */
    public void requestRemoval() {
        removeRequested = true;
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

    /**
     * 体の大きさ(半径)。ブロックなどに押し出されるときの大きさで、当たり判定より大きい
     */
    public double bodyRadius() {
        return Math.max(collisionSize, imageSize.getOrDefault("ship", 30) * 0.3);
    }

    /**
     * 見た目と当たり判定の大きさの倍率(ShipConfigの大きさにかける)
     * コンストラクタの中から呼ばれるので、フィールドは使わず固定の値を返すこと。
     */
    protected double sizeScale() {
        return 1.0;
    }
}
