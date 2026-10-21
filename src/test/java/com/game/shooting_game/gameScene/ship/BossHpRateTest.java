package com.game.shooting_game.gameScene.ship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.utils.GameBalance;

class BossHpRateTest {
    @Test
    @DisplayName("BalanceConfig の bossHpRate は 5 以上")
    void rateIsAtLeastFive() {
        assertThat(GameBalance.bossHpRate()).isGreaterThanOrEqualTo(5.0);
    }

    @Test
    @DisplayName("ボスのゲージHPには倍率がかかっている(設定の700→Boss1)")
    void bossGaugeHpScaled() {
        assertThat(BossLibrary.get("Boss1").getGaugeHp()).isCloseTo(700 * GameBalance.bossHpRate(), within(1e-6));
        assertThat(BossLibrary.get("Boss10").getGaugeHp()).isCloseTo(2100 * GameBalance.bossHpRate(), within(1e-6));
    }

    @Test
    @DisplayName("コインボスには倍率をかけない")
    void coinBossNotScaled() {
        assertThat(BossLibrary.get("BossCoin").getGaugeHp()).isCloseTo(24000, within(1e-6));
    }
}
