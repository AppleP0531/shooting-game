package com.game.shooting_game.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ダメージ計算の式(GameUtils)のテスト。DamageCalculator が集める値は渡さず、式だけを確かめる。
 */
class DamageFormulaTest {
    private static final double EPS = 1e-9;

    @Test
    @DisplayName("火力 = 基礎ダメージ × 火力倍率 + 実数値の攻撃力")
    void attackDamage() {
        assertEquals(17.0, GameUtils.attackDamage(10, 1.5, 2), EPS);
        assertEquals(10.0, GameUtils.attackDamage(10, 1, 0), EPS);
        assertEquals(2.0, GameUtils.attackDamage(0, 3, 2), EPS);
    }

    @Test
    @DisplayName("敵が受けるダメージ = 火力 × 色の倍率 ÷ 敵のHP倍率")
    void damageToEnemy() {
        assertEquals(4.25, GameUtils.damageToEnemy(17, 0.5, 2), EPS);
        assertEquals(17.0, GameUtils.damageToEnemy(17, 1, 1), EPS);
    }

    @Test
    @DisplayName("自機が受けるダメージ: 割合で軽減 → 敵の強さの倍率 → 実数値の防御を引く")
    void damageToPlayerOrder() {
        assertEquals(50.0, GameUtils.damageToPlayer(100, 0.5, 1.0, 0), EPS);
        assertEquals(60.0, GameUtils.damageToPlayer(100, 0.5, 1.2, 0), EPS);
        assertEquals(45.0, GameUtils.damageToPlayer(100, 0.5, 1.0, 5), EPS);
        // 倍率をかけたあとに実数値を引く(100 × 0.5 × 1.2 − 5)
        assertEquals(55.0, GameUtils.damageToPlayer(100, 0.5, 1.2, 5), EPS);
    }

    @Test
    @DisplayName("軽減率は0〜0.8に収まり、どれだけ固めても元の3割は通る")
    void damageToPlayerLimits() {
        // 軽減率が0.8を超えても0.8として扱う(100 × 0.2 = 20 だが、最低3割の30が通る)
        assertEquals(30.0, GameUtils.damageToPlayer(100, 0.95, 1.0, 0), EPS);
        // 実数値の防御が大きすぎても最低3割
        assertEquals(30.0, GameUtils.damageToPlayer(100, 0.5, 1.0, 1000), EPS);
        // 負の軽減率は0として扱う
        assertEquals(100.0, GameUtils.damageToPlayer(100, -0.5, 1.0, 0), EPS);
    }

    @Test
    @DisplayName("軽減前のダメージが0以下ならそのまま返す")
    void damageToPlayerNonPositive() {
        assertEquals(0.0, GameUtils.damageToPlayer(0, 0.5, 1.2, 5), EPS);
        assertEquals(-5.0, GameUtils.damageToPlayer(-5, 0.5, 1.2, 5), EPS);
    }
}
