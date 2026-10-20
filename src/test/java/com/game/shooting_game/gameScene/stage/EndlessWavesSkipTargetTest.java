package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EndlessWavesSkipTargetTest {
    @Test
    @DisplayName("スキルなし・記録なしなら 1 から始まる")
    void noSkillStartsAtOne() {
        assertThat(EndlessWaves.skipTarget(0, 0)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(0, 50)).isEqualTo(1);
    }

    @Test
    @DisplayName("スキルの数だけ飛べるが、最高到達ウェーブの次(best+1)を超えない")
    void cappedByBestPlusOne() {
        assertThat(EndlessWaves.skipTarget(10, 100)).isEqualTo(11);
        assertThat(EndlessWaves.skipTarget(10, 5)).isEqualTo(6);
        assertThat(EndlessWaves.skipTarget(10, 9)).isEqualTo(10);
        assertThat(EndlessWaves.skipTarget(10, 10)).isEqualTo(11);
    }

    @Test
    @DisplayName("負の値や小数でも最低 1、小数は切り捨て")
    void clampsAndTruncates() {
        assertThat(EndlessWaves.skipTarget(-5, 10)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(3, -4)).isEqualTo(1);
        assertThat(EndlessWaves.skipTarget(4.9, 100)).isEqualTo(5);
    }
}
