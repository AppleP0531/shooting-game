package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;

class DifficultyServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
    }

    @Test
    @DisplayName("記録キー: NORMAL は従来形式、他は :難易度 付き")
    void key() {
        assertThat(DifficultyService.key(3, Difficulty.NORMAL)).isEqualTo("stage3");
        assertThat(DifficultyService.key(3, Difficulty.HARD)).isEqualTo("stage3:HARD");
        assertThat(DifficultyService.key(12, Difficulty.NIGHTMARE)).isEqualTo("stage12:NIGHTMARE");
    }

    @Test
    @DisplayName("scaleRange は範囲に倍率をかけ、max が min を下回らない")
    void scaleRange() {
        assertThat(DifficultyService.scaleRange("3~6", 1.0)).isEqualTo("3~6");
        assertThat(DifficultyService.scaleRange("3~6", 2.0)).isEqualTo("6~12");
        assertThat(DifficultyService.scaleRange("10~20", 0.8)).isEqualTo("8~16");
        assertThat(DifficultyService.scaleRange("5~5", 1.6)).isEqualTo("8~8");
    }

    @Test
    @DisplayName("scaleRange は解釈できない文字列をそのまま返す")
    void scaleRangeInvalid() {
        assertThat(DifficultyService.scaleRange("5", 2.0)).isEqualTo("5");
        assertThat(DifficultyService.scaleRange("a~b", 2.0)).isEqualTo("a~b");
    }

    @Test
    @DisplayName("ハイスコアは難易度ごとに別々に取得できる")
    void highScore() {
        DataManager.getUserData().getProgress().updateHighScore("stage1", 500);
        DataManager.getUserData().getProgress().updateHighScore("stage1:HARD", 900);
        assertThat(DifficultyService.highScore(1, Difficulty.NORMAL)).isEqualTo(500);
        assertThat(DifficultyService.highScore(1, Difficulty.HARD)).isEqualTo(900);
        assertThat(DifficultyService.highScore(1, Difficulty.EASY)).isZero();
    }

    @Test
    @DisplayName("HARD はノーマルクリア後に解放され、EASY/NORMAL は常に解放")
    void unlockHard() {
        assertThat(DifficultyService.unlocked(1, Difficulty.EASY)).isTrue();
        assertThat(DifficultyService.unlocked(1, Difficulty.NORMAL)).isTrue();
        assertThat(DifficultyService.unlocked(1, Difficulty.HARD)).isFalse();
        assertThat(DifficultyService.lockReason(1, Difficulty.HARD)).contains("ノーマル");
        DataManager.getUserData().getProgress().setClearedStage(1);
        assertThat(DifficultyService.cleared(1, Difficulty.NORMAL)).isTrue();
        assertThat(DifficultyService.cleared(2, Difficulty.NORMAL)).isFalse();
        assertThat(DifficultyService.unlocked(1, Difficulty.HARD)).isTrue();
        assertThat(DifficultyService.lockReason(1, Difficulty.HARD)).isEmpty();
    }

    @Test
    @DisplayName("管理者モードでは全難易度が解放される")
    void adminMode() {
        DataManager.setAdminMode(true);
        try {
            assertThat(DifficultyService.unlocked(1, Difficulty.NIGHTMARE)).isTrue();
        } finally {
            DataManager.setAdminMode(false);
        }
    }

    @Test
    @DisplayName("Difficulty の倍率は難しいほど大きい")
    void difficultyRates() {
        assertThat(Difficulty.EASY.getRewardRate()).isLessThan(Difficulty.NORMAL.getRewardRate());
        assertThat(Difficulty.HARD.getRewardRate()).isGreaterThan(Difficulty.NORMAL.getRewardRate());
        assertThat(Difficulty.NIGHTMARE.getEnemyHpRate()).isGreaterThan(Difficulty.HARD.getEnemyHpRate());
        assertThat(Difficulty.NORMAL.describe(null, false)).contains("敵HP ×15.0").contains("報酬 ×1.0");
    }
}
