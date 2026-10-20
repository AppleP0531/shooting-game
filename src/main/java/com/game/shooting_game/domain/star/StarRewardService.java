package com.game.shooting_game.domain.star;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.utils.LogUtils;

/**
 * ★(ミッション達成数)のマイルストーン報酬
 * <p>
 * ★が interval(既定12)個たまるごとに報酬を受け取れる。受け取りは手動で、
 * 受取済みは progress.highScores のキー {@code "starReward:" + n}(値1)に記録する。
 * 注意: "mission:" で始まるキーは★の数に数えられるので、受取済みキーはその接頭辞にしないこと。
 */
public final class StarRewardService {
    private static final String component = "[StarRewardService]";
    private static final String CONFIG_PATH = "/static/configs/StarRewardConfig.json";
    private static final String CLAIMED_PREFIX = "starReward:";
    private static final int DEFAULT_INTERVAL = 12;

    /** 報酬(コインと素材) */
    public record Reward(int coins, Map<ResourceKinds, Integer> resources) {
    }

    private static int interval = DEFAULT_INTERVAL;
    private static List<Reward> tiers;

    private StarRewardService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (tiers == null)
            reload();
    }

    /** 設定を読み込み直す(失敗したら既定の表) */
    public static synchronized void reload() {
        int loadedInterval = DEFAULT_INTERVAL;
        List<Reward> loaded = new ArrayList<>();
        try (InputStream is = StarRewardService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            JsonNode root = new ObjectMapper().readTree(is);
            int iv = root.path("interval").asInt(DEFAULT_INTERVAL);
            loadedInterval = iv > 0 ? iv : DEFAULT_INTERVAL;
            for (JsonNode t : root.path("tiers"))
                loaded.add(parseTier(t));
        } catch (Exception e) {
            LogUtils.systemLog("[WARN]", component, "★報酬の設定を読めないため既定値を使います: " + e.getMessage());
            loadedInterval = DEFAULT_INTERVAL;
            loaded.clear();
        }
        if (loaded.isEmpty())
            loaded = defaultTiers();
        interval = loadedInterval;
        tiers = loaded;
    }

    private static Reward parseTier(JsonNode t) {
        int coins = Math.max(0, t.path("coins").asInt(0));
        Map<ResourceKinds, Integer> res = new LinkedHashMap<>();
        JsonNode r = t.path("resources");
        r.fieldNames().forEachRemaining(name -> {
            ResourceName rn = ResourceName.fromString(name);
            r.get(name).fields().forEachRemaining(en -> {
                int count = en.getValue().asInt(0);
                if (count > 0)
                    res.merge(new ResourceKinds(rn, Rarity.fromString(en.getKey())), count, Integer::sum);
            });
        });
        return new Reward(coins, res);
    }

    private static List<Reward> defaultTiers() {
        List<Reward> list = new ArrayList<>();
        Map<ResourceKinds, Integer> res = new LinkedHashMap<>();
        res.put(new ResourceKinds(ResourceName.GEAR, Rarity.COMMON), 5);
        res.put(new ResourceKinds(ResourceName.SCREW, Rarity.COMMON), 5);
        list.add(new Reward(500, res));
        return list;
    }

    // ----- 参照 -----
    public static int interval() {
        ensureLoaded();
        return interval;
    }

    public static int totalStars() {
        return DifficultyService.totalStars();
    }

    /** 到達したマイルストーンの数 */
    public static int milestoneCount() {
        return totalStars() / interval();
    }

    /** 次の目標★数(今の★より大きい最小の interval の倍数) */
    public static int nextTarget() {
        int iv = interval();
        return (totalStars() / iv + 1) * iv;
    }

    /** n番目(n=1 が interval 個)の報酬。表を超えたら最後の段を繰り返す */
    public static Reward rewardOf(int n) {
        ensureLoaded();
        int idx = Math.min(Math.max(n, 1), tiers.size()) - 1;
        return tiers.get(idx);
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    public static boolean isClaimed(int n) {
        Map<String, Integer> s = store();
        return s != null && s.containsKey(CLAIMED_PREFIX + n);
    }

    public static boolean canClaim(int n) {
        return store() != null && n >= 1 && n <= milestoneCount() && !isClaimed(n);
    }

    public static int claimableCount() {
        int c = 0;
        for (int n = 1; n <= milestoneCount(); n++)
            if (canClaim(n))
                c++;
        return c;
    }

    // ----- 受け取り -----
    /** 受け取る。受け取れないときは null(何度呼んでも二重には受け取れない) */
    public static Reward claim(int n) {
        if (!canClaim(n))
            return null;
        UserData ud = DataManager.getUserData();
        Reward r = rewardOf(n);
        ud.getResources().setCoins(ud.getResources().getCoins() + r.coins());
        Map<ResourceKinds, Integer> eq = ud.getResources().getEquipments();
        for (Map.Entry<ResourceKinds, Integer> e : r.resources().entrySet())
            eq.merge(e.getKey(), e.getValue(), Integer::sum);
        store().put(CLAIMED_PREFIX + n, 1);
        return r;
    }

    /** 受け取れるものをすべて受け取る(合計を返す。無ければ null) */
    public static Reward claimAll() {
        int coins = 0;
        Map<ResourceKinds, Integer> sum = new LinkedHashMap<>();
        boolean any = false;
        for (int n = 1; n <= milestoneCount(); n++) {
            Reward r = claim(n);
            if (r == null)
                continue;
            any = true;
            coins += r.coins();
            r.resources().forEach((k, v) -> sum.merge(k, v, Integer::sum));
        }
        return any ? new Reward(coins, sum) : null;
    }
}
