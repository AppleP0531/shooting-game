package com.game.shooting_game.gameScene.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.item.types.Bomb;
import com.game.shooting_game.gameScene.item.types.Heal;
import com.game.shooting_game.gameScene.item.types.LevelUp;
import com.game.shooting_game.gameScene.item.types.Magnet;
import com.game.shooting_game.gameScene.item.types.Shield;
import com.game.shooting_game.utils.Vector2;

public class ItemFactory {
    // アイテムのドロップテーブルの定義
    private static final Map<ItemKinds, Double> ITEM_DROP_TABLE = new LinkedHashMap<>();
    static {
        ITEM_DROP_TABLE.put(null, 90.0); // ドロップ無し
        ITEM_DROP_TABLE.put(ItemKinds.HEAL, 2.0); // 回復のドロップ率
        ITEM_DROP_TABLE.put(ItemKinds.LEVEL_UP, 5.0); // レベルアップのドロップ率
        ITEM_DROP_TABLE.put(ItemKinds.SHIELD, 3.0); // シールドのドロップ率
        ITEM_DROP_TABLE.put(ItemKinds.BOMB, 1.2); // ボムのドロップ率
        ITEM_DROP_TABLE.put(ItemKinds.MAGNET, 1.5); // マグネットのドロップ率
    }

    /**
     * アイテムの作成(種類ランダム)
     * 
     * @param pos 生成座標
     * @return アイテム
     */
    public static Item createItem(Vector2 pos) {
        ItemKinds kind = lottery();

        // ドロップするアイテムの種類を渡す(nullの場合はドロップ無し)
        return switch (kind) {
            case ItemKinds.HEAL -> new Heal(pos);
            case ItemKinds.LEVEL_UP -> new LevelUp(pos);
            case ItemKinds.SHIELD -> new Shield(pos);
            case ItemKinds.BOMB -> new Bomb(pos);
            case ItemKinds.MAGNET -> new Magnet(pos);
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
    public static Item createItem(Vector2 pos, ItemKinds kind) {
        return switch (kind) {
            case HEAL -> new Heal(pos);
            case LEVEL_UP -> new LevelUp(pos);
            case SHIELD -> new Shield(pos);
            case BOMB -> new Bomb(pos);
            case MAGNET -> new Magnet(pos);
        };
    }

    /**
     * 確率に基づくドロップするアイテム種の決定
     * 
     * @return アイテム種
     */
    private static ItemKinds lottery() {
        double totalWeight = ITEM_DROP_TABLE.values().stream().mapToDouble(Double::doubleValue).sum();
        Random random = new Random();
        double randomValue = random.nextDouble() * totalWeight;

        double cumulativeWeight = 0;
        for (Map.Entry<ItemKinds, Double> entry : ITEM_DROP_TABLE.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue < cumulativeWeight) {
                return entry.getKey();
            }
        }
        return null;
    }
}