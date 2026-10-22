package com.game.shooting_game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UIBuilder.hueShift(色相ずらし)の純ロジック
 */
class UIBuilderHueShiftTest {
    private static BufferedImage one(int argb) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, argb);
        return img;
    }

    private static float[] hsb(int argb) {
        return Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
    }

    @Test
    @DisplayName("透明は透明のまま")
    void transparent() {
        assertEquals(0, UIBuilder.hueShift(one(0), 0.5f).getRGB(0, 0));
    }

    @Test
    @DisplayName("灰色・白は変わらない")
    void grey() {
        int g = 0xFF808080;
        assertEquals(g, UIBuilder.hueShift(one(g), 0.6f).getRGB(0, 0));
        int w = 0xFFFFFFFF;
        assertEquals(w, UIBuilder.hueShift(one(w), 0.3f).getRGB(0, 0));
    }

    @Test
    @DisplayName("色のついた画素は色相が変わり、透明度と明るさは保たれる")
    void coloured() {
        int red = 0x80FF0000; // alpha 0x80
        int out = UIBuilder.hueShift(one(red), 2f / 3f).getRGB(0, 0); // 青
        assertEquals(0x80, out >>> 24);
        float[] a = hsb(red), b = hsb(out);
        assertTrue(Math.abs(a[0] - b[0]) > 0.3f, "hue " + a[0] + " -> " + b[0]);
        assertEquals(a[2], b[2], 0.02f);
        assertEquals(a[1], b[1], 0.02f);
        assertEquals(2f / 3f, b[0], 0.02f);
    }

    @Test
    @DisplayName("大きさは変わらない")
    void size() {
        BufferedImage img = new BufferedImage(7, 3, BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = UIBuilder.hueShift(img, 0.1f);
        assertEquals(7, out.getWidth());
        assertEquals(3, out.getHeight());
    }
}
