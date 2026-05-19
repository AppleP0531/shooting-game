package com.game.shooting_game.gameScene.item;

import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.item.types.*;
import com.game.shooting_game.interfaces.Collectible;

import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds;

import java.util.LinkedHashMap;

public class ItemFactory {
    private static final Random random = new Random();

    // ドロップテーブルの定義
    private static final Map<Collectible, Integer> DROP_TABLE = new LinkedHashMap<>();
    static {
        DROP_TABLE.put(null, 90); // ドロップ無し
        DROP_TABLE.put(ItemKinds.HEAL, 2); // 回復のドロップ率
        DROP_TABLE.put(ItemKinds.LEVEL_UP, 5); // レベルアップのドロップ率
        DROP_TABLE.put(ItemKinds.SHIELD, 3); // シールドのドロップ率
    }

    /**
     * アイテムの作成
     * 
     * @param pos 生成座標
     * @return アイテム
     */
    public static Item createItem(Vector2 pos) {
        Collectible kind = lottery();

        // ドロップするアイテムの種類を渡す(nullの場合はドロップ無し)
        return switch (kind) {
            case ItemKinds.HEAL -> new Heal(pos);
            case ItemKinds.LEVEL_UP -> new LevelUp(pos);
            case ItemKinds.SHIELD -> new Shield(pos);
            case null -> null;
            default -> null;
        };
    }

    /**
     * アイテムの作成(種類指定)
     * 
     * @param pos  生成位置
     * @param kind アイテム種
     * @return アイテム
     */
    public static Item createItem(Vector2 pos, Collectible kind) {
        return switch (kind) {
            case ItemKinds.HEAL -> new Heal(pos);
            case ItemKinds.LEVEL_UP -> new LevelUp(pos);
            case ItemKinds.SHIELD -> new Shield(pos);
            case ResourceKinds.COIN -> new Coin(pos);
            default -> null;
        };
    }

    /**
     * 確率に基づくドロップするアイテム種の決定
     * 
     * @return アイテム種
     */
    private static Collectible lottery() {
        int totalWeight = DROP_TABLE.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);

        int cumulativeWeight = 0;
        for (Map.Entry<Collectible, Integer> entry : DROP_TABLE.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue < cumulativeWeight) {
                return entry.getKey();
            }
        }
        return null;
    }
}