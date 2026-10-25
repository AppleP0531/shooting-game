package com.game.shooting_game.domain.farm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;

/**
 * 素材周回ステージ(エクストラの「素材」分類)のルール計算
 * 
 * 素材ごとに1つ、その素材だけがたくさん出るステージを用意する。素材の価値(MATERIALS の並び順)が高いほど、
 * 解放に必要なクリア済みステージが先になり、敵も硬くなり、上のレア度も出る。
 * 一度でも手に入れた素材だけが一覧に出る(拾った素材は highScores に "got:素材名" で記録する。
 * 消費して0個になっても、記録が残るので一覧に残る)。
 */
public final class MaterialFarmService {
    private MaterialFarmService() {
    }

    public static final int STAGE_BASE = 1300; // 周回ステージの番号の起点(STAGE_BASE + 素材の並び順)
    private static final String GOT_PREFIX = "got:";
    private static final int FIRST_UNLOCK = 31; // いちばん価値の低い素材の解放(クリア済みステージ)
    private static final int UNLOCK_STEP = 14; // 価値が1つ上がるごとに先になる幅
    private static final int WAVES = 6; // 周回は短く終わるよう、ウェーブ数を絞る

    /** 周回できる素材(価値の低い順) */
    public static final List<ResourceName> MATERIALS = List.of(ResourceName.GEAR, ResourceName.SCREW,
            ResourceName.COPPER_PLATE, ResourceName.IRON_BAR, ResourceName.ENERGY_CELL, ResourceName.CIRCUIT,
            ResourceName.LENS, ResourceName.ALLOY_PLATE, ResourceName.CRYSTAL_SHARD, ResourceName.PLASMA_CORE);

    public static int wavesOf() {
        return WAVES;
    }

    /** その素材の周回ステージの番号 */
    public static int stageId(ResourceName res) {
        return STAGE_BASE + MATERIALS.indexOf(res);
    }

    /** 周回ステージの番号か */
    public static boolean isFarmStage(int stageId) {
        return stageId >= STAGE_BASE && stageId < STAGE_BASE + MATERIALS.size();
    }

    /** 周回ステージの番号から素材(周回ステージでなければ null) */
    public static ResourceName resourceOf(int stageId) {
        return isFarmStage(stageId) ? MATERIALS.get(stageId - STAGE_BASE) : null;
    }

    /** 解放に必要なクリア済みステージ(敵の硬さの基準にもなる) */
    public static int unlockStage(ResourceName res) {
        return FIRST_UNLOCK + MATERIALS.indexOf(res) * UNLOCK_STEP;
    }

    /** 周回ステージのクリア報酬(その素材だけ。価値が高いほど上のレア度も出る) */
    public static Map<ResourceName, Map<Rarity, String>> rewards(ResourceName res) {
        int tier = MATERIALS.indexOf(res);
        double k = 1.0 + tier * 0.15;
        Map<Rarity, String> m = new LinkedHashMap<>();
        m.put(Rarity.COMMON, range(14 * k, 22 * k));
        if (tier >= 2)
            m.put(Rarity.UNCOMMON, range(3 * k, 6 * k));
        if (tier >= 5)
            m.put(Rarity.RARE, range(1 * k, 2 * k));
        if (tier >= 8)
            m.put(Rarity.EPIC, range(0, 1 * k));
        Map<ResourceName, Map<Rarity, String>> r = new LinkedHashMap<>();
        r.put(res, m);
        return r;
    }

    private static String range(double lo, double hi) {
        int a = (int) Math.round(lo), b = Math.max(a, (int) Math.round(hi));
        return a + "~" + b;
    }

    // ----- 入手済みの記録 -----
    /** 素材を手に入れたことを記録する(周回ステージの一覧に出るようになる) */
    public static void markObtained(ResourceName res) {
        UserData u = DataManager.getUserData();
        if (u != null && MATERIALS.contains(res))
            u.getProgress().getHighScores().put(GOT_PREFIX + res.name(), 1);
    }

    /** 手に入れたことがあるか(古いセーブは、今持っていれば入手済みとみなす) */
    public static boolean obtained(ResourceName res) {
        UserData u = DataManager.getUserData();
        if (u == null)
            return false;
        if (u.getProgress().getHighScores().containsKey(GOT_PREFIX + res.name()))
            return true;
        for (Map.Entry<ResourceKinds, Integer> e : u.getResources().getEquipments().entrySet())
            if (e.getKey().getResName() == res && e.getValue() > 0)
                return true;
        return false;
    }
}
