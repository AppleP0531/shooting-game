package com.game.shooting_game.gameScene.status;

import java.awt.Color;

import lombok.Getter;

/**
 * 特殊効果(状態異常)の種類
 * 自機・敵のどちらにもかかる。ルールの数値はここに集める(StatusSet が読む)。
 */
@Getter
public enum StatusEffect {
    // 名前, 色, 標準の時間(秒), 毎秒の継続ダメージ(最大HPに対する割合), 動く速さの倍率, 回復できないか
    POISON("毒", new Color(150, 90, 220), 6.0, 0.02, 1.0, true), // 長く続く弱い継続ダメージ。回復できない
    BURN("炎", new Color(255, 120, 40), 3.0, 0.05, 1.0, false), // 短く強い継続ダメージ
    SLOW("鈍足", new Color(120, 200, 255), 3.0, 0.0, 0.5, false), // 動き・弾の間隔が半分の速さ
    SHOCK("感電", new Color(255, 235, 90), 0.8, 0.0, 0.0, false), // 一瞬、動けない
    VULNERABLE("脆弱", new Color(230, 90, 160), 5.0, 0.0, 1.0, false); // 受けるダメージが増える(VULNERABLE_RATE)

    private final String displayName;
    private final Color color;
    private final double defaultSeconds;
    private final double tickRate; // 毎秒の継続ダメージ(最大HPに対する割合)
    private final double speedRate; // かかっている間の、動く速さの倍率(0なら止まる)
    private final boolean blocksHeal;

    /** 脆弱の間に受けるダメージの倍率(強さ1のとき) */
    public static final double VULNERABLE_RATE = 1.25;

    StatusEffect(String displayName, Color color, double defaultSeconds, double tickRate, double speedRate,
            boolean blocksHeal) {
        this.displayName = displayName;
        this.color = color;
        this.defaultSeconds = defaultSeconds;
        this.tickRate = tickRate;
        this.speedRate = speedRate;
        this.blocksHeal = blocksHeal;
    }

    /** 設定ファイルの名前から(なければ null。大文字小文字は問わない) */
    public static StatusEffect parse(String name) {
        if (name == null || name.isBlank())
            return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
