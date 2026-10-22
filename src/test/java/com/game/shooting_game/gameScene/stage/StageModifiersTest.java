package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StageModifiersTest {
    @Test
    @DisplayName("load と setEndlessScale は例外なく動く(enemyHpRate はGAMEシーンのみ効くので値は見ない)")
    void loadDoesNotThrow() {
        assertThatCode(() -> {
            StageModifiers.load(null, null, 1);
            StageModifiers.load(new StageModifiers.Spec(), Difficulty.NORMAL, 10);
            StageModifiers.setEndlessScale(1);
            StageModifiers.setEndlessScale(50);
            StageModifiers.load(null);
        }).doesNotThrowAnyException();
    }
}
