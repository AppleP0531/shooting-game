package com.game.shooting_game.utils;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ゲームバランスの調整値(BalanceConfig.json)
 * <p>
 * 自機の火力は、機体レベルで指数的に伸び(最大Lvで約100倍)、敵のHPはステージが進むごとに指数的に増える。
 * 数字だけを直したいときは、ここ(JSON)を変える。読めなければ既定値。
 */
public final class GameBalance {
    private static final String component = "[GameBalance]";
    private static final String CONFIG_PATH = "/static/configs/BalanceConfig.json";

    private static double unitAttackGrowth = 1.10;
    private static double attackPerStar = 0.25;
    private static double stageHpGrowth = 1.15;
    private static double endlessHpPerWave = 0.015;
    private static boolean loaded = false;

    private GameBalance() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded)
            return;
        loaded = true;
        try (InputStream is = GameBalance.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            JsonNode n = new ObjectMapper().readTree(is);
            unitAttackGrowth = Math.max(1.0, n.path("unitAttackGrowth").asDouble(unitAttackGrowth));
            attackPerStar = Math.max(0.0, n.path("attackPerStar").asDouble(attackPerStar));
            stageHpGrowth = Math.max(1.0, n.path("stageHpGrowth").asDouble(stageHpGrowth));
            endlessHpPerWave = Math.max(0.0, n.path("endlessHpPerWave").asDouble(endlessHpPerWave));
        } catch (Exception e) {
            LogUtils.systemLog("[WARN]", component, "バランスの設定を読めないため既定値を使います: " + e.getMessage());
        }
    }

    /** 機体レベルが1つ上がるごとの火力の倍率(指数) */
    public static double unitAttackGrowth() {
        ensureLoaded();
        return unitAttackGrowth;
    }

    /** ★1つあたりの火力の増え方(足し算) */
    public static double attackPerStar() {
        ensureLoaded();
        return attackPerStar;
    }

    /** ステージが1つ進むごとの敵HPの倍率(指数) */
    public static double stageHpGrowth() {
        ensureLoaded();
        return stageHpGrowth;
    }

    /** 無限モードで、ウェーブごとに足す敵HPの割合(ステージ相当の指数に重ねる) */
    public static double endlessHpPerWave() {
        ensureLoaded();
        return endlessHpPerWave;
    }

    /**
     * ステージの番号に応じた敵HPの倍率(1から始まり、指数的に増える)
     *
     * @param stageIndex ステージの番号(1以上。小数も可)
     */
    public static double stageHpRate(double stageIndex) {
        return Math.pow(stageHpGrowth(), Math.max(0, stageIndex - 1));
    }
}
