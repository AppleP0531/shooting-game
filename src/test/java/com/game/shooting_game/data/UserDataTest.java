package com.game.shooting_game.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDataTest {
    @Test
    @DisplayName("新規ユーザーは Ship1 を所持し、初期値が正しい")
    void initialState() {
        UserData d = new UserData();
        assertThat(d.getShips()).containsOnlyKeys("Ship1");
        assertThat(d.getSelectedShip()).isEqualTo("Ship1");
        assertThat(d.getTotalPlayTime()).isEqualTo("00:00:00");
        assertThat(d.getProgress().getExp()).isZero();
        assertThat(d.getResources().getCoins()).isZero();
    }

    @Test
    @DisplayName("unlockShip は冪等で、既存の強化状況を壊さない")
    void unlockShipIdempotent() {
        UserData d = new UserData();
        d.unlockShip("Ship2");
        d.getShips().get("Ship2").setLevel(10);
        d.unlockShip("Ship2");
        assertThat(d.getShips()).containsOnlyKeys("Ship1", "Ship2");
        assertThat(d.getShips().get("Ship2").getLevel()).isEqualTo(10);
        assertThat(d.getShips().get("Ship2").getStars()).isZero();
    }

    @Test
    @DisplayName("addExp は経験値を加算する")
    void addExp() {
        UserData.Progress p = new UserData.Progress();
        p.addExp(30);
        p.addExp(12);
        assertThat(p.getExp()).isEqualTo(42);
    }

    @Test
    @DisplayName("updateHighScore は高い値のときだけ更新する")
    void updateHighScore() {
        UserData.Progress p = new UserData.Progress();
        p.updateHighScore("stage1", 100);
        p.updateHighScore("stage1", 50);
        assertThat(p.getHighScores()).containsEntry("stage1", 100);
        p.updateHighScore("stage1", 200);
        assertThat(p.getHighScores()).containsEntry("stage1", 200);
        p.updateHighScore("stage2", 0); // 0 は記録されない
        assertThat(p.getHighScores()).doesNotContainKey("stage2");
    }
}
