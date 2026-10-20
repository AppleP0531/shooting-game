package com.game.shooting_game.gameScene.ship;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.stage.EndlessWaves;

class BossGaugeWindowTest {
    @Test
    @DisplayName("無限モードは後ろの3ゲージ(3ゲージ未満のボスは全部)")
    void endless() {
        assertThat(BossLibrary.gaugeWindow(7, EndlessWaves.STAGE_ID)).containsExactly(4, 3);
        assertThat(BossLibrary.gaugeWindow(5, EndlessWaves.STAGE_ID)).containsExactly(2, 3);
        assertThat(BossLibrary.gaugeWindow(3, EndlessWaves.STAGE_ID)).containsExactly(0, 3);
        assertThat(BossLibrary.gaugeWindow(2, EndlessWaves.STAGE_ID)).containsExactly(0, 2);
    }

    @Test
    @DisplayName("ノーマルステージ: 10の倍数は5ゲージ、5の倍数は3ゲージ(先頭から)")
    void normalStages() {
        assertThat(BossLibrary.gaugeWindow(7, 30)).containsExactly(0, 5);
        assertThat(BossLibrary.gaugeWindow(5, 10)).containsExactly(0, 5);
        assertThat(BossLibrary.gaugeWindow(3, 10)).containsExactly(0, 3); // ゲージが足りなければある分だけ
        assertThat(BossLibrary.gaugeWindow(6, 25)).containsExactly(0, 3);
        assertThat(BossLibrary.gaugeWindow(4, 15)).containsExactly(0, 3);
    }

    @Test
    @DisplayName("それ以外のステージ(5の倍数でない・100以降)は定義のまま")
    void others() {
        assertThat(BossLibrary.gaugeWindow(6, 7)).containsExactly(0, 6);
        assertThat(BossLibrary.gaugeWindow(6, 1155)).containsExactly(0, 6); // エクストラ(1000番台)
        assertThat(BossLibrary.gaugeWindow(6, 155)).containsExactly(0, 3); // 自動生成の通常ステージ(5の倍数)
    }

    @Test
    @DisplayName("絞った定義は、ゲージ数・撤収のゲージ数が範囲内で、元の定義は変わらない")
    void trimmedDefinition() {
        var full = BossLibrary.get("Boss6");
        int total = full.getGauges().size();
        var d = BossLibrary.forFight("Boss6", EndlessWaves.STAGE_ID);
        assertThat(d.getGauges()).hasSize(3);
        assertThat(d.getGauges().get(0)).isSameAs(full.getGauges().get(total - 3));
        assertThat(d.getRetreatAfterGauges()).isLessThanOrEqualTo(3);
        assertThat(full.getGauges()).hasSize(total);
    }
}
