package com.game.shooting_game;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.interfaces.Collectible;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ResourceManager implements Manager {
    private static final String component = "[ResourceManager]";
    private static ResourceManager instance;

    /**
     * リソース種
     */
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ResourceKinds implements Collectible {
        public static final ResourceKinds COIN = new ResourceKinds(ResourceName.COIN, Rarity.COMMON);

        /**
         * 素材の系統
         */
        public static enum Family {
            UPGRADE, // スキル強化用
            PART, // サブ機パーツ制作用
            MODULE, // モジュール基板(モジュールの制作にのみ使う)
            NONE // 素材ではない
        }

        /**
         * リソース名
         */
        public static enum ResourceName {
            // 強化素材(スキル強化にのみ使う)
            GEAR(Family.UPGRADE, "歯車"),
            SCREW(Family.UPGRADE, "ネジ"),
            // パーツ素材(サブ機パーツの制作にのみ使う)
            COPPER_PLATE(Family.PART, "銅板"),
            IRON_BAR(Family.PART, "鉄棒"),
            // 素材ではないもの
            COIN(Family.NONE, "コイン"),
            FRAME(Family.NONE, "枠"),
            // 以降は追加分(順番を変えるとコマンドの数値指定がずれるので末尾に足す)
            ENERGY_CELL(Family.UPGRADE, "エネルギーセル"),
            CIRCUIT(Family.UPGRADE, "回路基板"),
            LENS(Family.UPGRADE, "光学レンズ"),
            ALLOY_PLATE(Family.PART, "合金板"),
            CRYSTAL_SHARD(Family.PART, "結晶片"),
            PLASMA_CORE(Family.PART, "プラズマコア"),
            MODULE_CHIP(Family.MODULE, "モジュール基板");

            private final Family family;
            private final String displayName;

            ResourceName(Family family, String displayName) {
                this.family = family;
                this.displayName = displayName;
            }

            /** 素材の系統(強化素材とパーツ素材は用途が重ならない) */
            public Family getFamily() {
                return family;
            }

            /** 表示名 */
            public String getDisplayName() {
                return displayName;
            }

            public static ResourceName fromString(String value) {
                try {
                    int code = Integer.parseInt(value);
                    ResourceName[] values = ResourceName.values();
                    if (code < 0 || code >= values.length) {
                        throw new IllegalArgumentException("Invalid resource code: " + code);
                    }
                    return values[code];
                } catch (NumberFormatException e) {
                    return ResourceName.valueOf(value.toUpperCase());
                }
            }
        }

        /**
         * レアリティ
         */
        public static enum Rarity {
            COMMON,
            UNCOMMON,
            RARE,
            EPIC,
            LEGENDARY;

            public static Rarity fromString(String value) {
                try {
                    int code = Integer.parseInt(value);
                    Rarity[] values = Rarity.values();
                    if (code < 0 || code >= values.length) {
                        throw new IllegalArgumentException("Invalid rarity code: " + code);
                    }
                    return values[code];
                } catch (NumberFormatException e) {
                    return Rarity.valueOf(value.toUpperCase());
                }
            }
        }

        // MySQLのenum型ではなく文字列(VARCHAR)として保存する(素材を追加してもテーブル定義を変えずに済む)
        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(length = 32)
        private ResourceName resName;
        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(length = 16)
        private Rarity rarity;

        @com.fasterxml.jackson.annotation.JsonCreator
        public ResourceKinds(String key) {
            this.resName = ResourceName.valueOf(key.toUpperCase());
            this.rarity = Rarity.COMMON;
        }

        public ResourceKinds getResourceKind() {
            return new ResourceKinds(resName, rarity);
        }
    }

    // ----- データクラスの定義 -----
    /**
     * リソースコンフィグ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ResourceConfig {
        private String imagePath;

        @JsonProperty("rarity")
        private Map<Rarity, ResourceData> resData;
    }

    /**
     * リソースデータ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ResourceData {
        private Vector2 grids;
        private int useCount;
        private double frameRate;
        private int imageSize;
        // 表示用(リザルトなど)の別画像。<名前>_<レア度>_view.png を使う。未指定なら通常の画像を使う
        private Vector2 viewGrids;
        private int viewUseCount;
        private double viewFrameRate; // 0なら通常のframeRateを使う
    }

    private Map<ResourceName, ResourceConfig> resourceConfigMap; // リソースデータが格納されたマップ
    private Map<ResourceName, Map<Rarity, BufferedImage[]>> resourceImageCache; // 画像データ
    private Map<ResourceName, Map<Rarity, BufferedImage[]>> resourceViewCache; // 表示用画像データ
    private static final TypeReference<Map<ResourceName, ResourceConfig>> TYPE_REF = new TypeReference<>() {
    };

    private ResourceManager() {
        ResourceManager.instance = this;

        reload();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static ResourceManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, ResourceManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/ResourceConfig.json";
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            // ロード開始路湯
            LogUtils.systemLog(
                    "[DEBUG]",
                    component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET);

            if (is == null)
                throw new IOException("Resource not foune: " + CONFIG_PATH);

            // データを読み込む
            resourceConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.systemLog("[DEBUG]", component, "Configuration loaded successfully");
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
    private void loadImages() {
        final String IMAGE_BASE_PATH = "/static/img/resource";

        resourceImageCache = new HashMap<>();
        resourceViewCache = new HashMap<>();
        // ResourceNameの階層でループ
        for (Map.Entry<ResourceName, ResourceConfig> kindEntry : resourceConfigMap.entrySet()) {
            ResourceName name = kindEntry.getKey();
            ResourceConfig config = kindEntry.getValue();
            Map<Rarity, ResourceData> data = config.getResData();

            // レア度の階層でループ
            for (Map.Entry<Rarity, ResourceData> rarityEntry : data.entrySet()) {
                Rarity rarity = rarityEntry.getKey();
                ResourceData resourceImagedata = rarityEntry.getValue();

                // アイテムイメージ
                String imagePath = "";
                String suffix = "_" + rarity.toString().toLowerCase();
                try {
                    imagePath = resourceConfigMap.get(name).getImagePath();
                    imagePath = UIBuilder.createPathWithSuffix(IMAGE_BASE_PATH + imagePath, suffix);
                    BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                    Vector2 grids = resourceImagedata.getGrids();
                    int useCount = resourceImagedata.getUseCount();
                    BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount);

                    // 画像をキャッシュに保存
                    resourceImageCache.computeIfAbsent(name, k -> new HashMap<>()).put(rarity, frames);

                    // 表示用イメージ(指定があるときだけ)
                    if (resourceImagedata.getViewGrids() != null) {
                        String viewPath = UIBuilder.createPathWithSuffix(imagePath, "_view");
                        try {
                            BufferedImage viewSheet = UIBuilder.loadImage(viewPath);
                            int viewCount = resourceImagedata.getViewUseCount() > 0
                                    ? resourceImagedata.getViewUseCount()
                                    : useCount;
                            resourceViewCache.computeIfAbsent(name, k -> new HashMap<>()).put(rarity,
                                    UIBuilder.cutSpriteSheet(viewSheet, resourceImagedata.getViewGrids(), viewCount));
                        } catch (IOException e) {
                            LogUtils.systemLog(
                                    "[WARN]",
                                    component,
                                    "Failed to load view image: " + ANSI.YELLOW + viewPath + ANSI.RESET + ".");
                        }
                    }
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

    @Override
    public void setup() {

    }

    @Override
    public void update(double dt) {

    }

    @Override
    public void draw(Graphics g) {

    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
        loadImages();
    }

    /**
     * リソースデータの取得
     * 
     * @return リソースデータ
     */
    public Map<ResourceName, ResourceConfig> getResourceConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceConfigMap);
    }

    /**
     * 画像データの取得
     * 
     * @return 画像データ
     */
    public Map<ResourceName, Map<Rarity, BufferedImage[]>> getResourceImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceImageCache);
    }

    /**
     * 表示用画像データの取得(リザルトなど。指定のないリソースは含まれない)
     * 
     * @return 表示用画像データ
     */
    public Map<ResourceName, Map<Rarity, BufferedImage[]>> getResourceView() {
        return Collections.unmodifiableMap(resourceViewCache);
    }
}
