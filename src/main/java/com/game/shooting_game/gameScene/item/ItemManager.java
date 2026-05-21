package com.game.shooting_game.gameScene.item;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.interfaces.Collectible;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ItemManager implements Manager {
    private static final String component = "[ItemManager]";
    private static ItemManager instance;

    private List<Item> itemList = new ArrayList<>(); // アイテムリスト
    private List<Item> itemListToRemove = new ArrayList<>(); // 削除待ちアイテムリスト

    // アイテムの種類
    public static enum ItemKinds implements Collectible {
        HEAL,
        LEVEL_UP,
        SHIELD
    }

    // ----- データクラスの定義 -----
    /**
     * アイテムコンフィグ
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ItemConfig {
        private String imagePath;
        private Vector2 grids;
        private int useCount;
        private double frameRate;
        private int imageSize;
        private double collisionSize;
        private double speed;
    }

    private Map<ItemKinds, ItemConfig> itemConfigMap; // アイテムデータが格納されたマップ
    private Map<ItemKinds, BufferedImage[]> itemImageCache; // 画像データ
    private static final TypeReference<Map<ItemKinds, ItemConfig>> TYPE_REF = new TypeReference<>() {
    };

    private ItemManager() {
        ItemManager.instance = this;

        reload();
    }

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static ItemManager getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, ItemManager::new);
    }

    /**
     * config読み込み
     */
    private void loadConfig() {
        final String CONFIG_PATH = "/static/configs/ItemConfig.json";

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
            itemConfigMap = mapper.readValue(is, TYPE_REF);

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
    private void loadImages() {
        final String IMAGE_BASE_PATH = "/static/img/item/";

        itemImageCache = new HashMap<>();
        // ItemKindsの階層でループ
        for (Map.Entry<ItemKinds, ItemConfig> kindEntry : itemConfigMap.entrySet()) {
            ItemKinds kind = kindEntry.getKey();
            ItemConfig data = kindEntry.getValue();

            String imagePath = IMAGE_BASE_PATH + data.getImagePath();
            try {
                BufferedImage masterSheet = UIBuilder.loadImage(imagePath);
                Vector2 grids = data.getGrids();
                int useCount = data.getUseCount();
                double imageSize = data.getImageSize() * ShootingGameMain.getScreenSizeAdj();
                BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount, imageSize);

                // 画像をキャッシュに保存
                itemImageCache.put(kind, frames);
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

    @Override
    public void setup() {
        itemListToRemove.clear();
        itemList.clear();
    }

    @Override
    public void update(double dt) {
        new ArrayList<>(itemList).forEach(item -> item.update(dt));

        // 削除待ちリストのアイテムをメインリストから削除してから、削除待ちリストを初期化
        itemList.removeAll(itemListToRemove);
        itemListToRemove.clear();
    }

    @Override
    public void draw(Graphics g) {
        new ArrayList<>(itemList).forEach(item -> item.draw(g));
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        loadConfig();
        loadImages();
    }

    /**
     * アイテムデータの取得
     * 
     * @return アイテムデータ
     */
    public Map<ItemKinds, ItemConfig> getItemConfig() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(itemConfigMap);
    }

    /**
     * 画像データの取得
     * 
     * @return 画像データ
     */
    public Map<ItemKinds, BufferedImage[]> getItemImage() {
        // 外部から変更できないように不変ビューを渡す
        return Collections.unmodifiableMap(itemImageCache);
    }

    /**
     * アイテムの追加
     * 
     * @param item アイテム
     */
    public void addItem(Item item) {
        if (item.getKind() instanceof ItemKinds) {
            // 同じアイテムがいくつあるかカウント
            int sameItemCount = 0;
            for (Item it : itemList) {
                if (it.getKind() == item.getKind()) {
                    sameItemCount++;
                }
            }

            // 同じアイテムが3つ未満の場合のみ追加
            if (sameItemCount < 3)
                itemList.add(item);
        } else if (item.getKind() instanceof ResourceKinds) {
            itemList.add(item);
        }
    }

    /**
     * 削除待ちリストに指定されたアイテムを追加
     * 
     * @param item 削除するアイテム
     */
    public void removeItem(Item item) {
        itemListToRemove.add(item);
    }

    /**
     * アイテムの削除
     */
    public void clearItems() {
        itemList.forEach(item -> itemListToRemove.add(item));
    }

    /**
     * アイテムリストの取得
     * 
     * @return アイテムリスト
     */
    public List<Item> getItemList() {
        // コピーの取得
        return new ArrayList<>(itemList);
    }
}