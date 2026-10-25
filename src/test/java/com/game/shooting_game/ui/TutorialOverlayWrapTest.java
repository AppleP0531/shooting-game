package com.game.shooting_game.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TutorialOverlayWrapTest {
    private static FontMetrics metrics() {
        var g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        return g.getFontMetrics(new Font("SansSerif", Font.PLAIN, 14));
    }

    @Test
    @DisplayName("幅に収まるように折り返し、元の文字は失われない")
    void wrapsWithoutLosingText() {
        FontMetrics fm = metrics();
        String text = "ステージ1は、操作を覚えるためのステージです。ゲームはいったん止まっています。";
        List<String> lines = TutorialOverlay.wrap(text, fm, 120);
        assertThat(lines.size()).isGreaterThan(1);
        for (String l : lines)
            assertThat(fm.stringWidth(l)).isLessThanOrEqualTo(120 + fm.getMaxAdvance());
        assertThat(String.join("", lines)).isEqualTo(text);
    }

    @Test
    @DisplayName("改行(\\n)は行を分け、空行も残る")
    void keepsNewlines() {
        List<String> lines = TutorialOverlay.wrap("あ\n\nい", metrics(), 500);
        assertThat(lines).containsExactly("あ", "", "い");
    }

    @Test
    @DisplayName("1文字も入らない幅でも、止まらずに1文字ずつ並べる")
    void tinyWidthTerminates() {
        List<String> lines = TutorialOverlay.wrap("あいう", metrics(), 1);
        assertThat(String.join("", lines)).isEqualTo("あいう");
    }
}
