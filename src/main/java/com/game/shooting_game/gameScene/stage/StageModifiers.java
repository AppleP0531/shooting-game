package com.game.shooting_game.gameScene.stage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.game.shooting_game.utils.GameBalance;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステージ条件(そのステージの間だけ効く、難しくなる・変わる条件)
 * 
 * - StageConfig の "modifiers" で指定する(ボスチャレンジなど)。
 * - デイリーチャレンジは、日付から決めた条件を同じ形でまとめて渡す。
 * - 無限モードは、ウェーブが進むほど敵のHPとダメージが上がる(setEndlessScale)。
 * ゲーム中(GAMEシーン)以外では効かない。ロビーのプレビューやテストに影響しないため。
 */
public final class StageModifiers {
    /**
     * 条件の指定
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Spec {
        private double enemyHpRate = 1.0; // 敵のHP倍率(ボスも同じ。ダメージを割って表す)
        private double enemyDamageRate = 1.0; // 敵から受けるダメージの倍率
        private double playerHpRate = 1.0; // 自機の最大HPの倍率
        private double coinRate = 1.0; // 拾うコインの倍率
        private boolean noHeal = false; // 回復アイテムが効かない
        private boolean noShield = false; // シールドが使えない
    }

    private static Spec current = new Spec();
    private static Difficulty difficulty = Difficulty.NORMAL;
    private static double endlessHp = 1.0;
    private static double endlessDamage = 1.0;
    private static boolean bossStage = false; // ボスチャレンジか(難易度のボス用倍率をかける)
    private static double stageHp = 1.0; // ステージが進むほど敵のHPが増える倍率(BalanceConfig.json の stageHpGrowth)

    private StageModifiers() {
    }

    /**
     * ステージの開始時に読み込む(nullなら条件なし)
     */
    public static void load(Spec spec) {
        load(spec, Difficulty.NORMAL);
    }

    /**
     * ステージの開始時に読み込む(難易度の倍率も、ステージ条件に重ねてかける)
     */
    public static void load(Spec spec, Difficulty diff) {
        load(spec, diff, 1);
    }

    /**
     * ステージの開始時に読み込む(ステージの番号に応じた敵HPの倍率も決める)
     *
     * @param stageIndex ステージの番号(ノーマルはステージ番号。エクストラは解放に要るステージ番号)
     */
    public static void load(Spec spec, Difficulty diff, int stageIndex) {
        load(spec, diff, stageIndex, false);
    }

    /**
     * ステージの開始時に読み込む(ボスチャレンジなら、難易度のボス用倍率も重ねる)
     */
    public static void load(Spec spec, Difficulty diff, int stageIndex, boolean boss) {
        bossStage = boss;
        stageHp = GameBalance.stageHpRate(stageIndex);
        current = spec == null ? new Spec() : spec;
        difficulty = diff == null ? Difficulty.NORMAL : diff;
        endlessHp = 1.0;
        endlessDamage = 1.0;
    }

    /**
     * 無限モードの強さの更新(ウェーブが進むほど強くなる)
     * 
     * @param wave 今のウェーブ番号
     */
    public static void setEndlessScale(int wave) {
        // 無限モードは、元にしているステージ(2ウェーブごとに1つ進む)の指数に、ウェーブごとの増加を重ねる
        endlessHp = GameBalance.stageHpRate(1 + (wave - 1) / 2.0) * (1 + GameBalance.endlessHpPerWave() * wave);
        endlessDamage = 1 + 0.01 * wave;
    }

    private static boolean active() {
        return SceneManager.getInstance().getSceneName() == SceneType.GAME;
    }

    public static double enemyHpRate() {
        return active() ? current.getEnemyHpRate() * difficulty.getEnemyHpRate() * (bossStage ? difficulty.getBossHpRate() : 1.0) * endlessHp * stageHp : 1.0;
    }

    public static double enemyDamageRate() {
        return active() ? current.getEnemyDamageRate() * difficulty.getEnemyDamageRate() * (bossStage ? difficulty.getBossDamageRate() : 1.0) * endlessDamage : 1.0;
    }

    public static double playerHpRate() {
        return active() ? current.getPlayerHpRate() : 1.0;
    }

    public static double coinRate() {
        return active() ? current.getCoinRate() : 1.0;
    }

    public static boolean noHeal() {
        return active() && current.isNoHeal();
    }

    public static boolean noShield() {
        return active() && current.isNoShield();
    }

    /**
     * 条件の説明(表示用。条件がなければ空)
     */
    public static String describe(Spec s) {
        return describe(s, true);
    }

    /**
     * 条件の説明(表示用)
     *
     * @param withEnemyRates 敵のHP・受けるダメージの倍率も出すか(難易度の表示と重なるときは出さない)
     */
    public static String describe(Spec s, boolean withEnemyRates) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        if (withEnemyRates && s.getEnemyHpRate() != 1.0)
            sb.append("・敵のHP ×").append(fmt(s.getEnemyHpRate())).append("\n");
        if (withEnemyRates && s.getEnemyDamageRate() != 1.0)
            sb.append("・敵から受けるダメージ ×").append(fmt(s.getEnemyDamageRate())).append("\n");
        if (s.getPlayerHpRate() != 1.0)
            sb.append("・自機の最大HP ×").append(fmt(s.getPlayerHpRate())).append("\n");
        if (s.getCoinRate() != 1.0)
            sb.append("・拾うコイン ×").append(fmt(s.getCoinRate())).append("\n");
        if (s.isNoHeal())
            sb.append("・回復アイテムが効かない\n");
        if (s.isNoShield())
            sb.append("・シールドが使えない\n");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
