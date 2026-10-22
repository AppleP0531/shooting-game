package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.stage.StageManager.MissionSpec;

class MissionStarsTest {
    private Map<String, Integer> store() {
        return DataManager.getUserData().getProgress().getHighScores();
    }

    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
    }

    @Test
    @DisplayName("記録キー: HARD は従来の mission:stageN:i、ほかは難易度つき")
    void missionKey() {
        assertThat(DifficultyService.missionKey(3, Difficulty.HARD, 1)).isEqualTo("mission:stage3:1");
        assertThat(DifficultyService.missionKey(3, Difficulty.NORMAL, 0)).isEqualTo("mission:stage3:NORMAL:0");
        assertThat(DifficultyService.missionKey(3, Difficulty.EASY, 2)).isEqualTo("mission:stage3:EASY:2");
        assertThat(DifficultyService.missionKey(3, Difficulty.NIGHTMARE, 2)).isEqualTo("mission:stage3:NIGHTMARE:2");
    }

    @Test
    @DisplayName("judge: 被弾回数・HP割合・時間は境界値を含めて達成")
    void judgeBoundary() {
        MissionSpec m = new MissionSpec();
        double limit = 100;
        boolean[] at = DifficultyService.judge(m, limit, m.getMaxHits(), m.getMinHpRate(), limit);
        assertThat(at).containsExactly(true, true, true);

        boolean[] over = DifficultyService.judge(m, limit, m.getMaxHits() + 1, m.getMinHpRate() - 0.001,
                limit + 0.001);
        assertThat(over).containsExactly(false, false, false);
    }

    @Test
    @DisplayName("totalStars は全難易度の★を数える(高い難易度の達成は、下の難易度の分も数える)")
    void totalStars() {
        assertThat(DifficultyService.totalStars()).isZero();
        store().put("mission:stage1:0", 1);
        store().put("mission:stage1:NORMAL:0", 1);
        store().put("mission:stage2:EASY:2", 1);
        store().put("stage1", 100);
        store().put("stage1:HARD", 100);
        // HARD(従来のキー)1つ=EASY・NORMAL・HARD の3つ、NORMAL は重なる、EASY の別ステージで1つ
        assertThat(DifficultyService.totalStars()).isEqualTo(4);
    }
}
