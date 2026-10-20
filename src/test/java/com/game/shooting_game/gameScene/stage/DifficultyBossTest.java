package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DifficultyBossTest {
    @Test
    @DisplayName("ボス用倍率: HARD 1.3/1.2、NIGHTMARE 2.8/1.4、他は 1.0")
    void bossRates() {
        assertThat(Difficulty.EASY.getBossHpRate()).isEqualTo(1.0);
        assertThat(Difficulty.EASY.getBossDamageRate()).isEqualTo(1.0);
        assertThat(Difficulty.NORMAL.getBossHpRate()).isEqualTo(1.0);
        assertThat(Difficulty.NORMAL.getBossDamageRate()).isEqualTo(1.0);
        assertThat(Difficulty.HARD.getBossHpRate()).isEqualTo(1.3);
        assertThat(Difficulty.HARD.getBossDamageRate()).isEqualTo(1.2);
        assertThat(Difficulty.NIGHTMARE.getBossHpRate()).isEqualTo(2.8);
        assertThat(Difficulty.NIGHTMARE.getBossDamageRate()).isEqualTo(1.4);
    }

    @Test
    @DisplayName("通常倍率: HARD は HP 1.8・被ダメ 1.4、NIGHTMARE は HP 3.5・被ダメ 2.0")
    void normalRates() {
        assertThat(Difficulty.HARD.getEnemyHpRate()).isEqualTo(1.8);
        assertThat(Difficulty.HARD.getEnemyDamageRate()).isEqualTo(1.4);
        assertThat(Difficulty.NIGHTMARE.getEnemyHpRate()).isEqualTo(3.5);
        assertThat(Difficulty.NIGHTMARE.getEnemyDamageRate()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("難易度が上がるほど HP・被ダメ倍率は単調に増える(通常もボスも)")
    void monotonic() {
        Difficulty[] order = { Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD, Difficulty.NIGHTMARE };
        for (int i = 1; i < order.length; i++) {
            Difficulty lo = order[i - 1];
            Difficulty hi = order[i];
            assertThat(hi.getEnemyHpRate()).isGreaterThan(lo.getEnemyHpRate());
            assertThat(hi.getEnemyDamageRate()).isGreaterThan(lo.getEnemyDamageRate());
            assertThat(hi.getEnemyHpRate() * hi.getBossHpRate())
                    .isGreaterThan(lo.getEnemyHpRate() * lo.getBossHpRate());
            assertThat(hi.getEnemyDamageRate() * hi.getBossDamageRate())
                    .isGreaterThan(lo.getEnemyDamageRate() * lo.getBossDamageRate());
        }
        double hardHp = Difficulty.HARD.getEnemyHpRate() * Difficulty.HARD.getBossHpRate();
        double nmHp = Difficulty.NIGHTMARE.getEnemyHpRate() * Difficulty.NIGHTMARE.getBossHpRate();
        assertThat(hardHp).isCloseTo(2.34, within(1e-9));
        assertThat(nmHp).isCloseTo(9.8, within(1e-9));
    }

    @Test
    @DisplayName("describeBoss: NORMAL は describe と同じ、HARD/NIGHTMARE はボス用倍率を掛けた値を出す")
    void describeBoss() {
        assertThat(Difficulty.NORMAL.describeBoss()).isEqualTo(Difficulty.NORMAL.describe());
        assertThat(Difficulty.HARD.describeBoss()).contains("敵HP ×2.3").contains("被ダメージ ×1.7");
        assertThat(Difficulty.NIGHTMARE.describeBoss()).contains("敵HP ×9.8").contains("被ダメージ ×2.8")
                .contains("レア度UP");
        assertThat(Difficulty.HARD.describeBoss()).isNotEqualTo(Difficulty.HARD.describe());
    }
}
