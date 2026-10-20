package com.game.shooting_game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StageLockIconTest {
    private static int alpha(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xff;
    }

    @Test
    @DisplayName("番号つきの四角は指定の大きさで、中は塗られ、四隅は透明")
    void placeholder() {
        BufferedImage image = StageLockIcon.placeholder(50, 50, "3");
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
        assertEquals(0, alpha(image, 0, 0)); // 角丸の外側
        assertTrue(alpha(image, 8, 25) > 0); // 中は塗られている
    }

    @Test
    @DisplayName("南京錠は指定の大きさで、背景は透明、中央付近に絵がある")
    void padlock() {
        BufferedImage image = StageLockIcon.padlock(30);
        assertEquals(30, image.getWidth());
        assertEquals(0, alpha(image, 0, 0));
        assertTrue(alpha(image, 15, 22) > 0); // 本体
    }

    @Test
    @DisplayName("鍵を重ねたアイコンは、基のアイコンが暗くなり、中央に鍵が載る")
    void overlay() {
        BufferedImage base = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 40; y++)
            for (int x = 0; x < 40; x++)
                base.setRGB(x, y, new Color(255, 255, 255, 255).getRGB());
        BufferedImage lock = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++)
                lock.setRGB(x, y, new Color(255, 0, 0, 255).getRGB());

        BufferedImage result = StageLockIcon.overlay(base, lock, 40, 40);

        assertEquals(40, result.getWidth());
        // 基のアイコンの部分は不透明度が下がる(暗くなる)
        assertTrue(alpha(result, 2, 2) < 255);
        assertTrue(alpha(result, 2, 2) > 0);
        // 中央は鍵の色(赤)がそのまま載る
        assertEquals(new Color(255, 0, 0, 255).getRGB(), result.getRGB(20, 20));
        assertNotEquals(base.getRGB(2, 2), result.getRGB(2, 2));
    }
}
