package src.gameScene.bullet;

import src.ShootingGameMain;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;
import src.gameScene.ship.ShipManager.ShipKinds;
import src.interfaces.Manager;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 弾管理クラス
 */
public final class BulletManager implements Manager {
    private static final String component = "[BulletManager]";
    private static BulletManager instance;

    // 弾の種類
    public static enum BulletKinds {
        STRAIGHT,
        TRACKING,
        LASER,
        BALL
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
        private Map<String, Double> cooldowns = new HashMap<>();

        @JsonAnySetter
        public void addCooldown(String key, Double value) {
            this.cooldowns.put(key, value);
        }
    }

    private Map<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>> bulletConfigMap; // 弾データが格納されたマップ
    private Map<ShipKinds, Map<BulletKinds, Map<Integer, BufferedImage[]>>> bulletImageCache; // 画像データ

    private List<Bullet> bulletList = new ArrayList<>(); // 弾リスト
    private List<Bullet> bulletListToRemove = new ArrayList<>(); // 削除待ち弾リスト

    // bulletConfig用にネストされたMapの型の定義
    // Key 1: オブジェクト, Key 2: 弾の種類, Key 3: 情報名, Value: データ
    private static final TypeReference<Map<ShipKinds, Map<BulletKinds, Map<Integer, BulletConfig>>>> TYPE_REF = new TypeReference<>() {
    };

    // コンストラクタ
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
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET);
            LogUtils.systemLog(
                    "[FATAL]",
                    component,
                    ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    public void loadImages() {
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

                    String imagePath = data.getImagePath();

                    if (imagePath == null) {
                        // 画像パス未定義ログ
                        LogUtils.systemLog(
                                "[WARN]",
                                component,
                                "imagePath for " + ANSI.YELLOW + owner + " - " + kind + ANSI.RESET
                                        + " is not defined.");
                        continue;
                    }

                    try {
                        BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                        Vector2 grids = data.getGrids();
                        int useCount = data.getUseCount();
                        double imageSize = data.getImageSize() * ShootingGameMain.getScreenSizeAdj();
                        BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount, imageSize);

                        // 画像をキャッシュに保存
                        bulletImageCache
                                .computeIfAbsent(owner, o -> new HashMap<>())
                                .computeIfAbsent(kind, k -> new HashMap<>())
                                .put(type, frames);
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

    @Override
    public void setup() {
        bulletListToRemove.clear();
        bulletList.clear();
    }

    @Override
    public void update(double dt) {
        bulletList.forEach(bullet -> bullet.update(dt));
        // 条件を満たした弾を削除待ちリストに追加
        for (Bullet bullet : bulletList) {
            // 画面外に出た、または生存していない場合
            if (!bullet.inScreen() || !bullet.isLife())
                bulletListToRemove.add(bullet);
        }

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
    public Map<ShipKinds, Map<BulletKinds, Map<Integer, BufferedImage[]>>> getBulletImage() {
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