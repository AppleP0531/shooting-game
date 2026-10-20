package com.game.shooting_game.gameScene.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;

class FrozenStepTest {
    private static final double DT = 0.016;

    @Test
    @DisplayName("凍っていなければ、経過時間はそのまま")
    void notFrozen() {
        assertEquals(DT, ShipManager.frozenStep(DT, ShipKinds.ENEMY, 0), 1e-12);
        assertEquals(DT, ShipManager.frozenStep(DT, ShipKinds.BOSS, 0), 1e-12);
        assertEquals(DT, ShipManager.frozenStep(DT, ShipKinds.ENEMY, -1), 1e-12);
    }

    @Test
    @DisplayName("凍結中: 敵は止まり、ボスは半分の速さ、それ以外は変わらない")
    void frozen() {
        assertEquals(0, ShipManager.frozenStep(DT, ShipKinds.ENEMY, 2.0), 1e-12);
        assertEquals(DT * 0.5, ShipManager.frozenStep(DT, ShipKinds.BOSS, 0.1), 1e-12);
        assertEquals(DT, ShipManager.frozenStep(DT, ShipKinds.PLAYER, 2.0), 1e-12);
    }
}
