package com.game.shooting_game.lobbyScene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LobbySceneUIBarTest {
    @Test
    void 基準までは比例し超えた分は対数で伸びる() {
        assertEquals(0, LobbySceneUI.barRatio(-1), 1e-9);
        assertEquals(0.5, LobbySceneUI.barRatio(0.5), 1e-9);
        assertEquals(1.0, LobbySceneUI.barRatio(1.0), 1e-9);
        assertEquals(1.2, LobbySceneUI.barRatio(10), 1e-9); // 10倍で +0.2
        assertEquals(1.4, LobbySceneUI.barRatio(100), 1e-9);
        assertTrue(LobbySceneUI.barRatio(46281) > LobbySceneUI.barRatio(4628)); // 超えても増え続ける
    }
}
