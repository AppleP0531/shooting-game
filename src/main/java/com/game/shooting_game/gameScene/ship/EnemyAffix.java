package com.game.shooting_game.gameScene.ship;

import java.awt.Color;

import lombok.Getter;

/**
 * 敵に付くランダムなエフェクト(ナイトメア専用)
 * 
 * NIGHTMARE のステージでは、ボス以外の敵が出現するとき、一定の確率で1つだけ付く。
 * 機体のまわりに、種類ごとの色の輪が光って見分けられる。
 */
@Getter
public enum EnemyAffix {
    SWIFT("高速", new Color(90, 220, 255)), // 動きが速い
    ARMORED("装甲", new Color(190, 195, 210)), // HPが多い
    REGEN("再生", new Color(100, 230, 130)), // 少しずつ回復する
    RAPID("連射", new Color(255, 170, 70)); // 弾を撃つ間隔が短い

    /** エフェクトが付く確率 */
    public static final double CHANCE = 0.35;
    /** 高速: 速度の倍率 */
    public static final double SWIFT_SPEED_RATE = 1.4;
    /** 装甲: HPの倍率 */
    public static final double ARMORED_HP_RATE = 2.0;
    /** 再生: 1秒に回復する、最大HPに対する割合 */
    public static final double REGEN_RATE_PER_SEC = 0.03;
    /** 連射: 弾の間隔の倍率(小さいほど速く撃つ) */
    public static final double RAPID_COOLDOWN_RATE = 0.6;

    private final String label;
    private final Color color;

    EnemyAffix(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /**
     * エフェクトを決める(乱数は呼ぶ側が渡すので、結果を固定してテストできる)
     * 
     * @param chanceRoll 付けるかを決める乱数(0以上1未満)。CHANCE 未満なら付く
     * @param kindRoll   種類を選ぶ乱数(0以上1未満)
     * @return 付くエフェクト(付かなければnull)
     */
    public static EnemyAffix roll(double chanceRoll, double kindRoll) {
        if (chanceRoll >= CHANCE)
            return null;
        EnemyAffix[] all = values();
        int index = (int) Math.floor(Math.max(0, Math.min(0.999999, kindRoll)) * all.length);
        return all[index];
    }
}
