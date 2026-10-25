package com.game.shooting_game.utils.animation;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnimationClampTest {
    private static BufferedImage[] frames(int n) {
        BufferedImage[] f = new BufferedImage[n];
        for (int i = 0; i < n; i++)
            f[i] = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        return f;
    }

    @Test
    @DisplayName("コマ数より大きい番号を返すロジックでも、最後のコマを返して止まらない")
    void clampsToLastFrame() {
        Animation a = new Animation(frames(1), 8, 0.1);
        a.setCustomLogic((current, anim) -> 5); // 設定のコマ数より多い番号(ダミー画像などで起きる)
        a.update(1.0);
        assertThat(a.getCurrentImage()).isNotNull();
    }

    @Test
    @DisplayName("負の番号でも、最初のコマを返す")
    void clampsNegative() {
        Animation a = new Animation(frames(3), 8, 0.1);
        a.setCustomLogic((current, anim) -> -2);
        a.update(1.0);
        assertThat(a.getCurrentImage()).isNotNull();
    }

    @Test
    @DisplayName("画像がないときは null")
    void noImages() {
        assertThat(new Animation(new BufferedImage[0], 8, 0.1).getCurrentImage()).isNull();
    }
}
