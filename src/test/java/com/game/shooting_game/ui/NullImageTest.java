package com.game.shooting_game.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ui.NullImage.Kind;

class NullImageTest {
    @Test
    @DisplayName("どの要素でも、ダミー画像が描ける(正方形で、透明ではない)")
    void renderAll() {
        for (Kind k : Kind.values()) {
            BufferedImage im = NullImage.render(k);
            assertThat(im.getWidth()).isEqualTo(im.getHeight());
            assertThat(im.getRGB(3, 3) >>> 24).as(k + " の角").isEqualTo(255);
        }
    }

    @Test
    @DisplayName("要素ごとに色が違う")
    void colorsDiffer() {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (Kind k : Kind.values())
            colors.add(NullImage.render(k).getRGB(0, 0));
        assertThat(colors).hasSize(Kind.values().length);
    }

    @Test
    @DisplayName("frames は、指定のコマ数ぶん(0以下でも1枚)の同じ絵を返す")
    void frames() {
        assertThat(NullImage.frames(Kind.ENEMY, 12)).hasSize(12).doesNotContainNull();
        assertThat(NullImage.frames(Kind.ENEMY, 0)).hasSize(1);
        BufferedImage[] f = NullImage.frames(Kind.BULLET, 4);
        assertThat(f[0]).isSameAs(f[3]);
    }
}
