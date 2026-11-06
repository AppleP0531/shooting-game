package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.game.shooting_game.utils.GameBalance;

class NormalCapTest {
    @Test
    void 敵HPの上限はステージ200のナイトメアの値() {
        double expected = GameBalance.stageHpRate(StageModifiers.NORMAL_STAGE_COUNT)
                * Difficulty.NIGHTMARE.getEnemyHpRate();
        assertThat(StageModifiers.normalHpCap(false)).isCloseTo(expected, within(1e-6));
        // ボスチャレンジは、ボス用の倍率も込み
        assertThat(StageModifiers.normalHpCap(true))
                .isCloseTo(expected * Difficulty.NIGHTMARE.getBossHpRate(), within(1e-6));
    }

    @Test
    void ダメージの上限はナイトメアの値() {
        // ステージ200までに増える分(stageDamageRate)も込みの、ナイトメアの値
        double stage = GameBalance.stageDamageRate(StageModifiers.NORMAL_STAGE_COUNT);
        assertThat(StageModifiers.normalDamageCap(false))
                .isCloseTo(stage * Difficulty.NIGHTMARE.getEnemyDamageRate(), within(1e-9));
        assertThat(StageModifiers.normalDamageCap(true))
                .isCloseTo(stage * Difficulty.NIGHTMARE.getEnemyDamageRate() * Difficulty.NIGHTMARE.getBossDamageRate(),
                        within(1e-9));
    }

    @Test
    void ナイトメアの通常の最大より小さい難易度は上限の内側() {
        for (Difficulty d : Difficulty.values()) {
            assertThat(d.getEnemyHpRate() * GameBalance.stageHpRate(StageModifiers.NORMAL_STAGE_COUNT))
                    .isLessThanOrEqualTo(StageModifiers.normalHpCap(false) + 1e-6);
            assertThat(d.getEnemyDamageRate() * GameBalance.stageDamageRate(StageModifiers.NORMAL_STAGE_COUNT))
                    .isLessThanOrEqualTo(StageModifiers.normalDamageCap(false) + 1e-6);
        }
    }
}
