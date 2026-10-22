package com.game.shooting_game.gameScene.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DamagePopupFormatTest {
    @Test
    void 小さい数はそのまま() {
        assertEquals("1", DamagePopupManager.format(0, false)); // 最低1
        assertEquals("29", DamagePopupManager.format(29.4, false));
        assertEquals("9999", DamagePopupManager.format(9999, false));
    }

    @Test
    void 大きい数は万億にまとめる() {
        assertEquals("1万", DamagePopupManager.format(10_000, false));
        assertEquals("22.7万", DamagePopupManager.format(227_426, false));
        assertEquals("231万", DamagePopupManager.format(2_314_060, false));
        assertEquals("1.2億", DamagePopupManager.format(120_000_000, false));
        assertEquals("1.5兆", DamagePopupManager.format(1_500_000_000_000L, false));
    }

    @Test
    void 会心は感嘆符が付く() {
        assertEquals("22.7万!", DamagePopupManager.format(227_426, true));
        assertEquals("5!", DamagePopupManager.format(5, true));
    }
}
