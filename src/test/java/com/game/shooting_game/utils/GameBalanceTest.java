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
        assertThat(GameBalance.rankAttackGrowth()).isGreaterThanOrEqualTo(1.0);
        assertThat(GameBalance.endlessHpPerWave()).isGreaterThanOrEqualTo(0.0);
        assertThat(GameBalance.unitAttackGrowth()).isCloseTo(1.028, within(1e-9));
        assertThat(GameBalance.rankAttackGrowth()).isCloseTo(4.0, within(1e-9));
        // ランクアップでレベルが1に戻っても、前のランクのLv50より弱くならない
        assertThat(GameBalance.rankAttackGrowth()).isGreaterThanOrEqualTo(Math.pow(GameBalance.unitAttackGrowth(), 49));
        assertThat(GameBalance.stageHpGrowth()).isCloseTo(1.034, within(1e-9));
        assertThat(GameBalance.endlessHpPerWave()).isCloseTo(0.015, within(1e-9));
        assertThat(GameBalance.bossHpRate()).isCloseTo(5.0, within(1e-9));
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

    @Test
    @DisplayName("敵レベルが上がるほど敵HPが増える(レベル0は等倍、負は0扱い)")
    void enemyLevelHpRate() {
        assertThat(GameBalance.enemyLevelHpRate(0)).isCloseTo(1.0, within(1e-9));
        assertThat(GameBalance.enemyLevelHpRate(-3)).isCloseTo(1.0, within(1e-9));
        assertThat(GameBalance.enemyLevelHpRate(1)).isCloseTo(1 + GameBalance.enemyLevelHp(), within(1e-9));
        assertThat(GameBalance.enemyLevelHpRate(2)).isGreaterThan(GameBalance.enemyLevelHpRate(1));
    }

    @Test
    @DisplayName("stageDamageRate は 1 から始まり、ステージが進むほど増える(HPより緩やか)")
    void stageDamageRateMonotonic() {
        assertThat(GameBalance.stageDamageRate(1)).isCloseTo(1.0, within(1e-9));
        assertThat(GameBalance.stageDamageRate(0)).isCloseTo(1.0, within(1e-9));
        assertThat(GameBalance.stageDamageRate(100)).isGreaterThan(GameBalance.stageDamageRate(50));
        assertThat(GameBalance.stageDamageRate(200)).isLessThan(GameBalance.stageHpRate(200));
    }
}
