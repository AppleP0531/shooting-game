package com.game.shooting_game.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.domain.module.ModuleInventory;

/**
 * ユーザーデータ
 */
@Data // Getter, Setterの自動生成
@JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
@JsonPropertyOrder({ "lastPlayTime", "totalPlayTime", "selectedShip", "subShip", "progress", "resources", "ships" }) // 並び順の指定
public class UserData {
    private static final String component = "[UserData]";

    private String username = "";
    private String lastPlayTime;
    private String totalPlayTime = "00:00:00";
    private String selectedShip = "Ship1";
    private String subShip; // サブ機を装備しているか(装備中はnull以外)
    private Map<String, Integer> subShipParts = new LinkedHashMap<>(); // サブ機パーツの制作状況(パーツ名→Mk)
    private ModuleInventory modules = new ModuleInventory(); // 所有・装着中のモジュール
    private Progress progress = new Progress();
    private Resources resources = new Resources();
    private Map<String, ShipStats> ships = new LinkedHashMap<>();

    public UserData() {
        unlockShip("Ship1");
    }

    /**
     * 進捗
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class Progress {
        private int exp = 0; // 獲得経験値の総量
        private int clearedStage = 0; // ステージクリア状況
        private Map<String, Integer> highScores = new HashMap<>(); // ステージごとの最高スコア

        // 最高スコアの更新と保存
        public void updateHighScore(String stageKey, int score) {
            if (score > highScores.getOrDefault(stageKey, 0)) {
                highScores.put(stageKey, score);
            }
        }

        // スコアの加算
        public void addExp(int amount) {
            exp += amount;
        }
    }

    /**
     * 所有資源
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class Resources {
        private int coins = 0; // コイン
        private Map<ResourceKinds, Integer> equipments = new HashMap<>(); // 資源
    }

    // 船の情報
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class ShipStats {
        private Map<String, Integer> upgrades = new HashMap<>(); // 強化状況
        private int level = 1; // 機体レベル(1〜50。火力と耐久力の基礎値が上がる)
        private int stars = 0; // ★(0〜5。同じ機体を消費して上げる)
        private int copies = 0; // 持っている予備の同一機体の数(★を上げるのに消費)
        private String researchSkill; // 研究中のスキル名(研究していなければnull)
        private long researchFinishAt; // 研究が終わる時刻(エポックミリ秒)
    }

    // 新しい船のアンロック
    public void unlockShip(String shipId) {
        // まだ所有していない場合は追加
        if (!ships.containsKey(shipId)) {
            ships.put(shipId, new ShipStats());
            LogUtils.systemLog(
                    "[INFO]",
                    component,
                    shipId + " unlocked.");
        }
    }
}
