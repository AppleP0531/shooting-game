package com.game.shooting_game.domain.achievement;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.star.StarRewardService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.utils.LogUtils;

/**
 * 実績(段階式。目標の数に届くたびに、その段の報酬を受け取れる)
 * <p>
 * 数える値(指標)は2種類:
 * <ul>
 * <li>{@code stat:名前} 遊ぶたびに増える累計(progress.highScores に保存。{@link #add} で足す)</li>
 * <li>{@code derived:名前} すでにある記録から読むもの(cleared=クリア済みステージ / stars=★の数 / endlessWave=無限モードの最高ウェーブ)</li>
 * </ul>
 * 受け取った段の数は、{@code "ach:" + 実績ID}(値=受け取った段の数)に記録する。
 * 注意: "mission:" で始まるキーは★の数に数えられるので、同じ接頭辞にしないこと。
 */
public final class AchievementService {
    private static final String component = "[AchievementService]";
    private static final String CONFIG_PATH = "/static/configs/AchievementConfig.json";
    private static final String CLAIMED_PREFIX = "ach:";
    public static final String STAT_PREFIX = "stat:";
    private static final String DERIVED_PREFIX = "derived:";

    // 累計の指標の名前(遊んでいる最中に増やす場所が使う)
    public static final String KILLS = STAT_PREFIX + "kills";
    public static final String BOSS_KILLS = STAT_PREFIX + "bossKills";
    public static final String ITEMS = STAT_PREFIX + "items";
    public static final String COINS = STAT_PREFIX + "coins";
    public static final String PLAY_SECONDS = STAT_PREFIX + "playSeconds";
    public static final String GAME_OVERS = STAT_PREFIX + "gameOvers";

    /** 1段(目標の数と報酬) */
    public record Tier(int goal, Reward reward) {
    }

    /** 実績。divisor は表示のための割り算(秒→分など)、unit は表示の単位 */
    public record Achievement(String id, String title, String description, String metric, String unit, int divisor,
            List<Tier> tiers) {
    }

    private static List<Achievement> achievements;

    private AchievementService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (achievements == null)
            reload();
    }

    /** 設定を読み込み直す(読めなければ空) */
    public static synchronized void reload() {
        List<Achievement> loaded = new ArrayList<>();
        try (InputStream is = AchievementService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            for (JsonNode n : new ObjectMapper().readTree(is).path("achievements")) {
                List<Tier> tiers = new ArrayList<>();
                for (JsonNode t : n.path("tiers"))
                    tiers.add(new Tier(Math.max(1, t.path("goal").asInt(1)), StarRewardService.parseTier(t)));
                tiers.sort(java.util.Comparator.comparingInt(Tier::goal));
                loaded.add(new Achievement(n.path("id").asText(), n.path("title").asText(),
                        n.path("description").asText(""), n.path("metric").asText(), n.path("unit").asText(""),
                        Math.max(1, n.path("divisor").asInt(1)), List.copyOf(tiers)));
            }
        } catch (Exception e) {
            LogUtils.warn(component, "実績の設定を読めません: " + e.getMessage());
            loaded.clear();
        }
        achievements = List.copyOf(loaded);
    }

    public static List<Achievement> all() {
        ensureLoaded();
        return achievements;
    }

    // ----- 数える -----
    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    /** 累計の指標に足す(int の上限で止まる。データがなければ何もしない) */
    public static void add(String metric, int amount) {
        Map<String, Integer> s = store();
        if (s == null || amount <= 0 || com.game.shooting_game.domain.trial.TrialService.isActive())
            return; // お試し出撃では数えない
        s.merge(metric, amount, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
    }

    /** 指標のいまの値 */
    public static int value(String metric) {
        Map<String, Integer> s = store();
        if (s == null)
            return 0;
        if (metric.startsWith(DERIVED_PREFIX)) {
            return switch (metric.substring(DERIVED_PREFIX.length())) {
                case "cleared" -> DataManager.getUserData().getProgress().getClearedStage();
                case "stars" -> DifficultyService.totalStars();
                case "endlessWave" -> s.getOrDefault("endless:wave", 0);
                default -> 0;
            };
        }
        return s.getOrDefault(metric, 0);
    }

    public static int value(Achievement a) {
        return value(a.metric());
    }

    // ----- 段と受け取り -----
    /** 目標に届いた段の数 */
    public static int reachedTiers(Achievement a) {
        int v = value(a);
        int n = 0;
        for (Tier t : a.tiers())
            if (v >= t.goal())
                n++;
        return n;
    }

    /** 受け取り済みの段の数 */
    public static int claimedTiers(Achievement a) {
        Map<String, Integer> s = store();
        return s == null ? 0 : Math.min(s.getOrDefault(CLAIMED_PREFIX + a.id(), 0), a.tiers().size());
    }

    /** 受け取れる段があるか */
    public static boolean canClaim(Achievement a) {
        return store() != null && reachedTiers(a) > claimedTiers(a);
    }

    /** 受け取れる実績の数(ロビーのバッジなど) */
    public static int claimableCount() {
        int c = 0;
        for (Achievement a : all())
            if (canClaim(a))
                c++;
        return c;
    }

    /** 次の目標(すべて受け取り済みなら null) */
    public static Tier nextTier(Achievement a) {
        int claimed = claimedTiers(a);
        return claimed >= a.tiers().size() ? null : a.tiers().get(claimed);
    }

    /** 受け取れる段をすべて受け取る(合計の報酬を返す。受け取れなければ null) */
    public static Reward claim(Achievement a) {
        if (!canClaim(a))
            return null;
        int from = claimedTiers(a);
        int to = reachedTiers(a);
        int coins = 0;
        int tickets = 0;
        Map<ResourceKinds, Integer> sum = new LinkedHashMap<>();
        for (int i = from; i < to; i++) {
            Reward r = a.tiers().get(i).reward();
            coins += r.coins();
            tickets += r.skipTickets();
            r.resources().forEach((k, v) -> sum.merge(k, v, Integer::sum));
        }
        Reward total = new Reward(coins, sum, tickets);
        StarRewardService.grant(total);
        store().put(CLAIMED_PREFIX + a.id(), to);
        return total;
    }

    /** 表示用の値(divisor で割る) */
    public static int shown(Achievement a, int raw) {
        return raw / a.divisor();
    }
}
