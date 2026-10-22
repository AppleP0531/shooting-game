package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.stage.DifficultyService.LiveMission;
import com.game.shooting_game.gameScene.stage.DifficultyService.LiveState;
import com.game.shooting_game.gameScene.stage.StageManager.MissionSpec;

class LiveMissionTest {
    private static MissionSpec spec() {
        MissionSpec m = new MissionSpec(); // 被弾3回以内・HP50%以上(既定)
        return m;
    }

    @Test
    void 順調なら3つとも見込みあり() {
        List<LiveMission> l = DifficultyService.live(spec(), 60, 1, 0.8, 20, new boolean[3]);
        assertThat(l).hasSize(3);
        assertThat(l).allMatch(x -> x.state() == LiveState.OK);
        assertThat(l.get(0).progress()).isEqualTo("被弾 1/3");
        assertThat(l.get(1).progress()).isEqualTo("HP 80%");
        assertThat(l.get(2).progress()).isEqualTo("20秒 / 60秒");
    }

    @Test
    void 被弾が多すぎると失敗() {
        assertThat(DifficultyService.live(spec(), 60, 3, 1.0, 0, new boolean[3]).get(0).state())
                .isEqualTo(LiveState.OK); // 上限ちょうどは満たす
        assertThat(DifficultyService.live(spec(), 60, 4, 1.0, 0, new boolean[3]).get(0).state())
                .isEqualTo(LiveState.FAILED);
    }

    @Test
    void HPが足りないうちは保留で回復すれば満たす() {
        assertThat(DifficultyService.live(spec(), 60, 0, 0.3, 0, new boolean[3]).get(1).state())
                .isEqualTo(LiveState.PENDING);
        assertThat(DifficultyService.live(spec(), 60, 0, 0.5, 0, new boolean[3]).get(1).state())
                .isEqualTo(LiveState.OK);
    }

    @Test
    void 時間切れは失敗() {
        assertThat(DifficultyService.live(spec(), 60, 0, 1.0, 60, new boolean[3]).get(2).state())
                .isEqualTo(LiveState.OK);
        assertThat(DifficultyService.live(spec(), 60, 0, 1.0, 61, new boolean[3]).get(2).state())
                .isEqualTo(LiveState.FAILED);
    }

    @Test
    void 記録済みかどうかが引き継がれる() {
        List<LiveMission> l = DifficultyService.live(spec(), 60, 0, 1.0, 0, new boolean[] { true, false, true });
        assertThat(l.get(0).earned()).isTrue();
        assertThat(l.get(1).earned()).isFalse();
        assertThat(l.get(2).earned()).isTrue();
    }
}
