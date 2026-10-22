package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StarInheritTest {
    @Test
    void ハードで達成したら下の難易度でも達成済み() {
        Map<String, Integer> s = Map.of("mission:stage3:0", 1); // ハード(従来のキー)
        assertThat(DifficultyService.earned(s, 3, Difficulty.EASY, 0)).isTrue();
        assertThat(DifficultyService.earned(s, 3, Difficulty.NORMAL, 0)).isTrue();
        assertThat(DifficultyService.earned(s, 3, Difficulty.HARD, 0)).isTrue();
        // 上の難易度には引き継がれない
        assertThat(DifficultyService.earned(s, 3, Difficulty.NIGHTMARE, 0)).isFalse();
        // 別のミッション・別のステージは別
        assertThat(DifficultyService.earned(s, 3, Difficulty.EASY, 1)).isFalse();
        assertThat(DifficultyService.earned(s, 4, Difficulty.EASY, 0)).isFalse();
    }

    @Test
    void ナイトメアで達成したらすべての難易度で達成済み() {
        Map<String, Integer> s = Map.of("mission:stage3:NIGHTMARE:2", 1);
        for (Difficulty d : Difficulty.values())
            assertThat(DifficultyService.earned(s, 3, d, 2)).as(d.name()).isTrue();
    }

    @Test
    void 下の難易度で達成しても上には引き継がれない() {
        Map<String, Integer> s = Map.of("mission:stage3:NORMAL:0", 1);
        assertThat(DifficultyService.earned(s, 3, Difficulty.EASY, 0)).isTrue();
        assertThat(DifficultyService.earned(s, 3, Difficulty.NORMAL, 0)).isTrue();
        assertThat(DifficultyService.earned(s, 3, Difficulty.HARD, 0)).isFalse();
    }

    @Test
    void 記録がなければ未達成() {
        assertThat(DifficultyService.earned(null, 1, Difficulty.EASY, 0)).isFalse();
        assertThat(DifficultyService.earned(Map.of(), 1, Difficulty.EASY, 0)).isFalse();
    }


    @Test
    void 星の合計は高い難易度の達成を下の難易度の分も数える() {
        // ハード1つ → イージー・ノーマル・ハードで3つ
        assertThat(DifficultyService.countStars(java.util.List.of("mission:stage3:0"))).isEqualTo(3);
        // ナイトメア1つ → 4つ
        assertThat(DifficultyService.countStars(java.util.List.of("mission:stage3:NIGHTMARE:0"))).isEqualTo(4);
        // 同じミッションをハードとナイトメアで取っても、重ねて数えない
        assertThat(DifficultyService.countStars(java.util.List.of("mission:stage3:0", "mission:stage3:NIGHTMARE:0")))
                .isEqualTo(4);
        // 別のミッションは別に数える
        assertThat(DifficultyService.countStars(java.util.List.of("mission:stage3:0", "mission:stage3:1"))).isEqualTo(6);
        // ミッションのキー以外は数えない
        assertThat(DifficultyService.countStars(java.util.List.of("endless:wave", "daily:x"))).isEqualTo(0);
    }
}
