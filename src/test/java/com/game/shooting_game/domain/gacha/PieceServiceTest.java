package com.game.shooting_game.domain.gacha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;

class PieceServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.load("tester", "create");
    }

    @Test
    @DisplayName("add で増え、owned で読める。0以下の追加は無視する")
    void addAndOwned() {
        assertThat(PieceService.owned("Ship2")).isZero();
        PieceService.add("Ship2", 5);
        PieceService.add("Ship2", 3);
        PieceService.add("Ship2", 0);
        PieceService.add("Ship2", -4);
        assertThat(PieceService.owned("Ship2")).isEqualTo(8);
        assertThat(PieceService.owned("Ship3")).isZero();
        assertThat(DataManager.getUserData().getProgress().getHighScores()).containsEntry("piece:Ship2", 8);
    }

    @Test
    @DisplayName("consume は足りれば減らして true、足りなければ false で減らさない")
    void consume() {
        PieceService.add("Ship2", 10);
        assertThat(PieceService.consume("Ship2", 11)).isFalse();
        assertThat(PieceService.owned("Ship2")).isEqualTo(10);
        assertThat(PieceService.consume("Ship2", 4)).isTrue();
        assertThat(PieceService.owned("Ship2")).isEqualTo(6);
        assertThat(PieceService.consume("Ship2", 6)).isTrue();
        assertThat(PieceService.owned("Ship2")).isZero();
    }

    @Test
    @DisplayName("rankUpPieces の表と、範囲外は0")
    void rankUpPieces() {
        assertThat(PieceService.UNLOCK_PIECES).isEqualTo(50);
        int[] expected = { 100, 200, 500, 1000, 1500 };
        for (int r = 0; r < expected.length; r++)
            assertThat(PieceService.rankUpPieces(r)).isEqualTo(expected[r]);
        assertThat(PieceService.rankUpPieces(5)).isZero();
        assertThat(PieceService.rankUpPieces(-1)).isZero();
    }
}
