package com.game.shooting_game.domain.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;

class UnitServiceSetLevelTest {
    private static final String SHIP = "setLevelTestShip";

    @BeforeEach
    void setUp() {
        DataManager.load("tester", "create");
        DataManager.getUserData().unlockShip(SHIP);
    }

    @Test
    @DisplayName("setLevel は、1〜最大レベルの範囲に収めて設定する")
    void clampsToRange() {
        assertThat(UnitService.setLevel(SHIP, 30)).isEqualTo(30);
        assertThat(UnitService.getLevel(SHIP)).isEqualTo(30);
        assertThat(UnitService.setLevel(SHIP, 999)).isEqualTo(UnitService.MAX_LEVEL);
        assertThat(UnitService.setLevel(SHIP, -5)).isEqualTo(1);
        assertThat(UnitService.getLevel(SHIP)).isEqualTo(1);
    }

    @Test
    @DisplayName("持っていない機体は -1 を返し、何も作らない")
    void notOwned() {
        assertThat(UnitService.setLevel("noSuchShip", 10)).isEqualTo(-1);
        assertThat(DataManager.getUserData().getShips()).doesNotContainKey("noSuchShip");
    }
}
