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
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.interfaces.Collectible;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.NullImage;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public final class ItemManager implements Manager {
    private static final String component = "[ItemManager]";
    private static ItemManager instance;

    private List<Item> itemList = new ArrayList<>(); // アイテムリスト
    private List<Item> itemListToRemove = new ArrayList<>(); // 削除待ちアイテムリスト
    @lombok.Getter
    @lombok.Setter
    private double magnetTimer; // マグネット効果の残り時間(0より大きい間は全アイテムを引き寄せる)
    @lombok.Getter
    private double regenTimer; // 継続回復の残り時間
    private double regenPerSec; // 継続回復の毎秒の回復量
    // 攻撃力アップ(ダメージ計算から、インスタンスなしで読めるように static)
    private static double atkUpTimer; // 攻撃力アップの残り時間
    private static double atkUpRate; // 攻撃力アップの倍率(例: 1.3)

    /**
     * 継続回復を始める(重ねて取ると、時間と毎秒の回復量を取り直す)
     */
    public void startRegen(double totalHeal, double seconds) {
        regenTimer = seconds;
        regenPerSec = totalHeal / seconds;
    }

    /**
     * 攻撃力アップを始める(重ねて取ると、時間と倍率を取り直す)
     */
    public static void startAtkUp(double rate, double seconds) {
        atkUpRate = rate;
        atkUpTimer = seconds;
    }

    /**
     * 攻撃力アップの残り時間
     */
    public static double getAtkUpTimer() {
        return atkUpTimer;
    }

    /**
     * 攻撃力アップの今の倍率(効果中でなければ1)
     */
    public static double atkUpMultiplier() {
        return atkUpTimer > 0 ? atkUpRate : 1.0;
    }

    // アイテムの種類
    public static enum ItemKinds implements Collectible {
        HEAL,
        LEVEL_UP,
        SHIELD,
        BOMB,
        MAGNET,
        FREEZE,
        REGEN, // 少量継続回復
        RAMPAGE, // 一時的に暴走状態になる
        ATK_UP, // 一定時間、攻撃力アップ
        DURABLE_SHIELD, // ショップ専用(耐久シールド。耐久が尽きるまで被弾を吸う)
        SKIP_TICKET // ショップ専用(研究スキップ券。ステージには出ない)
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
            LogUtils.debug(component,
                    "Attempting to load configuration from " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET + ".");

            // nullチェック
            if (is == null)
                throw new IOException("Resource not found: " + CONFIG_PATH);

            // データを読み込む
            itemConfigMap = mapper.readValue(is, TYPE_REF);

            // ロード成功ログ
            LogUtils.debug(component,
                    "Configuration loaded successfully");
        } catch (IOException e) {
            // ロードエラーログ
            LogUtils.fatal(component,
                    "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + "." + ANSI.RESET + "\n"
                            + ANSI.RED + "Application will terminate." + ANSI.RESET,
                    e);
        }
    }

    /**
     * 画像読み込み
     */
    private void loadImages() {
        final String IMAGE_BASE_PATH = "/static/img/item";

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
                BufferedImage[] frames = UIBuilder.cutSpriteSheet(masterSheet, grids, useCount);

                // 画像をキャッシュに保存
                itemImageCache.put(kind, frames);
            } catch (IOException e) {
                switch (e.getMessage()) {
                    // 画像ファイル未発見ログ
                    case "FILE_NOT_FOUND" -> {
                        LogUtils.warn(component,
                                "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ". Using the null image.");
                        itemImageCache.put(kind, NullImage.frames(NullImage.Kind.ITEM, data.getUseCount()));
                    }

                    // 画像読み込みエラーログ
                    case "BROKEN_FILE" -> LogUtils.error(component,
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
        magnetTimer = 0;
        regenTimer = 0;
        atkUpTimer = 0;
    }

    @Override
    public void update(double dt) {
        magnetTimer = Math.max(0, magnetTimer - dt);
        atkUpTimer = Math.max(0, atkUpTimer - dt);
        if (regenTimer > 0) {
            double step = Math.min(dt, regenTimer);
            regenTimer -= step;
            var player = ShipManager.getInstance().getPlayer();
            if (player != null)
                player.heal(regenPerSec * step);
        }
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