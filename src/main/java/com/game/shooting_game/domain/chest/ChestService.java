package com.game.shooting_game.domain.chest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.module.Module;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.domain.star.StarRewardService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.utils.LogUtils;

/**
 * 宝箱と鍵
 * <p>
 * 鍵(銅・銀・金)を、初回クリア・ボス撃破・ミッションの★でもらい、宝箱に使うと、時間をかけて開く
 * (同時に開けられるのは {@link #MAX_SLOTS} 個まで。研究スキップ券で、すぐ開く)。
 * 鍵の種類が、開く宝箱のランクになる。中身(コイン・素材・モジュール)はランクごとに ChestConfig.json で決める。
 * <p>
 * 保存は progress.highScores の接頭辞つきキーに相乗りする:
 * <ul>
 * <li>{@code key:ランク} 鍵の所持数</li>
 * <li>{@code chest:番号:rank} スロットの宝箱のランク(銅=1, 銀=2, 金=3。空なら無い)</li>
 * <li>{@code chest:番号:at} 開く時刻(基準の時刻 {@link #BASE_EPOCH_SEC} からの秒数。int に収める)</li>
 * </ul>
 */
public final class ChestService {
    private static final String component = "[ChestService]";
    private static final String CONFIG_PATH = "/static/configs/ChestConfig.json";
    private static final String KEY_PREFIX = "key:";
    private static final String CHEST_PREFIX = "chest:";

    /** 同時に開けられる宝箱の数(スロット数) */
    public static final int MAX_SLOTS = 10;
    /** 開く時刻の基準(2026-01-01 00:00 UTC。秒数を int に収めるため) */
    public static final long BASE_EPOCH_SEC = 1767225600L;

    /** 鍵・宝箱のランク */
    public enum Rank {
        COPPER("銅", 600), SILVER("銀", 3600), GOLD("金", 14400);

        private final String label;
        private final int defaultSeconds;

        Rank(String label, int defaultSeconds) {
            this.label = label;
            this.defaultSeconds = defaultSeconds;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 中身の設定(ランクごと) */
    public record Table(int openSeconds, int coinMin, int coinMax, List<ResourceEntry> resources,
            List<ModuleEntry> modules) {
    }

    /** 素材の1行(確率で、最小〜最大個) */
    public record ResourceEntry(ResourceName name, Rarity rarity, int min, int max, double chance) {
    }

    /** モジュールの1行(確率で1個) */
    public record ModuleEntry(Rarity rarity, double chance) {
    }

    /** 宝箱から出たもの(モジュールは、受け取るときに作る) */
    public record Loot(int coins, Map<ResourceKinds, Integer> resources, List<Rarity> moduleRarities) {
    }

    /** 受け取った結果(作ったモジュールつき) */
    public record Opened(Loot loot, List<Module> modules) {
    }

    private static Map<Rank, Table> tables;

    private ChestService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (tables == null)
            reload();
    }

    /** 設定を読み込み直す(読めなければ、時間だけの既定の表) */
    public static synchronized void reload() {
        Map<Rank, Table> loaded = new EnumMap<>(Rank.class);
        try (InputStream is = ChestService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            JsonNode root = new ObjectMapper().readTree(is);
            for (Rank rank : Rank.values()) {
                JsonNode n = root.path(rank.name());
                if (n.isMissingNode())
                    continue;
                String[] coins = n.path("coins").asText("0~0").split("~");
                int cmin = Integer.parseInt(coins[0].trim());
                int cmax = coins.length > 1 ? Integer.parseInt(coins[1].trim()) : cmin;
                List<ResourceEntry> res = new ArrayList<>();
                for (JsonNode r : n.path("resources")) {
                    String[] amount = r.path("amount").asText("1~1").split("~");
                    int amin = Integer.parseInt(amount[0].trim());
                    int amax = amount.length > 1 ? Integer.parseInt(amount[1].trim()) : amin;
                    res.add(new ResourceEntry(ResourceName.valueOf(r.path("name").asText()),
                            Rarity.valueOf(r.path("rarity").asText("COMMON")), Math.max(0, amin),
                            Math.max(amin, amax), clamp01(r.path("chance").asDouble(1.0))));
                }
                List<ModuleEntry> mods = new ArrayList<>();
                for (JsonNode m : n.path("modules"))
                    mods.add(new ModuleEntry(Rarity.valueOf(m.path("rarity").asText("COMMON")),
                            clamp01(m.path("chance").asDouble(0))));
                loaded.put(rank, new Table(Math.max(0, n.path("openSeconds").asInt(rank.defaultSeconds)),
                        Math.max(0, cmin), Math.max(cmin, cmax), List.copyOf(res), List.copyOf(mods)));
            }
        } catch (Exception e) {
            LogUtils.warn(component, "宝箱の設定を読めないため既定値を使います: " + e.getMessage());
            loaded.clear();
        }
        for (Rank rank : Rank.values())
            loaded.computeIfAbsent(rank, r -> new Table(r.defaultSeconds, 0, 0, List.of(), List.of()));
        tables = loaded;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    public static Table table(Rank rank) {
        ensureLoaded();
        return tables.get(rank);
    }

    /** 開くのにかかる時間(秒) */
    public static int openSeconds(Rank rank) {
        return table(rank).openSeconds();
    }

    // ----- 鍵のドロップ(入手の決まり。画面に依存しない) -----
    /** 初回クリアでもらえる鍵(難易度が高いほどよい鍵) */
    public static Rank firstClearKey(Difficulty diff) {
        return rankOf(diff);
    }

    /** ボスを倒したときにもらえる鍵(確率は {@link #BOSS_KEY_CHANCE}) */
    public static Rank bossKey(Difficulty diff) {
        return rankOf(diff);
    }

    /** ボス撃破で鍵が出る確率 */
    public static final double BOSS_KEY_CHANCE = 0.35;
    /** 1回のクリアで、ミッションの★からもらえる鍵の最大数 */
    public static final int MISSION_KEY_CAP = 3;

    private static Rank rankOf(Difficulty diff) {
        if (diff == null)
            return Rank.COPPER;
        return switch (diff) {
            case EASY, NORMAL -> Rank.COPPER;
            case HARD -> Rank.SILVER;
            case NIGHTMARE -> Rank.GOLD;
        };
    }

    // ----- 保存 -----
    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    /** いまの時刻(基準からの秒数) */
    public static int nowSec() {
        return (int) (System.currentTimeMillis() / 1000 - BASE_EPOCH_SEC);
    }

    // ----- 鍵 -----
    public static int keyCount(Rank rank) {
        Map<String, Integer> s = store();
        return s == null ? 0 : s.getOrDefault(KEY_PREFIX + rank.name(), 0);
    }

    public static void addKey(Rank rank, int n) {
        Map<String, Integer> s = store();
        if (s != null && n > 0)
            s.merge(KEY_PREFIX + rank.name(), n, Integer::sum);
    }

    // ----- スロット -----
    /** スロットの宝箱のランク(空なら null) */
    public static Rank slotRank(int slot) {
        Map<String, Integer> s = store();
        if (s == null || slot < 0 || slot >= MAX_SLOTS)
            return null;
        int v = s.getOrDefault(CHEST_PREFIX + slot + ":rank", 0);
        return v >= 1 && v <= Rank.values().length ? Rank.values()[v - 1] : null;
    }

    /** 開くまでの残り秒数(空なら0) */
    public static int remaining(int slot, int now) {
        Map<String, Integer> s = store();
        if (slotRank(slot) == null || s == null)
            return 0;
        return Math.max(0, s.getOrDefault(CHEST_PREFIX + slot + ":at", 0) - now);
    }

    /** 開き終わっていて、受け取れるか */
    public static boolean isReady(int slot, int now) {
        return slotRank(slot) != null && remaining(slot, now) <= 0;
    }

    /** 開けている最中(まだ受け取っていない)宝箱の数 */
    public static int usedSlots() {
        int n = 0;
        for (int i = 0; i < MAX_SLOTS; i++)
            if (slotRank(i) != null)
                n++;
        return n;
    }

    /** 空いているスロット(なければ -1) */
    public static int freeSlot() {
        for (int i = 0; i < MAX_SLOTS; i++)
            if (slotRank(i) == null)
                return i;
        return -1;
    }

    /** 受け取れる宝箱の数(ロビーのバッジなど) */
    public static int readyCount(int now) {
        int n = 0;
        for (int i = 0; i < MAX_SLOTS; i++)
            if (isReady(i, now))
                n++;
        return n;
    }

    // ----- 開ける・受け取る -----
    /**
     * 鍵を1つ使って、宝箱を開け始める
     *
     * @return 使ったスロット(鍵がない・スロットが埋まっているときは -1。鍵は減らない)
     */
    public static int open(Rank rank, int now) {
        Map<String, Integer> s = store();
        int slot = freeSlot();
        if (s == null || slot < 0 || keyCount(rank) <= 0)
            return -1;
        s.merge(KEY_PREFIX + rank.name(), -1, Integer::sum);
        s.put(CHEST_PREFIX + slot + ":rank", rank.ordinal() + 1);
        s.put(CHEST_PREFIX + slot + ":at", now + openSeconds(rank));
        return slot;
    }

    /**
     * 研究スキップ券を1枚使って、開き終わらせる
     *
     * @return 使えたらtrue(空のスロット・すでに開き終わっている・券がないときはfalse。券は減らない)
     */
    public static boolean skip(int slot, int now) {
        Map<String, Integer> s = store();
        if (s == null || slotRank(slot) == null || remaining(slot, now) <= 0)
            return false;
        if (!DataManager.isAdminMode() && !ShopService.consume(ItemKinds.SKIP_TICKET))
            return false;
        s.put(CHEST_PREFIX + slot + ":at", now);
        return true;
    }

    /** 中身を決める(乱数を渡せる。モジュールは、受け取るときに作る) */
    public static Loot roll(Rank rank, Random rnd) {
        Table t = table(rank);
        int coins = t.coinMin() >= t.coinMax() ? t.coinMin() : t.coinMin() + rnd.nextInt(t.coinMax() - t.coinMin() + 1);
        Map<ResourceKinds, Integer> res = new LinkedHashMap<>();
        for (ResourceEntry e : t.resources()) {
            if (rnd.nextDouble() >= e.chance())
                continue;
            int n = e.min() >= e.max() ? e.min() : e.min() + rnd.nextInt(e.max() - e.min() + 1);
            if (n > 0)
                res.merge(new ResourceKinds(e.name(), e.rarity()), n, Integer::sum);
        }
        List<Rarity> mods = new ArrayList<>();
        for (ModuleEntry m : t.modules())
            if (rnd.nextDouble() < m.chance())
                mods.add(m.rarity());
        return new Loot(coins, res, mods);
    }

    /**
     * 開き終わった宝箱を受け取る(中身を所持品へ入れ、スロットを空ける)
     *
     * @return 受け取った中身(まだ開き終わっていない・空のスロットなら null)
     */
    public static Opened claim(int slot, int now, Random rnd) {
        Map<String, Integer> s = store();
        if (s == null || !isReady(slot, now))
            return null;
        Rank rank = slotRank(slot);
        Loot loot = roll(rank, rnd);
        StarRewardService.grant(new Reward(loot.coins(), loot.resources(), 0));
        List<Module> modules = new ArrayList<>();
        for (Rarity r : loot.moduleRarities())
            modules.add(ModuleService.addNew(null, r));
        s.remove(CHEST_PREFIX + slot + ":rank");
        s.remove(CHEST_PREFIX + slot + ":at");
        return new Opened(loot, modules);
    }

    /** 時間の表示(1時間以上は h:mm:ss、それ未満は m:ss) */
    public static String formatTime(int sec) {
        int h = sec / 3600;
        int m = sec % 3600 / 60;
        int s = sec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
}
