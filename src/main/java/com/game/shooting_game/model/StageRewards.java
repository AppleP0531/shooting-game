package com.game.shooting_game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Data;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.domain.module.Module;

@Data
public class StageRewards {
    private static StageRewards instance;

    private int score; // ステージクリアスコア
    private Map<ResourceKinds, Integer> resources = new HashMap<>(); // ステージクリア報酬
    private String difficultyLabel = ""; // 遊んだ難易度の表示(ノーマルは空)
    private List<String> newMissions = new ArrayList<>(); // 今回新しく達成したミッション
    private List<Module> modules = new ArrayList<>(); // クリアで手に入ったモジュール

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
        modules.clear();
        score = 0;
        difficultyLabel = "";
        newMissions.clear();
    }
}
