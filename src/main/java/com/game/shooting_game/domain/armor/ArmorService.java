package com.game.shooting_game.domain.armor;

import java.util.LinkedHashMap;
import java.util.Map;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * 装甲(防御装甲・火力装甲)
 * 機体に1つずつ付く装備で、コインと強化素材でレベルを上げる(0〜MAX_LEVEL。0は未強化で効果なし)。★はレベル STAR_STEP ごとに1つ。
 * 効果はレベルに比例して、モジュールの能力(ModuleStat)と同じ種類で足される。ModuleService.stat がモジュールと合算する。
 * レベルは Progress.highScores に接頭辞つき(armor:&lt;種類&gt;)で保存する(機体ごとではなく共通)。
 */
public final class ArmorService {
    private static final String component = "[ArmorService]";
    private static final String KEY_PREFIX = "armor:";

    public static final int MAX_LEVEL = 20;
    public static final int STAR_STEP = 4;
    public static final int MAX_STARS = MAX_LEVEL / STAR_STEP;

    /**
     * 装甲の種類と、レベル1つあたりの効果
     */
    public enum Type {
        DEFENSE("防御装甲", "体力・防御・シールドが上がる",
                Map.of(ModuleStat.HP_PCT, 0.04, ModuleStat.DEF_PCT, 0.01, ModuleStat.SHIELD_PCT, 0.04)),
        FIREPOWER("火力装甲", "攻撃力・会心が上がる",
                Map.of(ModuleStat.ATK_PCT, 0.05, ModuleStat.CRIT_RATE, 0.005, ModuleStat.CRIT_DMG, 0.02));

        private final String title;
        private final String detail;
        private final Map<ModuleStat, Double> perLevel;

        Type(String title, String detail, Map<ModuleStat, Double> perLevel) {
            this.title = title;
            this.detail = detail;
            // 表示の順を、能力の宣言順にそろえる
            Map<ModuleStat, Double> ordered = new LinkedHashMap<>();
            for (ModuleStat s : ModuleStat.values())
                if (perLevel.containsKey(s))
                    ordered.put(s, perLevel.get(s));
            this.perLevel = ordered;
        }

        public String title() {
            return title;
        }

        public String detail() {
            return detail;
        }

        /** レベル1つあたりの効果(能力 → 値) */
        public Map<ModuleStat, Double> perLevel() {
            return perLevel;
        }
    }

    private ArmorService() {
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    private static String key(Type type) {
        return KEY_PREFIX + type.name();
    }

    /** 保存してあるレベル(0〜MAX_LEVEL。未保存は0=未強化) */
    public static int level(Type type) {
        Map<String, Integer> s = store();
        return clampLevel(s == null ? 0 : s.getOrDefault(key(type), 0));
    }

    static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    /** レベルに応じた★の数(0〜MAX_STARS) */
    public static int stars(int level) {
        return clampLevel(level) / STAR_STEP;
    }

    public static int stars(Type type) {
        return stars(level(type));
    }

    /** 指定したレベルでの、ある能力の効果(その装甲にない能力は0) */
    public static double value(Type type, ModuleStat stat, int level) {
        return type.perLevel().getOrDefault(stat, 0.0) * clampLevel(level);
    }

    /** 今装着している装甲(全種類)の、ある能力の合計。ModuleService.stat から呼ばれる */
    public static double stat(ModuleStat stat) {
        if (store() == null) // データがないとき(単体テストなど)は効果なし
            return 0;
        double sum = 0;
        for (Type t : Type.values())
            sum += value(t, stat, level(t));
        return sum;
    }

    /** 今のレベルから次のレベルへ上げるコスト(機体のレベルのコストを、2倍のレベル相当で使う) */
    public static Cost levelCost(int level) {
        return UnitService.levelCost(Math.max(1, Math.min(UnitService.MAX_LEVEL, clampLevel(level) * 2)));
    }

    public static boolean levelUp(Type type) {
        UserData ud = DataManager.getUserData();
        Map<String, Integer> s = store();
        int lv = level(type);
        if (ud == null || s == null || lv >= MAX_LEVEL)
            return false;
        Cost cost = levelCost(lv);
        if (!CostService.canAfford(cost))
            return false;
        CostService.pay(ud, cost);
        s.put(key(type), lv + 1);
        LogUtils.info(component, ANSI.YELLOW + type.title() + ANSI.RESET + " reached Lv" + (lv + 1) + ".");
        return true;
    }

    /** レベルを直接設定する(コストは見ない。管理用)。範囲に収めて、設定後のレベルを返す(保存先がなければ -1) */
    public static int setLevel(Type type, int level) {
        Map<String, Integer> s = store();
        if (s == null)
            return -1;
        s.put(key(type), clampLevel(level));
        return clampLevel(level);
    }
}
