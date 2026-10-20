package com.game.shooting_game.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameBalanceTest {
    @Test
    @DisplayName("BalanceConfig.json の値が読めて、下限を満たす")
    void valuesLoaded() {
        assertThat(GameBalance.unitAttackGrowth()).isGreaterThanOrEqualTo(1.0);
        assertThat(GameBalance.stageHpGrowth()).isGreaterThanOrEqualTo(1.0);
        assertThat(GameBalance.attackPerStar()).isGreaterThanOrEqualTo(0.0);
        assertThat(GameBalance.endlessHpPerWave()).isGreaterThanOrEqualTo(0.0);
        assertThat(GameBalance.unitAttackGrowth()).isCloseTo(1.10, within(1e-9));
        assertThat(GameBalance.attackPerStar()).isCloseTo(0.25, within(1e-9));
        assertThat(GameBalance.stageHpGrowth()).isCloseTo(1.15, within(1e-9));
        assertThat(GameBalance.endlessHpPerWave()).isCloseTo(0.015, within(1e-9));
    }

    @Test
    @DisplayName("stageHpRate は 1 から始まり、ステージが進むほど増える")
    void stageHpRateMonotonic() {
        assertThat(GameBalance.stageHpRate(1)).isCloseTo(1.0, within(1e-9));
        assertThat(GameBalance.stageHpRate(0)).isCloseTo(1.0, within(1e-9));
        double prev = GameBalance.stageHpRate(1);
        for (int i = 2; i <= 30; i++) {
            double v = GameBalance.stageHpRate(i);
            assertThat(v).as("stage " + i).isGreaterThan(prev);
            prev = v;
        }
        assertThat(GameBalance.stageHpRate(3))
                .isCloseTo(Math.pow(GameBalance.stageHpGrowth(), 2), within(1e-9));
    }
}
