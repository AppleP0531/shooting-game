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
    @DisplayName("通常倍率: HP は EASY 1・NORMAL 15・HARD 230・NIGHTMARE 3500、被ダメは EASY 0.8・NORMAL 3・HARD 10・NIGHTMARE 40")
    void normalRates() {
        assertThat(Difficulty.EASY.getEnemyHpRate()).isEqualTo(1.0);
        assertThat(Difficulty.NORMAL.getEnemyHpRate()).isEqualTo(15.0);
        assertThat(Difficulty.HARD.getEnemyHpRate()).isEqualTo(230.0);
        assertThat(Difficulty.HARD.getEnemyDamageRate()).isEqualTo(10.0);
        assertThat(Difficulty.NIGHTMARE.getEnemyHpRate()).isEqualTo(3500.0);
        assertThat(Difficulty.NIGHTMARE.getEnemyDamageRate()).isEqualTo(40.0);
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
        assertThat(hardHp).isCloseTo(299.0, within(1e-9));
        assertThat(nmHp).isCloseTo(9800.0, within(1e-9));
    }

    @Test
    @DisplayName("describe: ボスチャレンジはボス用倍率を掛けた値、ステージ固有の条件も重ねた値を出す")
    void describeBoss() {
        assertThat(Difficulty.NORMAL.describe(null, true)).isEqualTo(Difficulty.NORMAL.describe(null, false));
        assertThat(Difficulty.HARD.describe(null, true)).contains("敵HP ×299").contains("ダメージ ×12");
        assertThat(Difficulty.NIGHTMARE.describe(null, true)).contains("敵HP ×9,800").contains("ダメージ ×56")
                .contains("レア度UP");
        assertThat(Difficulty.HARD.describe(null, true)).isNotEqualTo(Difficulty.HARD.describe(null, false));

        StageModifiers.Spec spec = new StageModifiers.Spec();
        spec.setEnemyHpRate(2.0);
        assertThat(Difficulty.NORMAL.describe(spec, false)).contains("敵HP ×30.0");
    }
}
