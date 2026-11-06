package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EndlessWavesSkipTargetTest {
    @Test
    @DisplayName("スキルなしなら、記録がいくつでも 1 から始まる")
    void noSkillStartsAtOne() {
        assertThat(EndlessWaves.skipTarget(0, 0)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(0, 50)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(0, 500)).isEqualTo(1);
    }

    @Test
    @DisplayName("スキルがあると、最高到達ウェーブの50ウェーブ前から始まる")
    void startsFiftyBeforeBest() {
        assertThat(EndlessWaves.skipTarget(50, 100)).isEqualTo(50);
        assertThat(EndlessWaves.skipTarget(50, 60)).isEqualTo(10);
        assertThat(EndlessWaves.skipTarget(50, 51)).isEqualTo(1);
    }

    @Test
    @DisplayName("最高が50以下なら 1 から始まる(最低 1)")
    void atLeastOne() {
        assertThat(EndlessWaves.skipTarget(50, 50)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(50, 10)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(50, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("負の値や記録でも最低 1、小数は切り捨て")
    void clampsAndTruncates() {
        assertThat(EndlessWaves.skipTarget(-5, 100)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(50, -4)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(49.9, 100)).isEqualTo(51);
    }
}
