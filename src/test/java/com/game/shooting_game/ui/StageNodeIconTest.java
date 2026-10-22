package com.game.shooting_game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ui.StageNodeIcon.Look;

class StageNodeIconTest {
    private static int alpha(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xff;
    }

    private static boolean sameImage(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if (a.getRGB(x, y) != b.getRGB(x, y))
                    return false;
        return true;
    }

    @Test
    @DisplayName("台の見た目は、未解放>ボス>★3で金>通常の順で決まる")
    void lookOf() {
        assertEquals(Look.LOCKED, StageNodeIcon.lookOf(true, true, 3));
        assertEquals(Look.LOCKED, StageNodeIcon.lookOf(true, false, 0));
        assertEquals(Look.BOSS, StageNodeIcon.lookOf(false, true, 3));
        assertEquals(Look.BOSS, StageNodeIcon.lookOf(false, true, 0));
        assertEquals(Look.GOLD, StageNodeIcon.lookOf(false, false, 3));
        assertEquals(Look.NORMAL, StageNodeIcon.lookOf(false, false, 2));
        assertEquals(Look.NORMAL, StageNodeIcon.lookOf(false, false, 0));
    }

    @Test
    @DisplayName("絵は指定の大きさで、四隅は透明、台の中心付近は塗られる")
    void create() {
        BufferedImage image = StageNodeIcon.create(100, 115, "7", Look.NORMAL, 1);
        assertEquals(100, image.getWidth());
        assertEquals(115, image.getHeight());
        assertEquals(0, alpha(image, 0, 0));
        assertEquals(0, alpha(image, 99, 0));
        int cy = (int) (115 * StageNodeIcon.PLATFORM_CENTER);
        assertTrue(alpha(image, 50, cy) > 0);
    }

    @Test
    @DisplayName("未解放では★を描かない(★の数を変えても絵が変わらない)")
    void lockedHasNoStars() {
        BufferedImage none = StageNodeIcon.create(100, 115, "7", Look.LOCKED, 0);
        BufferedImage three = StageNodeIcon.create(100, 115, "7", Look.LOCKED, 3);
        assertTrue(sameImage(none, three));
    }

    @Test
    @DisplayName("解放済みは、★を取ると★の位置が金色で塗られる")
    void unlockedDrawsStars() {
        BufferedImage none = StageNodeIcon.create(100, 115, "7", Look.NORMAL, 0);
        BufferedImage three = StageNodeIcon.create(100, 115, "7", Look.NORMAL, 3);
        assertNotEquals(none.getRGB(50, 102), three.getRGB(50, 102));
        assertEquals(255, alpha(three, 50, 102));
        int rgb = three.getRGB(50, 102) & 0xffffff;
        assertEquals(0xffcd32, rgb);
    }

    @Test
    @DisplayName("ナイトメアをクリアした台は、★がシルバーで塗られる")
    void silverStars() {
        BufferedImage gold = StageNodeIcon.create(100, 115, "7", Look.NORMAL, 3, false);
        BufferedImage silver = StageNodeIcon.create(100, 115, "7", Look.NORMAL, 3, true);
        assertEquals(0xffcd32, gold.getRGB(50, 102) & 0xffffff);
        assertEquals(0xd6e0f0, silver.getRGB(50, 102) & 0xffffff);
        // 引数を省いた版は金
        assertTrue(sameImage(gold, StageNodeIcon.create(100, 115, "7", Look.NORMAL, 3)));
    }

    @Test
    @DisplayName("ボスと通常で台の縁の色が違う(台の右端の縁)")
    void bossDiffersFromNormal() {
        BufferedImage normal = StageNodeIcon.create(100, 115, "5", Look.NORMAL, 0);
        BufferedImage boss = StageNodeIcon.create(100, 115, "5", Look.BOSS, 0);
        int cy = (int) (115 * StageNodeIcon.PLATFORM_CENTER);
        assertNotEquals(normal.getRGB(91, cy), boss.getRGB(91, cy));
        // 通常は青寄り、ボスは赤寄り
        int n = normal.getRGB(91, cy), b = boss.getRGB(91, cy);
        assertTrue((n & 0xff) > ((n >> 16) & 0xff));
        assertTrue(((b >> 16) & 0xff) > (b & 0xff));
    }

    @Test
    @DisplayName("星は中心から半径の範囲に収まる5つ角の形")
    void star() {
        Path2D star = StageNodeIcon.star(50, 50, 10);
        assertTrue(star.contains(50, 50));
        assertTrue(!star.contains(50, 38));
        assertTrue(star.getBounds2D().getHeight() <= 20.0001);
        assertTrue(star.getBounds2D().getWidth() <= 20.0001);
    }
}
