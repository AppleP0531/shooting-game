package com.game.shooting_game.gameScene.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Family;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.item.types.Resource;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.utils.Vector2;

public class ResourceFactory {
    public enum DropModel {
        ENEMY_DEFEAT, // 通常の敵撃破
        BOSS_BONUS // ボスのゲージ破壊ボーナス(レアが出やすい)
    }

    // レアリティの抽選テーブル(nullはドロップ無し)
    private static final Map<DropModel, Map<Rarity, Double>> RESOURCE_DROP_TABLE = new LinkedHashMap<>();
    static {
        Map<Rarity, Double> enemyDefeatTable = new LinkedHashMap<>();
        enemyDefeatTable.put(null, 82.0);
        enemyDefeatTable.put(Rarity.COMMON, 11.0);
        enemyDefeatTable.put(Rarity.UNCOMMON, 4.5);
        enemyDefeatTable.put(Rarity.RARE, 1.6);
        enemyDefeatTable.put(Rarity.EPIC, 0.7);
        enemyDefeatTable.put(Rarity.LEGENDARY, 0.2);
        RESOURCE_DROP_TABLE.put(DropModel.ENEMY_DEFEAT, enemyDefeatTable);

        Map<Rarity, Double> bossBonusTable = new LinkedHashMap<>();
        bossBonusTable.put(null, 20.0);
        bossBonusTable.put(Rarity.COMMON, 30.0);
        bossBonusTable.put(Rarity.UNCOMMON, 28.0);
        bossBonusTable.put(Rarity.RARE, 14.0);
        bossBonusTable.put(Rarity.EPIC, 6.0);
        bossBonusTable.put(Rarity.LEGENDARY, 2.0);
        RESOURCE_DROP_TABLE.put(DropModel.BOSS_BONUS, bossBonusTable);
    }

    // 素材ごとの出やすさ(スキルツリー・パーツの必要量にほぼ比例させている。強化素材とパーツ素材は別系統。どちらかのアイテムが他方に使われることはない)
    private static final Map<ResourceName, Double> NAME_WEIGHT = new LinkedHashMap<>();
    static {
        // 強化素材
        NAME_WEIGHT.put(ResourceName.GEAR, 2.2);
        NAME_WEIGHT.put(ResourceName.SCREW, 1.9);
        NAME_WEIGHT.put(ResourceName.ENERGY_CELL, 4.0);
        NAME_WEIGHT.put(ResourceName.CIRCUIT, 2.9);
        NAME_WEIGHT.put(ResourceName.LENS, 2.7);
        // パーツ素材
        NAME_WEIGHT.put(ResourceName.COPPER_PLATE, 2.0);
        NAME_WEIGHT.put(ResourceName.IRON_BAR, 2.5);
        NAME_WEIGHT.put(ResourceName.ALLOY_PLATE, 4.2);
        NAME_WEIGHT.put(ResourceName.CRYSTAL_SHARD, 2.4);
        NAME_WEIGHT.put(ResourceName.PLASMA_CORE, 1.0);
        // モジュール基板
        NAME_WEIGHT.put(ResourceName.MODULE_CHIP, 1.0);
    }

    private static final Random RANDOM = new Random();

    /**
     * リソースの作成(種類ランダム)
     * 
     * @param model ドロップモデル
     * @param pos   生成位置
     * @return リソース(ドロップ無しならnull)
     */
    public static Resource createResource(DropModel model, Vector2 pos) {
        Rarity rarity = lottery(model);
        if (rarity == null)
            return null;

        // 強化素材・パーツ素材・モジュール基板を 4:4:2 で抽選してから、その系統の中で種類を選ぶ
        double f = RANDOM.nextDouble();
        Family family = f < 0.4 ? Family.UPGRADE : f < 0.8 ? Family.PART : Family.MODULE;
        ResourceName resName = pickName(family);

        return new Resource(new ResourceKinds(resName, rarity), pos);
    }

    /**
     * リソースの作成(種類指定)
     * 
     * @param pos     生成位置
     * @param resKind アイテム種
     * @return リソース
     */
    public static Resource createResource(Vector2 pos, ResourceKinds resKind) {
        return new Resource(resKind, pos);
    }

    /**
     * 系統内で重み付き抽選
     */
    private static ResourceName pickName(Family family) {
        double total = 0;
        for (Map.Entry<ResourceName, Double> e : NAME_WEIGHT.entrySet())
            if (e.getKey().getFamily() == family)
                total += e.getValue();

        double value = RANDOM.nextDouble() * total;
        double cumulative = 0;
        ResourceName last = null;
        for (Map.Entry<ResourceName, Double> e : NAME_WEIGHT.entrySet()) {
            if (e.getKey().getFamily() != family)
                continue;
            last = e.getKey();
            cumulative += e.getValue();
            if (value <= cumulative)
                return last;
        }
        return last;
    }

    /**
     * 確率に基づくドロップするアイテム種の決定
     *
     * @param model 使用モデル
     * @return レアリティ(nullはドロップ無し)
     */
    private static Rarity lottery(DropModel model) {
        // スキルによるドロップ率上昇(ドロップ無しの重みは変えず、ドロップ側だけ増やす)
        double dropRate = 1 + SkillService.getBonus("DROP_RATE");

        Map<Rarity, Double> table = RESOURCE_DROP_TABLE.get(model);
        double totalWeight = 0;
        for (Map.Entry<Rarity, Double> e : table.entrySet())
            totalWeight += e.getKey() == null ? e.getValue() : e.getValue() * dropRate;

        double randomValue = RANDOM.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;
        for (Map.Entry<Rarity, Double> entry : table.entrySet()) {
            cumulativeWeight += entry.getKey() == null ? entry.getValue() : entry.getValue() * dropRate;
            if (randomValue <= cumulativeWeight) {
                return entry.getKey();
            }
        }
        return null;
    }
}
