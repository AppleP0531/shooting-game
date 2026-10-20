package com.game.shooting_game.gameScene.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnemyAffixTest {
    @Test
    @DisplayName("付く確率(CHANCE)未満の乱数なら付き、以上なら付かない")
    void chance() {
        assertEquals(EnemyAffix.SWIFT, EnemyAffix.roll(0.0, 0.0));
        assertEquals(EnemyAffix.SWIFT, EnemyAffix.roll(EnemyAffix.CHANCE - 1e-9, 0.0));
        assertNull(EnemyAffix.roll(EnemyAffix.CHANCE, 0.0));
        assertNull(EnemyAffix.roll(0.99, 0.5));
    }

    @Test
    @DisplayName("種類の乱数は、4つの種類に等しく割り振られる")
    void kind() {
        assertEquals(EnemyAffix.SWIFT, EnemyAffix.roll(0, 0.0));
        assertEquals(EnemyAffix.SWIFT, EnemyAffix.roll(0, 0.24));
        assertEquals(EnemyAffix.ARMORED, EnemyAffix.roll(0, 0.25));
        assertEquals(EnemyAffix.REGEN, EnemyAffix.roll(0, 0.5));
        assertEquals(EnemyAffix.RAPID, EnemyAffix.roll(0, 0.75));
        assertEquals(EnemyAffix.RAPID, EnemyAffix.roll(0, 0.999999));
    }

    @Test
    @DisplayName("範囲外の乱数でも例外にならない")
    void outOfRange() {
        assertEquals(EnemyAffix.RAPID, EnemyAffix.roll(0, 1.5));
        assertEquals(EnemyAffix.SWIFT, EnemyAffix.roll(0, -1));
    }

    @Test
    @DisplayName("倍率の定数が、狙った強さになっている")
    void constants() {
        assertTrue(EnemyAffix.SWIFT_SPEED_RATE > 1);
        assertTrue(EnemyAffix.ARMORED_HP_RATE > 1);
        assertTrue(EnemyAffix.RAPID_COOLDOWN_RATE < 1 && EnemyAffix.RAPID_COOLDOWN_RATE > 0);
        assertTrue(EnemyAffix.REGEN_RATE_PER_SEC > 0 && EnemyAffix.REGEN_RATE_PER_SEC < 0.2);
    }
}
