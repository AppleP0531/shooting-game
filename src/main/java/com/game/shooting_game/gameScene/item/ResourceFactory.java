package com.game.shooting_game.gameScene.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.item.types.Resource;
import com.game.shooting_game.utils.Vector2;

public class ResourceFactory {
    public enum DropModel {
        ENEMY_DEFEAT
    }

    // リソースのドロップテーブルの定義
    private static final Map<DropModel, Map<Rarity, Double>> RESOURCE_DROP_TABLE = new LinkedHashMap<>();
    static {
        Map<Rarity, Double> enemyDefeatTable = new LinkedHashMap<>();
        enemyDefeatTable.put(null, 91.4);
        enemyDefeatTable.put(Rarity.COMMON, 5.0);
        enemyDefeatTable.put(Rarity.UNCOMMON, 2.0);
        enemyDefeatTable.put(Rarity.RARE, 0.5);
        enemyDefeatTable.put(Rarity.EPIC, 0.1);
        enemyDefeatTable.put(Rarity.LEGENDARY, 0.05);
        RESOURCE_DROP_TABLE.put(DropModel.ENEMY_DEFEAT, enemyDefeatTable);
    }

    /**
     * リソースの作成(種類ランダム)
     * 
     * @param model ドロップモデル
     * @param pos   生成位置
     * @return リソース
     */
    public static Resource createResource(DropModel model, Vector2 pos) {
        Rarity rarity = lottery(model);
        if (rarity == null)
            return null;

        ResourceName[] resNames = ResourceName.values();
        Random random = new Random();
        int randomIndex = random.nextInt(resNames.length);
        ResourceName resName = resNames[randomIndex];

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
     * 確率に基づくドロップするアイテム種の決定
     *
     * @param model 使用モデル
     * @return アイテム種
     */
    private static Rarity lottery(DropModel model) {
        double totalWeight = RESOURCE_DROP_TABLE.get(model).values().stream().mapToDouble(Double::doubleValue).sum();
        Random random = new Random();
        double randomValue = random.nextDouble() * totalWeight;

        double cumulativeWeight = 0.0;
        for (Map.Entry<Rarity, Double> entry : RESOURCE_DROP_TABLE.get(model).entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue <= cumulativeWeight) {
                return entry.getKey();
            }
        }
        return null; // 万が一のため
    }
}
