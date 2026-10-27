package com.game.shooting_game.stageScene.normal.subPanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StageScrollTest {
    @Test
    void 最初のステージは動かさない() {
        assertEquals(0, StageBtnPanel.scrollToCenter(0, 1.0, 600, 5000), 1e-9);
    }

    @Test
    void 台がパネルの中央に来る量になる() {
        // 高さ600、基準点は下から50(=550)。台の高さ800なら、中央(300)に置くには 300 - 550 + 800 = 550
        assertEquals(550, StageBtnPanel.scrollToCenter(800, 1.0, 600, 5000), 1e-9);
    }

    @Test
    void 倍率が違っても同じ割合で動く() {
        assertEquals(2 * 550, StageBtnPanel.scrollToCenter(800, 2.0, 1200, 9999), 1e-9);
    }

    @Test
    void 最大を超えず負にもならない() {
        assertEquals(100, StageBtnPanel.scrollToCenter(800, 1.0, 600, 100), 1e-9);
        assertEquals(0, StageBtnPanel.scrollToCenter(-500, 1.0, 600, 100), 1e-9);
        assertEquals(0, StageBtnPanel.scrollToCenter(800, 1.0, 600, -3), 1e-9);
    }
}
