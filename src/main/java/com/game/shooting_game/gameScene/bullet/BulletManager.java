package com.game.shooting_game.gameScene.bullet;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.AnimationManager;

public final class BulletManager implements Manager {
    private static final String component = "[BulletManager]";
    private static BulletManager instance;

    // 弾の種類
    public static enum BulletKinds {
        STRAIGHT,
        CURVE,
        WAVE,
        TRACKING,
        LASER,
        BALL,
        SPIRAL, // 進行方向の左右にらせんを描いて進む(2発を逆位相で撃つと二重らせん)
        SPLIT // 一定時間進んだあと、扇状の小さな弾に分裂する
    }

    // ----- データクラスの定義 -----
    /**
     * 弾コンフィグ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class BulletConfig {
        private String imagePath;
        private Vector2 grids;
        private int useCount;
        private double frameRate;
        private int imageSize;
        private double collisionSize;
        private double speed;
        private double damage;
        private double diff; // Curve用: 変角量
        private double startSpeed; // 初速(0なら最初から speed)。accel と組み合わせて加速する弾にする
        private double accel; // 加速度(1秒あたりの速度の増加)。speed まで加速する
        private int pierce; // 貫通数(当たった敵を何体まで通り抜けるか。0なら当たって消える)
        private double amplitude; // Spiral用: らせんの振れ幅
        private double frequency; // Spiral用: らせんの速さ(rad/秒)
        private double splitTime; // Split用: 分裂するまでの時間(秒)
        private int splitCount; // Split用: 分裂後の弾の数
        private double splitSpread; // Split用: 分裂後の弾が広がる角度(度、全体)
        private int childId; // Split用: 分裂後の弾のID(STRAIGHT)
        private double childRate = 0.5; // Split用: 分裂後の弾のダメージ倍率
        private Map<String, Double> cooldowns = new HashMap<>();

        @JsonAnySetter
        public void addCooldown(String key, Double value) {
            this.cooldowns.put(key, value);
        }
    }

    private Map<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>> bulletConfigMap; // 弾データが格納されたマップ
    private Map<ShipKinds, Map<BulletKinds, Map<String, BufferedImage[]>>> bulletImageCache; // 画像データ

    private List<Bullet> bulletList = new ArrayList<>(); // 弾リスト
    private List<Bullet> bulletListToRemove = new ArrayList<>(); // 削除待ち弾リスト

    // bulletConfig用にネストされたMapの型の定義
    // Key 1: オブジェクト, Key 2: 弾の種類, Key 3: 情報名, Value: データ
    private static final TypeReference<Map<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>>> TYPE_REF = new TypeReference<>() {
    };

    private BulletManager() {
        BulletManager.instance = this;

        reload();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static BulletManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, BulletManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/BulletConfig.json";

        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            bulletConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.error(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET + "\n"
                            + ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    public void loadImages() {
        final String IMAGE_BASE_PATH = "/static/img/bullet";

        bulletImageCache = new HashMap<>();
        // ShipKindsの階層でループ
        for (Map.Entry<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>> ownerEntry : bulletConfigMap
                .entrySet()) {
            ShipKinds owner = ownerEntry.getKey();

            // BulletKindsの階層でループ
            for (Map.Entry<BulletKinds, Map<Integer, BulletConfig>> kindEntry : ownerEntry.getValue().entrySet()) {
                BulletKinds kind = kindEntry.getKey();

                // 弾タイプの階層でループ
                for (Map.Entry<Integer, BulletConfig> typeEntry : kindEntry.getValue().entrySet()) {
                    Integer type = typeEntry.getKey();
                    BulletConfig data = typeEntry.getValue();

                    if (data == null) {
                        // データ未定義ログ
                        LogUtils.systemLog(
                                "[WARN]",
                                component,
                                "data for " + ANSI.YELLOW + owner + " - " + kind + ANSI.RESET + " is not defined.");
                        continue;
                    }

                    for (ColorMode mode : ColorMode.values()) {
                        String imagePath = IMAGE_BASE_PATH + data.getImagePath();
                        try {
                            String suffix = "_" + mode.toString().toLowerCase();
                            imagePath = UIBuilder.createPathWithSuffix(imagePath, suffix);

                            BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                            Vector2 grids = data.getGrids();
                            int useCount = data.getUseCount();
                            BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount);

                            // 画像をキャッシュに保存
                            bulletImageCache
                                    .computeIfAbsent(owner, o -> new HashMap<>())
                                    .computeIfAbsent(kind, k -> new HashMap<>())
                                    .put(String.valueOf(type) + suffix, frames);
                        } catch (IOException e) {
                            switch (e.getMessage()) {
                                // 画像ファイル未発見ログ
                                case "FILE_NOT_FOUND" -> LogUtils.systemLog(
                                        "[WARN]",
                                        component,
                                        "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                                // 画像読み込みエラーログ
                                case "BROKEN_FILE" -> LogUtils.systemLog(
                                        "[ERROR]",
                                        component,
                                        "Failed to load image: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                                // その他エラーログ
                                default -> System.out.println(e);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setup() {
        bulletList.forEach(this::releaseAnimation);
        bulletListToRemove.clear();
        bulletList.clear();
    }

    @Override
    public void update(double dt) {
        // 更新中に分裂弾などが新しい弾を追加しても壊れないよう、コピーを回す
        new ArrayList<>(bulletList).forEach(bullet -> bullet.update(dt));
        // ブロックに当たった弾は消える(自機・敵のどちらの弾も。貫通弾とレーザーは通り抜ける)
        BeamManager beams = BeamManager.getInstance();
        for (Bullet bullet : new ArrayList<>(bulletList)) {
            if (bullet.isPiercing() || bullet.getKind() == BulletKinds.LASER || !bullet.isLife())
                continue;
            if (beams.blocksBullet(bullet.getPos(), bullet.getCollisionSize()))
                bullet.destroySelf();
        }
        // 条件を満たした弾を削除待ちリストに追加
        for (Bullet bullet : bulletList) {
            // 画面外に出た、または生存していない場合
            if (!bullet.inScreen() || !bullet.isLife())
                bulletListToRemove.add(bullet);
        }

        // 削除する弾のアニメーションを解放(解放しないとアニメーション更新が弾の数だけ溜まり続ける)
        bulletListToRemove.forEach(this::releaseAnimation);

        // 削除待ちリストの弾をメインリストから削除してから、削除待ちリストを初期化
        bulletList.removeAll(bulletListToRemove);
        bulletListToRemove.clear();
    }

    @Override
    public void draw(Graphics g) {
        new ArrayList<>(bulletList).forEach(bullet -> bullet.draw(g));
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
        loadImages();
    }

    /**
     * 弾の追加
     * 
     * @param bullet 弾
     */
    public void addBullet(Bullet bullet) {
        bulletList.add(bullet);
    }

    /**
     * 削除待ちリストに指定された弾の削除
     */
    public void clearBullets() {
        bulletList.forEach(bullet -> bulletListToRemove.add(bullet));
    }

    /**
     * プレイヤー以外の弾をすべて削除待ちにする(ボスのゲージ破壊時など)
     */
    public void clearEnemyBullets() {
        for (Bullet bullet : bulletList) {
            if (bullet.getOwner() != ShipKinds.PLAYER)
                bulletListToRemove.add(bullet);
        }
    }

    /**
     * 弾のアニメーションを解放
     * 
     * @param bullet 弾
     */
    private void releaseAnimation(Bullet bullet) {
        if (bullet.animationId != null)
            AnimationManager.getInstance().removeAnimation(bullet.animationId);
    }

    /**
     * 削除待ちリストに指定された弾を追加
     * 
     * @param bullet 削除する弾
     */
    public void removeBullet(Bullet bullet) {
        bulletListToRemove.add(bullet);
    }

    /**
     * 弾のデータの取得
     * 
     * @return 弾データ
     */
    public Map<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>> getBulletConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(bulletConfigMap);
    }

    /**
     * 画像データの取得
     * 
     * @return 画像データ
     */
    public Map<ShipKinds, Map<BulletKinds, Map<String, BufferedImage[]>>> getBulletImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(bulletImageCache);
    }

    /**
     * 弾のリストの取得
     * 
     * @return 弾リスト
     */
    public List<Bullet> getBulletList() {
        // コピーの取得
        return new ArrayList<>(bulletList);
    }
}