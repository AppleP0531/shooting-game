package src;

import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;
import src.interfaces.Manager;
import src.interfaces.Collectible;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ResourceManager implements Manager {
    private static final String component = "[ResourceManager]";
    private static ResourceManager instance;

    // リソースの種類
    public static enum ResourceKinds implements Collectible {
        COIN,
        RESOURCE_1,
        RESOURCE_2,
        RESOURCE_3,
        RESOURCE_4
    }

    // ----- データクラスの定義 -----
    /**
     * リソースコンフィグ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ResourceConfig {
        private int id;
        private Map<String, ResourceData> rarity;
    }

    /**
     * リソースデータ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ResourceData {
        private String imagePath;
        private Vector2 grids;
        private int useCount;
        private double frameRate;
        private int imageSize;
    }

    private Map<ResourceKinds, ResourceConfig> resourceConfigMap; // リソースデータが格納されたマップ
    private Map<ResourceKinds, Map<String, BufferedImage[]>> resourceImageCache; // 画像データ
    private static final TypeReference<Map<ResourceKinds, ResourceConfig>> TYPE_REF = new TypeReference<>() {
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
    private void loadImages() {
        resourceImageCache = new HashMap<>();
        // ResourceKindsの階層でループ
        for (Map.Entry<ResourceKinds, ResourceConfig> kindEntry : resourceConfigMap.entrySet()) {
            ResourceKinds kind = kindEntry.getKey();
            Map<String, ResourceData> data = kindEntry.getValue().getRarity();

            // レア度の階層でループ
            for (Map.Entry<String, ResourceData> rarityEntry : data.entrySet()) {
                String rarity = rarityEntry.getKey();
                ResourceData recourcedata = rarityEntry.getValue();
                String imagePath = recourcedata.getImagePath();

                if (imagePath == null) {
                    // 画像パス未定義ログ
                    LogUtils.systemLog(
                            "[WARN]",
                            component,
                            "imagePath for resource " + ANSI.YELLOW + kind + ANSI.RESET + " is not defined.");
                    continue;
                }

                try {
                    BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                    Vector2 grids = recourcedata.getGrids();
                    int useCount = recourcedata.getUseCount();
                    double imageSize = recourcedata.getImageSize() * ShootingGameMain.getScreenSizeAdj();
                    BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount, imageSize);

                    // 画像をキャッシュに保存
                    resourceImageCache.computeIfAbsent(kind, k -> new HashMap<>()).put(rarity, frames);
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
    public Map<ResourceKinds, ResourceConfig> getResourceConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceConfigMap);
    }

    /**
     * 画像データの取得
     * 
     * @return 画像データ
     */
    public Map<ResourceKinds, Map<String, BufferedImage[]>> getResourceImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceImageCache);
    }
}
