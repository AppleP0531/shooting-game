package com.game.shooting_game.domain.skill;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Family;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.upgradeScene.UpgradeManager.SkillTreeData;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * スキルツリーの強化状況の集計・コスト計算・購入処理
 * 強化にはコインと「強化素材」(Family.UPGRADE)を消費する。パーツ素材は使えない。
 */
public final class SkillService {
    private static final String component = "[SkillService]";
    private static final String CONFIG_PATH = "/static/configs/SkillTreeConfig.json";

    private static List<SkillTreeData> config;
    private static Map<String, SkillTreeData> byName;

    // 効果量のキャッシュ(機体か強化状況が変わったら作り直す)
    private static final Map<String, Double> bonusCache = new HashMap<>();
    private static String cacheShip = null;

    private SkillService() {
    }

    // ----- config -----
    public static synchronized List<SkillTreeData> getConfig() {
        if (config == null) {
            config = new ArrayList<>();
            byName = new HashMap<>();
            try (InputStream is = SkillService.class.getResourceAsStream(CONFIG_PATH)) {
                if (is == null)
                    throw new IOException("Resource not found: " + CONFIG_PATH);
                config = new ObjectMapper().readValue(is, new TypeReference<List<SkillTreeData>>() {
                });
                for (SkillTreeData d : config)
                    byName.put(d.getName(), d);
            } catch (IOException e) {
                LogUtils.error("[ERROR]", component,
                        "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET, e);
            }
        }
        return config;
    }

    public static SkillTreeData find(String name) {
        getConfig();
        return byName.get(name);
    }

    // ----- レベル -----
    private static Map<String, Integer> upgrades() {
        UserData ud = DataManager.getUserData();
        if (ud == null)
            return null;
        var stats = ud.getShips().get(ud.getSelectedShip());
        return stats == null ? null : stats.getUpgrades();
    }

    public static int getLevel(String name) {
        tickResearch(); // 研究が終わっていればレベルに反映する
        Map<String, Integer> up = upgrades();
        return up == null ? 0 : up.getOrDefault(name, 0);
    }

    /**
     * 解放されているか(根は常に解放、それ以外は親が必要レベルに達していること)
     */
    public static boolean isUnlocked(String name) {
        SkillTreeData d = find(name);
        if (d == null)
            return false;
        // 根と、根に直接つながる系統の起点は最初から解放されている
        if (d.getRootNode() == null || d.getName().equals("Origin") || d.getRootNode().equals("Origin"))
            return true;
        return getLevel(d.getRootNode()) >= d.getReqLevel();
    }

    /**
     * 効果量の合計(選択中の機体)
     * 
     * @param effect 効果名
     * @return 効果量。未強化なら0
     */
    public static synchronized double getBonus(String effect) {
        UserData ud = DataManager.getUserData();
        if (ud == null)
            return 0;
        if (!ud.getSelectedShip().equals(cacheShip)) {
            bonusCache.clear();
            cacheShip = ud.getSelectedShip();
        }
        return bonusCache.computeIfAbsent(effect, k -> {
            double sum = 0;
            for (SkillTreeData d : getConfig()) {
                if (k.equals(d.getEffect()))
                    sum += getLevel(d.getName()) * d.getPerLevel();
            }
            return sum;
        });
    }

    public static synchronized void invalidate() {
        bonusCache.clear();
    }

    // ----- コスト -----
    /**
     * 現在のレベルから次のレベルに上げるためのコスト
     * 素材は「どのレア度のものを何個」と決まっていて、ほかのレア度では払えない(変換所で5個→1個上のレア度に変換できる)
     */
    public static Cost getCost(SkillTreeData d, int level) {
        int coins = (int) (Math.round(d.getCoin() * Math.pow(d.getCoinGrowth(), level) / 10.0) * 10);
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        List<Map<String, Map<String, Integer>>> levels = d.getMatLevels();
        if (levels != null && !levels.isEmpty()) {
            Map<String, Map<String, Integer>> mats = levels.get(Math.min(level, levels.size() - 1));
            for (Map.Entry<String, Map<String, Integer>> e : mats.entrySet()) {
                ResourceName name = ResourceName.valueOf(e.getKey());
                // 強化素材以外はコストに指定できない
                if (name.getFamily() != Family.UPGRADE)
                    continue;
                for (Map.Entry<String, Integer> r : e.getValue().entrySet())
                    items.put(new ResourceKinds(name, Rarity.valueOf(r.getKey())), r.getValue());
            }
        }
        return new Cost(coins, items);
    }

    // ----- 研究時間 -----
    private static UserData.ShipStats stats() {
        UserData ud = DataManager.getUserData();
        return ud == null ? null : ud.getShips().get(ud.getSelectedShip());
    }

    /**
     * 現在のレベルから次のレベルに上げるための研究時間(秒)
     */
    public static long researchSeconds(SkillTreeData d, int level) {
        return Math.max(1, Math.round(d.getResearchSec() * Math.pow(d.getResearchGrowth(), level)));
    }

    /**
     * 研究中かどうか(選択中の機体。どのスキルでも1つだけ研究できる)
     */
    public static boolean isResearching() {
        tickResearch();
        UserData.ShipStats st = stats();
        return st != null && st.getResearchSkill() != null;
    }

    /**
     * 指定したスキルが研究中かどうか
     */
    public static boolean isResearching(String name) {
        UserData.ShipStats st = stats();
        return isResearching() && name.equals(st.getResearchSkill());
    }

    /**
     * 研究中のスキル名(研究していなければnull)
     */
    public static String researchingSkill() {
        return isResearching() ? stats().getResearchSkill() : null;
    }

    /**
     * 研究の残り時間(秒)。研究していなければ0
     */
    public static long researchRemaining() {
        if (!isResearching())
            return 0;
        long ms = stats().getResearchFinishAt() - System.currentTimeMillis();
        return Math.max(0, (ms + 999) / 1000);
    }

    /**
     * 研究スキップ券を1枚使って、研究中のスキルをすぐ完了させる
     *
     * @return 使えて完了したらtrue(研究中でない・券がないときはfalse。券は減らない)
     */
    public static synchronized boolean skipResearchWithTicket() {
        if (!isResearching())
            return false;
        if (!DataManager.isAdminMode() && !ShopService.consume(ItemKinds.SKIP_TICKET)) // 管理者モードは無制限
            return false;
        stats().setResearchFinishAt(System.currentTimeMillis());
        return tickResearch();
    }

    /**
     * 研究が終わっていればレベルを上げる
     *
     * @return このとき研究が完了したらtrue
     */
    public static synchronized boolean tickResearch() {
        UserData.ShipStats st = stats();
        if (st == null || st.getResearchSkill() == null || System.currentTimeMillis() < st.getResearchFinishAt())
            return false;

        String name = st.getResearchSkill();
        st.getUpgrades().merge(name, 1, Integer::sum);
        st.setResearchSkill(null);
        st.setResearchFinishAt(0);
        invalidate();

        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + name + ANSI.RESET + " research completed. Lv" + st.getUpgrades().get(name) + ".");
        return true;
    }

    /**
     * 時間の表示(1時間以上は h:mm:ss、それ未満は m:ss)
     */
    public static String formatTime(long sec) {
        long h = sec / 3600;
        long m = sec % 3600 / 60;
        long s = sec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    public static boolean isMaxLevel(SkillTreeData d) {
        return getLevel(d.getName()) >= d.getMaxLevel();
    }

    /**
     * 強化の実行
     * 
     * @return 強化できたらtrue
     */
    public static synchronized boolean purchase(String name) {
        SkillTreeData d = find(name);
        UserData ud = DataManager.getUserData();
        Map<String, Integer> up = upgrades();
        if (d == null || ud == null || up == null)
            return false;
        if (!isUnlocked(name) || isMaxLevel(d) || isResearching())
            return false;

        int level = getLevel(name);
        Cost cost = getCost(d, level);
        if (!CostService.canAfford(cost))
            return false;

        CostService.pay(ud, cost);

        // コストを払ったら研究開始。時間が経つとレベルが上がる
        UserData.ShipStats st = stats();
        long sec = researchSeconds(d, level);
        st.setResearchSkill(name);
        st.setResearchFinishAt(System.currentTimeMillis() + sec * 1000);

        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + name + ANSI.RESET + " research started (" + formatTime(sec) + ").");
        return true;
    }

    // ----- 表示 -----
    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9)
            return String.valueOf((long) Math.rint(v));
        return String.format("%.1f", v);
    }

    private static String fmtEffect(SkillTreeData d, double value) {
        if ("%".equals(d.getUnit()))
            return fmt(value * 100) + "%";
        return fmt(value) + d.getUnit();
    }

    /**
     * 詳細パネルに表示する文章
     */
    public static String describe(SkillTreeData d) {
        int level = getLevel(d.getName());
        StringBuilder sb = new StringBuilder();
        sb.append(d.getDetail()).append("\n\n");

        if (d.getEffect() != null && d.getPerLevel() != 0) {
            sb.append("効果: Lvごとに ").append(d.isReduce() ? "-" : "+").append(fmtEffect(d, d.getPerLevel())).append("\n");
            sb.append("現在: ").append(d.isReduce() ? "-" : "+").append(fmtEffect(d, d.getPerLevel() * level));
            if (level < d.getMaxLevel())
                sb.append(" → 次: ").append(d.isReduce() ? "-" : "+").append(fmtEffect(d, d.getPerLevel() * (level + 1)));
            sb.append("\n");
        }
        sb.append("Lv ").append(level).append(" / ").append(d.getMaxLevel()).append("\n\n");

        if (level >= d.getMaxLevel()) {
            sb.append("【最大レベル】");
            return sb.toString();
        }
        if (!isUnlocked(d.getName())) {
            SkillTreeData parent = find(d.getRootNode());
            sb.append("【未解放】親スキル「").append(parent != null ? parent.getTitle() : d.getRootNode())
                    .append("」をLv").append(d.getReqLevel()).append("にすると解放");
            return sb.toString();
        }

        sb.append("研究時間: ").append(formatTime(researchSeconds(d, level)));
        if (isResearching(d.getName()))
            sb.append("\n【研究中】残り ").append(formatTime(researchRemaining()));
        return sb.toString();
    }

    /**
     * コスト欄の下に出す一言(強化できるかどうか)
     */
    public static String status(SkillTreeData d) {
        int level = getLevel(d.getName());
        if (level >= d.getMaxLevel() || !isUnlocked(d.getName()))
            return "";
        if (isResearching()) {
            SkillTreeData r = find(researchingSkill());
            return "「" + (r != null ? r.getTitle() : researchingSkill()) + "」を研究中です(残り "
                    + formatTime(researchRemaining()) + ")";
        }
        if (CostService.canAfford(getCost(d, level)))
            return "強化できます(研究 " + formatTime(researchSeconds(d, level)) + ")";
        return "";
    }
}
