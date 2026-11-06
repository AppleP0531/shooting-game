package com.game.shooting_game.domain.shop;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.LogUtils;

/**
 * ショップ(ボム・フリーズ。敵のドロップではなく購入してステージに持ち込む)
 * <p>
 * 所持数は progress.highScores のキー {@code "inv:" + 種類名}(値=所持数)に記録する。
 * 注意: "mission:" で始まるキーは★の数に数えられるので、その接頭辞にはしないこと。
 * 管理者モードではコインを消費しない。
 */
public final class ShopService {
    private static final String component = "[ShopService]";
    private static final String CONFIG_PATH = "/static/configs/ShopConfig.json";
    private static final String INV_PREFIX = "inv:";

    /** 商品 */
    public record ShopItem(ItemKinds kind, int price, int maxOwned, String name, String description) {
    }

    private static List<ShopItem> items;

    private ShopService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (items == null)
            reload();
    }

    /** 設定を読み込み直す(失敗したら既定の商品) */
    public static synchronized void reload() {
        List<ShopItem> loaded = new ArrayList<>();
        try (InputStream is = ShopService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            JsonNode root = new ObjectMapper().readTree(is);
            for (JsonNode n : root.path("items")) {
                try {
                    ItemKinds kind = ItemKinds.valueOf(n.path("kind").asText(""));
                    loaded.add(new ShopItem(kind,
                            Math.max(0, n.path("price").asInt(0)),
                            Math.max(1, n.path("maxOwned").asInt(10)),
                            n.path("name").asText(kind.name()),
                            n.path("description").asText("")));
                } catch (IllegalArgumentException e) {
                    LogUtils.warn(component, "不明な商品を無視します: " + n.path("kind").asText(""));
                }
            }
        } catch (Exception e) {
            LogUtils.warn(component, "ショップの設定を読めないため既定値を使います: " + e.getMessage());
            loaded.clear();
        }
        if (loaded.isEmpty())
            loaded = defaultItems();
        items = List.copyOf(loaded);
    }

    private static List<ShopItem> defaultItems() {
        return List.of(
                new ShopItem(ItemKinds.BOMB, 80000, 10, "ボム", "画面の敵弾を消し、敵全体にダメージ"),
                new ShopItem(ItemKinds.FREEZE, 100000, 10, "フリーズ", "敵(ボス以外)を4秒止め、ボスは動きを半分にする"),
                new ShopItem(ItemKinds.DURABLE_SHIELD, 120000, 10, "耐久シールド",
                        "最大HPの50%ぶんの耐久のバリアを張る。クールタイムなしで、耐久が尽きるまで被弾を吸う"),
                new ShopItem(ItemKinds.SKIP_TICKET, 150000, 10, "研究スキップ券", "スキルツリー強化の研究の待ち時間を、すぐ終わらせる"));
    }

    // ----- 参照 -----
    public static List<ShopItem> items() {
        ensureLoaded();
        return items;
    }

    /** 商品情報(無ければ null) */
    public static ShopItem find(ItemKinds kind) {
        for (ShopItem it : items())
            if (it.kind() == kind)
                return it;
        return null;
    }

    /** 保存キー(例: inv:BOMB) */
    public static String key(ItemKinds kind) {
        return INV_PREFIX + kind.name();
    }

    public static int owned(ItemKinds kind) {
        Map<String, Integer> s = store();
        return s == null || kind == null ? 0 : Math.max(0, s.getOrDefault(key(kind), 0));
    }

    /** 買えるか判定する純粋関数(管理者モードならコインは問わない) */
    public static boolean canBuy(int coins, int price, int owned, int maxOwned, boolean admin) {
        return owned < maxOwned && (admin || coins >= price);
    }

    public static boolean canBuy(ItemKinds kind) {
        ShopItem it = find(kind);
        if (it == null || store() == null)
            return false;
        return canBuy(DataManager.getUserData().getResources().getCoins(), it.price(), owned(kind), it.maxOwned(),
                DataManager.isAdminMode());
    }

    // ----- 操作 -----
    /** 購入する(買えないときは false) */
    public static boolean buy(ItemKinds kind) {
        if (!canBuy(kind))
            return false;
        ShopItem it = find(kind);
        UserData ud = DataManager.getUserData();
        if (!DataManager.isAdminMode())
            ud.getResources().setCoins(ud.getResources().getCoins() - it.price());
        store().put(key(kind), owned(kind) + 1);
        return true;
    }

    /** 1つ使う(持っていなければ false) */
    public static boolean consume(ItemKinds kind) {
        int n = owned(kind);
        if (n <= 0)
            return false;
        store().put(key(kind), n - 1);
        return true;
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }
}
