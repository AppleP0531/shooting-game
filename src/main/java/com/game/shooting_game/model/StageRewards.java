package com.game.shooting_game.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Data;

import com.game.shooting_game.ResourceManager.ResourceKinds;

@Data
public class StageRewards {
    private static StageRewards instance;

    private int score; // ステージクリアスコア
    private Map<ResourceKinds, Integer> resources = new LinkedHashMap<>(); // ステージクリア報酬

    /**
     * インスタンスの取得
     * 
     * @return インスタンス
     */
    public static StageRewards getInstance() {
        // 初回呼び出され時は初期化してから返す
        return instance = Objects.requireNonNullElseGet(instance, StageRewards::new);
    }

    /**
     * キャッシュリセット
     */
    public void resetRewards() {
        resources.clear();
        score = 0;
    }
}
