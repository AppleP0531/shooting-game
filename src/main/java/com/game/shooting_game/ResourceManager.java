package com.game.shooting_game;

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

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.interfaces.Collectible;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

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

    // レアリティの種類
    public static enum Rarity {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }

    // 画像種類
    public static enum ImageType {
        ITEM,
        VIEW
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

        @JsonProperty("rarity")
        private Map<Rarity, Map<ImageType, ResourceData>> resData;
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
    private Map<ResourceKinds, Map<Rarity, BufferedImage[]>> resourceImageCache; // 画像データ
    private Map<ResourceKinds, Map<Rarity, BufferedImage[]>> resourceViewCache; // 画像データ
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
        final String IMAGE_BASE_PATH = "/static/img/resource/";

        resourceImageCache = new HashMap<>();
        resourceViewCache = new HashMap<>();
        // ResourceKindsの階層でループ
        for (Map.Entry<ResourceKinds, ResourceConfig> kindEntry : resourceConfigMap.entrySet()) {
            ResourceKinds kind = kindEntry.getKey();
            ResourceConfig config = kindEntry.getValue();
            Map<Rarity, Map<ImageType, ResourceData>> data = config.getResData();

            // レア度の階層でループ
            for (Map.Entry<Rarity, Map<ImageType, ResourceData>> rarityEntry : data.entrySet()) {
                Rarity rarity = rarityEntry.getKey();
                ResourceData resourceImagedata = rarityEntry.getValue().get(ImageType.ITEM);
                ResourceData resourceViewdata = rarityEntry.getValue().get(ImageType.VIEW);

                String imagePath = IMAGE_BASE_PATH + resourceImagedata.getImagePath();
                try {
                    // アイテムイメージ
                    BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                    Vector2 grids = resourceImagedata.getGrids();
                    int useCount = resourceImagedata.getUseCount();
                    double imageSize = resourceImagedata.getImageSize() * ShootingGameMain.getScreenSizeAdj();
                    BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount, imageSize);

                    // 画像をキャッシュに保存
                    resourceImageCache.computeIfAbsent(kind, k -> new HashMap<>()).put(rarity, frames);

                    // アイテムビュー
                    imagePath = UIBuilder.createPathWithSuffix(imagePath, "_view");
                    masterSheet = UIBuilder.loadImage(imagePath);
                    grids = resourceViewdata.getGrids();
                    useCount = resourceViewdata.getUseCount();
                    imageSize = resourceViewdata.getImageSize() * ShootingGameMain.getScreenSizeAdj();
                    frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount, imageSize);

                    // 画像をキャッシュに保存
                    resourceViewCache.computeIfAbsent(kind, k -> new HashMap<>()).put(rarity, frames);
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
    public Map<ResourceKinds, Map<Rarity, BufferedImage[]>> getResourceImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceImageCache);
    }

    /**
     * ビューデータの取得
     * 
     * @return ビューデータ
     */
    public Map<ResourceKinds, Map<Rarity, BufferedImage[]>> getResourceView() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(resourceViewCache);
    }
}
