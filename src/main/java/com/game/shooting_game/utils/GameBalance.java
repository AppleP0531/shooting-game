package com.game.shooting_game.utils;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ゲームバランスの調整値(BalanceConfig.json)
 * <p>
 * 自機の火力は、機体レベルとランクで指数的に伸び、敵のHPはステージが進むごとに指数的に増える。
 * 数字だけを直したいときは、ここ(JSON)を変える。読めなければ既定値。
 */
public final class GameBalance {
    private static final String component = "[GameBalance]";
    private static final String CONFIG_PATH = "/static/configs/BalanceConfig.json";

    private static double unitAttackGrowth = 1.06;
    private static double rankAttackGrowth = 18.0;
    private static double stageHpGrowth = 1.07;
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
            rankAttackGrowth = Math.max(1.0, n.path("rankAttackGrowth").asDouble(rankAttackGrowth));
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

    /** ランクが1つ上がるごとの火力の倍率(指数。レベルが1に戻っても、前のランクのLv50より強くなる値) */
    public static double rankAttackGrowth() {
        ensureLoaded();
        return rankAttackGrowth;
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
