package com.game.shooting_game.upgradeScene.subPanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class SkillIconComposerMaxTest {
    private static final BufferedImage GLYPH = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

    private static boolean same(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if (a.getRGB(x, y) != b.getRGB(x, y))
                    return false;
        return true;
    }

    @Test
    void 最大レベル1のスキルも取れば金の枠になる() {
        BufferedImage got = SkillIconComposer.compose(GLYPH, 1, 1, false);
        BufferedImage none = SkillIconComposer.compose(GLYPH, 0, 1, false);
        assertEquals(got.getWidth(), none.getWidth());
        assertEquals(false, same(got, none)); // 未取得とは別の見た目(金・MAX)
        // 最大レベル10の途中の見た目とも違う(金の枠)
        assertEquals(false, same(got, SkillIconComposer.compose(GLYPH, 1, 10, false)));
    }

    @Test
    void 根のボタンはレベルに関わらず見た目が変わらない() {
        assertEquals(true, same(SkillIconComposer.compose(GLYPH, 1, 1, true),
                SkillIconComposer.compose(GLYPH, 0, 1, true)));
    }
}
