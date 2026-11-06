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

    private static double unitAttackGrowth = 1.028;
    private static double rankAttackGrowth = 4.0;
    private static double stageHpGrowth = 1.034;
    private static double endlessHpPerWave = 0.015;
    private static double bossHpRate = 5.0;
    private static double enemyLevelHp = 0.2;
    private static double stageDamageGrowth = 1.004;
    private static double requiredBase = 100.0;
    private static double requiredStageGrowth = 1.00275;
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
            bossHpRate = Math.max(1.0, n.path("bossHpRate").asDouble(bossHpRate));
            enemyLevelHp = Math.max(0.0, n.path("enemyLevelHp").asDouble(enemyLevelHp));
            stageDamageGrowth = Math.max(1.0, n.path("stageDamageGrowth").asDouble(stageDamageGrowth));
            requiredBase = Math.max(0.0, n.path("requiredBase").asDouble(requiredBase));
            requiredStageGrowth = Math.max(1.0, n.path("requiredStageGrowth").asDouble(requiredStageGrowth));
        } catch (Exception e) {
            LogUtils.warn(component, "バランスの設定を読めないため既定値を使います: " + e.getMessage());
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

    /** ステージ1・ノーマルでの推定必要戦力値(戦力値の基準。ここを動かすと、全ステージの必要戦力値が比例して動く) */
    public static double requiredBase() {
        ensureLoaded();
        return requiredBase;
    }

    /**
     * 推定必要戦力値の、ステージが1つ進むごとの上乗せ(指数)。敵のHPとダメージの伸びだけでは、
     * 実際に要る自機の強さに足りなかったので、ステージ200・ナイトメアが約3000万になるよう合わせた値
     */
    public static double requiredStageGrowth() {
        ensureLoaded();
        return requiredStageGrowth;
    }

    /** 敵のレベルが1つ上がるごとに増える敵HPの割合(レベル2なら 1 + 2×この値 倍) */
    public static double enemyLevelHp() {
        ensureLoaded();
        return enemyLevelHp;
    }

    /** ステージが1つ進むごとの、敵から受けるダメージの倍率(指数。HPより緩やか) */
    public static double stageDamageGrowth() {
        ensureLoaded();
        return stageDamageGrowth;
    }

    /**
     * 敵のレベルに応じた敵HPの倍率(レベル0で1、レベルごとに enemyLevelHp ずつ増える)
     */
    public static double enemyLevelHpRate(int level) {
        return 1 + enemyLevelHp() * Math.max(0, level);
    }

    /**
     * ステージの番号に応じた、敵から受けるダメージの倍率(1から始まり、指数的に増える)
     */
    public static double stageDamageRate(double stageIndex) {
        return Math.pow(stageDamageGrowth(), Math.max(0, stageIndex - 1));
    }

    /** ボスのゲージHP(BossConfig の gaugeHp)にかける倍率。コインボスには掛けない */
    public static double bossHpRate() {
        ensureLoaded();
        return bossHpRate;
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
