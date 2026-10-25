package com.game.shooting_game.domain.power;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.domain.power.PowerService.Verdict;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.utils.GameBalance;

class PowerServiceTest {
    @Test
    @DisplayName("戦力値 = 100 × 火力倍率 × √(耐久÷100)。基本の機体(火力1・耐久100)は100")
    void powerFormula() {
        assertThat(PowerService.power(1.0, 100)).isCloseTo(100.0, within(1e-9));
        assertThat(PowerService.power(2.0, 100)).isCloseTo(200.0, within(1e-9));
        assertThat(PowerService.power(1.0, 400)).isCloseTo(200.0, within(1e-9));
        assertThat(PowerService.power(0, 100)).isZero();
        assertThat(PowerService.power(1.0, -5)).isZero(); // 負の耐久は0扱い
    }

    @Test
    @DisplayName("ステージ1・ノーマルの推定必要戦力値は、基準(requiredBase)と同じ")
    void requiredBase() {
        assertThat(PowerService.required(1, Difficulty.NORMAL)).isCloseTo(GameBalance.requiredBase(), within(1e-9));
        assertThat(PowerService.required(1, null)).isEqualTo(PowerService.required(1, Difficulty.NORMAL));
    }

    @Test
    @DisplayName("ステージが進むほど、難易度が上がるほど、必要戦力値が増える")
    void requiredMonotonic() {
        assertThat(PowerService.required(50, Difficulty.NORMAL)).isGreaterThan(PowerService.required(10, Difficulty.NORMAL));
        assertThat(PowerService.required(200, Difficulty.NORMAL)).isGreaterThan(PowerService.required(50, Difficulty.NORMAL));
        assertThat(PowerService.required(30, Difficulty.HARD)).isGreaterThan(PowerService.required(30, Difficulty.NORMAL));
        assertThat(PowerService.required(30, Difficulty.NIGHTMARE)).isGreaterThan(PowerService.required(30, Difficulty.HARD));
    }

    @Test
    @DisplayName("足りる/ぎりぎり(80%以上)/不足の判定")
    void verdict() {
        assertThat(PowerService.verdict(100, 100)).isEqualTo(Verdict.ENOUGH);
        assertThat(PowerService.verdict(150, 100)).isEqualTo(Verdict.ENOUGH);
        assertThat(PowerService.verdict(80, 100)).isEqualTo(Verdict.BARELY);
        assertThat(PowerService.verdict(99.9, 100)).isEqualTo(Verdict.BARELY);
        assertThat(PowerService.verdict(79.9, 100)).isEqualTo(Verdict.SHORT);
        assertThat(PowerService.verdict(0, 100)).isEqualTo(Verdict.SHORT);
    }
}
