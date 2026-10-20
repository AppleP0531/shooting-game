package com.game.shooting_game.gameScene.stage;

import lombok.Getter;

/**
 * ステージの難易度
 *
 * 難しいほど敵が硬く強くなり、報酬(素材・コイン・モジュールの確率とレア度)が豪華になる。
 * NIGHTMARE は、そのステージの HARD ミッションをすべて達成すると解放される。
 */
@Getter
public enum Difficulty {
    EASY("EASY", "イージー", 0.8, 0.8, 0.8, 1.0, 0, 1.0, 1.0),
    NORMAL("NORMAL", "ノーマル", 1.0, 1.0, 1.0, 1.0, 0, 1.0, 1.0),
    HARD("HARD", "ハード", 1.8, 1.4, 1.6, 1.5, 0, 1.3, 1.2),
    NIGHTMARE("NIGHTMARE", "ナイトメア", 350.0, 2.0, 2.5, 2.0, 1, 2.8, 1.4);

    private final String label;
    private final String title;
    private final double enemyHpRate; // 敵のHP倍率
    private final double enemyDamageRate; // 敵から受けるダメージの倍率
    private final double rewardRate; // 素材・コインの倍率
    private final double moduleChanceRate; // モジュール報酬の確率の倍率
    private final int rarityBump; // モジュール報酬のレア度の上がり幅
    private final double bossHpRate; // ボスチャレンジだけ、HP倍率にさらにかける倍率
    private final double bossDamageRate; // ボスチャレンジだけ、受けるダメージ倍率にさらにかける倍率

    Difficulty(String label, String title, double enemyHpRate, double enemyDamageRate, double rewardRate,
            double moduleChanceRate, int rarityBump, double bossHpRate, double bossDamageRate) {
        this.label = label;
        this.title = title;
        this.enemyHpRate = enemyHpRate;
        this.enemyDamageRate = enemyDamageRate;
        this.rewardRate = rewardRate;
        this.moduleChanceRate = moduleChanceRate;
        this.rarityBump = rarityBump;
        this.bossHpRate = bossHpRate;
        this.bossDamageRate = bossDamageRate;
    }

    /**
     * 難易度の説明(表示用)
     */
    public String describe() {
        if (this == NORMAL)
            return "標準の強さ・標準の報酬";
        return "敵HP ×" + fmt(enemyHpRate) + " / 被ダメージ ×" + fmt(enemyDamageRate)
                + "  報酬 ×" + fmt(rewardRate)
                + (moduleChanceRate != 1.0 ? " / モジュール確率 ×" + fmt(moduleChanceRate) : "")
                + (rarityBump > 0 ? " / モジュールのレア度UP" : "");
    }

    /**
     * ボスチャレンジでの説明(表示用)。通常ステージの倍率に、ボス用の倍率が重なる。
     */
    public String describeBoss() {
        if (this == NORMAL)
            return describe();
        return "敵HP ×" + fmt(enemyHpRate * bossHpRate) + " / 被ダメージ ×" + fmt(enemyDamageRate * bossDamageRate)
                + "  報酬 ×" + fmt(rewardRate)
                + (moduleChanceRate != 1.0 ? " / モジュール確率 ×" + fmt(moduleChanceRate) : "")
                + (rarityBump > 0 ? " / モジュールのレア度UP" : "");
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
