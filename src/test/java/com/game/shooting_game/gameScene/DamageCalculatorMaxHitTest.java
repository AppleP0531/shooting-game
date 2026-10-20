package com.game.shooting_game.gameScene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DamageCalculatorMaxHitTest {
    @Test
    @DisplayName("1回の攻撃のダメージ上限は 99,999(5桁)")
    void maxHitDamage() {
        assertThat(DamageCalculator.MAX_HIT_DAMAGE).isEqualTo(99_999);
    }
}
