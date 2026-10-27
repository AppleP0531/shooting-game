package com.game.shooting_game.gameScene.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItemManagerAtkUpTest {
    @BeforeEach
    void reset() {
        // 前のテストの効果を持ち越さない(時間0 = 効果なし)
        ItemManager.startAtkUp(1.0, 0);
    }

    @Test
    @DisplayName("効果中は指定の倍率、残り時間が読める")
    void active() {
        ItemManager.startAtkUp(1.3, 8.0);
        assertEquals(1.3, ItemManager.atkUpMultiplier(), 1e-12);
        assertEquals(8.0, ItemManager.getAtkUpTimer(), 1e-12);
    }

    @Test
    @DisplayName("効果がなければ倍率は1")
    void inactive() {
        assertEquals(1.0, ItemManager.atkUpMultiplier(), 1e-12);
        assertEquals(0.0, ItemManager.getAtkUpTimer(), 1e-12);
    }

    @Test
    @DisplayName("重ねて取ると、倍率と時間を取り直す")
    void restart() {
        ItemManager.startAtkUp(1.3, 2.0);
        ItemManager.startAtkUp(1.5, 8.0);
        assertEquals(1.5, ItemManager.atkUpMultiplier(), 1e-12);
        assertEquals(8.0, ItemManager.getAtkUpTimer(), 1e-12);
    }

    @Test
    @DisplayName("時間が0以下なら、倍率が設定されていても1")
    void zeroTime() {
        ItemManager.startAtkUp(2.0, 0);
        assertEquals(1.0, ItemManager.atkUpMultiplier(), 1e-12);
        ItemManager.startAtkUp(2.0, -1);
        assertEquals(1.0, ItemManager.atkUpMultiplier(), 1e-12);
    }
}
