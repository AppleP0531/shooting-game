package com.game.shooting_game.gameScene.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WarpEffectTest {
    @Test
    void 飛ぶ数が少なければ上限の速さのまま() {
        assertEquals(0.03, WarpEffect.stepSeconds(10, 0.03), 1e-9);
    }

    @Test
    void 飛ぶ数が多いと全体が数秒に収まる() {
        double step = WarpEffect.stepSeconds(321, 0.03);
        assertTrue(step < 0.03);
        assertEquals(WarpEffect.MAX_SKIP_SECONDS, step * 321, 1e-9);
    }

    @Test
    void 飛ぶ数が0以下でも0で割らない() {
        assertEquals(0.03, WarpEffect.stepSeconds(0, 0.03), 1e-9);
        assertEquals(0.03, WarpEffect.stepSeconds(-5, 0.03), 1e-9);
    }
}
