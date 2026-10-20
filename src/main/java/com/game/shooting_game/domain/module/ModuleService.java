package com.game.shooting_game.domain.module;

import com.game.shooting_game.domain.cost.CostService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.module.Module.Sub;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * モジュールの制作・装着・能力の集計
 * 
 * - 装着できるのは4枠(同じ種類を重ねてもよい)。
 * - 制作にはコインと、敵がドロップするモジュール基板、そして種類ごとに決まった素材が要る(レシピ)。
 *   レア度ごとに「そのレア度の素材」が必要で、足りなければ変換所で上げられる。
 * - メイン能力は種類とレア度で決まる固定の割合。サブ能力は最初はなく、Lv5・10・15・20で1つずつ、
 *   メインより低い値でランダムに加わる。
 */
public final class ModuleService {
    private static final String component = "[ModuleService]";

    // レア度ごとのメイン能力の基準値(種類ごとの倍率をかける)
    private static final double[] MAIN_BASE = { 0.04, 0.07, 0.11, 0.16, 0.24 };
    // サブ能力が取る範囲(基準値に対する割合。必ずメインより低い)
    private static final double SUB_MIN = 0.25;
    private static final double SUB_MAX = 0.75;
    // サブ能力ごとの、レア度ごとの基準値と倍率(割合のものはMAIN_BASEにかける倍率、実数値のものはそのまま)
    private static final Map<ModuleStat, double[]> SUB_TABLE = new LinkedHashMap<>();
    static {
        SUB_TABLE.put(ModuleStat.ATK_PCT, scaled(1.0));
        SUB_TABLE.put(ModuleStat.ATK_FLAT, new double[] { 0.5, 1, 1.5, 2.5, 4 });
        SUB_TABLE.put(ModuleStat.DEF_PCT, scaled(0.6));
        SUB_TABLE.put(ModuleStat.DEF_FLAT, new double[] { 0.5, 1, 1.5, 2, 3 });
        SUB_TABLE.put(ModuleStat.SHIELD_PCT, scaled(2.5));
        SUB_TABLE.put(ModuleStat.SHIELD_FLAT, new double[] { 3, 6, 10, 16, 25 });
        SUB_TABLE.put(ModuleStat.REPAIR_PCT, scaled(2.5));
        SUB_TABLE.put(ModuleStat.HP_PCT, scaled(1.5));
        SUB_TABLE.put(ModuleStat.HP_FLAT, new double[] { 5, 10, 18, 30, 50 });
        SUB_TABLE.put(ModuleStat.HEAL_PCT, scaled(2.0));
        SUB_TABLE.put(ModuleStat.HEAL_FLAT, new double[] { 1, 2, 4, 7, 12 });
    }

    // 制作のコスト(レア度ごと)。作り直しを前提にしているので、重めにしてある
    private static final int[] COINS = { 3000, 10000, 30000, 80000, 200000 };
    private static final int[] CHIPS = { 5, 6, 8, 10, 12 };
    private static final int[] SUBS = { 3, 4, 5, 6, 8 };

    // レベル。SUB_STEPごとのレベルに達するたびに、サブ能力が1つ加わる(最大SUB_COUNT個)
    public static final int MAX_LEVEL = 20;
    public static final int SUB_STEP = 5;
    public static final int SUB_COUNT = MAX_LEVEL / SUB_STEP;

    private ModuleService() {
    }

    private static double[] scaled(double k) {
        double[] r = new double[MAIN_BASE.length];
        for (int i = 0; i < r.length; i++)
            r[i] = MAIN_BASE[i] * k;
        return r;
    }

    private static ModuleInventory inv() {
        UserData ud = DataManager.getUserData();
        return ud == null ? new ModuleInventory() : ud.getModules();
    }

    // ----- 参照 -----
    public static List<Module> owned() {
        return inv().getOwned();
    }

    public static Module find(long id) {
        for (Module m : inv().getOwned())
            if (m.getId() == id)
                return m;
        return null;
    }

    /**
     * 装着中のモジュール(空き枠はnull)
     */
    public static Module[] slotted() {
        ModuleInventory in = inv();
        Module[] r = new Module[ModuleInventory.SLOTS];
        for (int i = 0; i < r.length && i < in.getSlots().size(); i++)
            r[i] = find(in.getSlots().get(i));
        return r;
    }

    public static boolean isEquipped(Module m) {
        for (long id : inv().getSlots())
            if (id == m.getId())
                return true;
        return false;
    }

    /**
     * 装着中のモジュールの、ある能力の合計
     */
    public static double stat(ModuleStat stat) {
        double sum = 0;
        for (Module m : slotted()) {
            if (m == null)
                continue;
            if (m.getKind().getMainStat() == stat)
                sum += mainNow(m);
            for (Sub s : m.getSubs())
                if (s.getStat() == stat)
                    sum += s.getValue();
        }
        return sum;
    }

    /**
     * 今のレベルでのメイン能力の値(Lv最大のときが、保存してある値。レベルに比例して上がる)
     */
    public static double mainNow(Module m) {
        int lv = Math.max(1, Math.min(m.getLevel(), MAX_LEVEL));
        return m.getMainValue() * lv / MAX_LEVEL;
    }

    // ----- 装着 -----
    public static boolean equip(long id) {
        ModuleInventory in = inv();
        if (find(id) == null || isEquipped(find(id)))
            return false;
        while (in.getSlots().size() < ModuleInventory.SLOTS)
            in.getSlots().add(0L);
        for (int i = 0; i < ModuleInventory.SLOTS; i++) {
            if (in.getSlots().get(i) == 0L || find(in.getSlots().get(i)) == null) {
                in.getSlots().set(i, id);
                DataManager.save();
                return true;
            }
        }
        return false; // 空き枠なし
    }

    public static void unequip(long id) {
        ModuleInventory in = inv();
        for (int i = 0; i < in.getSlots().size(); i++)
            if (in.getSlots().get(i) == id)
                in.getSlots().set(i, 0L);
        DataManager.save();
    }

    // ----- 制作 -----
    public static double mainValue(ModuleKind kind, Rarity rarity) {
        return MAIN_BASE[rarity.ordinal()] * kind.getMainScale();
    }

    /**
     * レシピ(コイン + 基板 + 種類ごとの素材。すべて指定のレア度のもの)
     */
    public static Cost recipe(ModuleKind kind, Rarity rarity) {
        int r = rarity.ordinal();
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        items.put(new ResourceKinds(ResourceName.MODULE_CHIP, rarity), CHIPS[r]);
        items.put(new ResourceKinds(kind.getSubMaterial(), rarity), SUBS[r]);
        return new Cost(COINS[r], items);
    }

    /**
     * 制作(サブ能力はここでランダムに決まる)
     * 
     * @return できたモジュール(材料が足りなければnull)
     */
    public static Module craft(ModuleKind kind, Rarity rarity) {
        UserData ud = DataManager.getUserData();
        Cost cost = recipe(kind, rarity);
        if (ud == null || !CostService.canAfford(cost))
            return null;
        CostService.pay(ud, cost);

        Module m = addNew(kind, rarity);
        LogUtils.systemLog("[INFO]", component,
                "Crafted " + ANSI.YELLOW + kind.getId() + ANSI.RESET + " (" + rarity + ").");
        return m;
    }

    /**
     * モジュールを1つ作って所持品に加える(ステージ報酬でも使う)
     * 
     * @param kind   種類(nullならランダム)
     * @param rarity レア度
     * @return できたモジュール
     */
    public static Module addNew(ModuleKind kind, Rarity rarity) {
        UserData ud = DataManager.getUserData();
        if (kind == null) {
            ModuleKind[] all = ModuleKind.values();
            kind = all[ThreadLocalRandom.current().nextInt(all.length)];
        }
        Module m = roll(kind, rarity);
        ModuleInventory in = ud.getModules();
        m.setId(in.getNextId());
        in.setNextId(in.getNextId() + 1);
        in.getOwned().add(m);
        DataManager.save();
        return m;
    }

    /**
     * 分解したときに戻る基板の数(制作に使う数の3分の1)
     */
    public static int dismantleRefund(Rarity rarity) {
        return Math.max(1, CHIPS[rarity.ordinal()] / 3);
    }

    /**
     * 新しいモジュールを作る(メインは固定。サブ能力はまだなく、レベルが上がると加わる)
     */
    public static Module roll(ModuleKind kind, Rarity rarity) {
        Module m = new Module();
        m.setKind(kind);
        m.setRarity(rarity);
        m.setMainValue(mainValue(kind, rarity));
        return m;
    }

    /**
     * サブ能力を1つ、ランダムに加える(メインや、すでにあるサブと同じ能力は出ない)
     */
    private static void addRandomSub(Module m) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<ModuleStat> pool = new ArrayList<>(SUB_TABLE.keySet());
        pool.remove(m.getKind().getMainStat());
        for (Sub s : m.getSubs())
            pool.remove(s.getStat());
        if (pool.isEmpty())
            return;

        ModuleStat st = pool.get(rnd.nextInt(pool.size()));
        double base = SUB_TABLE.get(st)[m.getRarity().ordinal()];
        double v = base * rnd.nextDouble(SUB_MIN, SUB_MAX);
        // メインより必ず低くする(割合は基準値の範囲内なので自然に低い。念のため)
        if (st.isPercent())
            v = Math.min(v, m.getMainValue() * 0.95);
        m.getSubs().add(new Sub(st, Math.round(v * 1000) / 1000.0));
    }

    // ----- レベルアップ -----
    /**
     * 次のレベルに上げるコスト(コインとそのレア度の基板)。最大レベルならnull
     */
    public static Cost levelUpCost(Module m) {
        if (m.getLevel() >= MAX_LEVEL)
            return null;
        int r = m.getRarity().ordinal();
        int coins = COINS[r] / 10 * m.getLevel();
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        items.put(new ResourceKinds(ResourceName.MODULE_CHIP, m.getRarity()), Math.max(1, CHIPS[r] / 2));
        return new Cost(coins, items);
    }

    /**
     * レベルアップ(Lv5・10・15・20に達したらサブ能力が1つ加わる)
     *
     * @return 上げられたらtrue
     */
    public static boolean levelUp(long id) {
        UserData ud = DataManager.getUserData();
        Module m = find(id);
        Cost cost = m == null ? null : levelUpCost(m);
        if (ud == null || cost == null || !CostService.canAfford(cost))
            return false;
        CostService.pay(ud, cost);

        m.setLevel(m.getLevel() + 1);
        // 以前の仕様で最初から4つ持っているモジュールは、サブ能力が増えない
        if (m.getLevel() % SUB_STEP == 0 && m.getSubs().size() < m.getLevel() / SUB_STEP)
            addRandomSub(m);
        DataManager.save();
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + m.getKind().getId() + ANSI.RESET + " reached Lv" + m.getLevel() + ".");
        return true;
    }

    /**
     * 分解(基板が戻る。装着中は分解できない)
     */
    public static boolean dismantle(long id) {
        UserData ud = DataManager.getUserData();
        Module m = find(id);
        if (ud == null || m == null || isEquipped(m))
            return false;
        ud.getModules().getOwned().remove(m);
        ResourceKinds chip = new ResourceKinds(ResourceName.MODULE_CHIP, m.getRarity());
        Map<ResourceKinds, Integer> eq = ud.getResources().getEquipments();
        eq.put(chip, eq.getOrDefault(chip, 0) + dismantleRefund(m.getRarity()));
        DataManager.save();
        return true;
    }

    // ----- 表示 -----
    public static String title(Module m) {
        return m.getKind().getTitle() + "  " + rarityName(m.getRarity());
    }

    public static String rarityName(Rarity r) {
        return switch (r) {
            case COMMON -> "コモン";
            case UNCOMMON -> "アンコモン";
            case RARE -> "レア";
            case EPIC -> "エピック";
            case LEGENDARY -> "レジェンド";
        };
    }

    public static String describe(Module m) {
        StringBuilder sb = new StringBuilder();
        sb.append(title(m)).append("   Lv ").append(m.getLevel()).append(" / ").append(MAX_LEVEL).append("\n");
        ModuleStat main = m.getKind().getMainStat();
        sb.append("メイン  ").append(main.getLabel()).append(" ").append(main.format(mainNow(m)));
        if (m.getLevel() < MAX_LEVEL)
            sb.append("  (Lv").append(MAX_LEVEL).append("で ").append(main.format(m.getMainValue())).append(")");
        sb.append("\n");
        for (Sub s : m.getSubs())
            sb.append("サブ    ").append(s.getStat().getLabel()).append(" ").append(s.getStat().format(s.getValue()))
                    .append("\n");
        // まだ解放されていないサブ能力は、解放されるレベルを出す
        for (int lv = (m.getSubs().size() + 1) * SUB_STEP; lv <= MAX_LEVEL; lv += SUB_STEP)
            sb.append("サブ    ??? (Lv").append(lv).append("で解放)\n");
        return sb.toString();
    }
}
