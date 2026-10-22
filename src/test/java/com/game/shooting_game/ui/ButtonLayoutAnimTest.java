package com.game.shooting_game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;

import org.junit.jupiter.api.Test;

class ButtonLayoutAnimTest {
    @Test
    void anchoredTopLeftは基準点に応じて左上をずらす() {
        assertEquals(new Point(100, 200), UIBuilder.anchoredTopLeft(Anchor.TOP_LEFT, 100, 200, 40, 20));
        assertEquals(new Point(80, 190), UIBuilder.anchoredTopLeft(Anchor.CENTER, 100, 200, 40, 20));
        assertEquals(new Point(60, 180), UIBuilder.anchoredTopLeft(Anchor.BOTTOM_RIGHT, 100, 200, 40, 20));
        assertEquals(new Point(80, 180), UIBuilder.anchoredTopLeft(Anchor.BOTTOM_CENTER, 100, 200, 40, 20));
    }

    @Test
    void 目標の倍率は状態で決まる() {
        assertEquals(ButtonAnimator.REST, ButtonAnimator.targetScale(true, false, false));
        assertEquals(ButtonAnimator.HOVER, ButtonAnimator.targetScale(true, true, false));
        assertEquals(ButtonAnimator.PRESS, ButtonAnimator.targetScale(true, true, true));
        assertEquals(ButtonAnimator.REST, ButtonAnimator.targetScale(false, true, true));
    }

    @Test
    void 倍率は目標へ近づき行き過ぎない() {
        double s = ButtonAnimator.REST;
        for (int i = 0; i < 60; i++) {
            double next = ButtonAnimator.approach(s, ButtonAnimator.HOVER, 0.016);
            assertTrue(next >= s && next <= ButtonAnimator.HOVER);
            s = next;
        }
        assertEquals(ButtonAnimator.HOVER, s, 0.01);
        assertEquals(ButtonAnimator.HOVER, ButtonAnimator.approach(0.5, ButtonAnimator.HOVER, 10), 1e-9);
    }

    @Test
    void コマ番号は時刻でループする() {
        assertEquals(0, ButtonAnimator.frameAt(0, 10, 4));
        assertEquals(3, ButtonAnimator.frameAt(0.35, 10, 4));
        assertEquals(0, ButtonAnimator.frameAt(0.4, 10, 4));
        assertEquals(0, ButtonAnimator.frameAt(5, 10, 0));
    }
}
