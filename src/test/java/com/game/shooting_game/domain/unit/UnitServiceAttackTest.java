package com.game.shooting_game.domain.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData.ShipStats;
import com.game.shooting_game.utils.GameBalance;

class UnitServiceAttackTest {
    private String shipId;
    private ShipStats stats;

    @BeforeEach
    void setUp() {
        DataManager.load("tester", "create");
        shipId = "attackTestShip";
        DataManager.getUserData().unlockShip(shipId);
        stats = DataManager.getUserData().getShips().get(shipId);
    }

    @Test
    @DisplayName("Lv1・★0 の火力は 1.0")
    void baseIsOne() {
        stats.setLevel(1);
        stats.setStars(0);
        assertThat(UnitService.attackRate(shipId)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("レベルを上げると火力は指数的に増える(Lv50 は 1.10^49 付近)")
    void levelGrowsExponentially() {
        stats.setStars(0);
        stats.setLevel(50);
        double lv50 = UnitService.attackRate(shipId);
        assertThat(lv50).isCloseTo(Math.pow(1.10, 49), within(1e-6 * lv50));
        assertThat(lv50).isBetween(90.0, 130.0);

        double prev = 0;
        for (int lv = 1; lv <= 50; lv++) {
            stats.setLevel(lv);
            double v = UnitService.attackRate(shipId);
            assertThat(v).isGreaterThan(prev);
            if (lv > 1)
                assertThat(v / prev).isCloseTo(GameBalance.unitAttackGrowth(), within(1e-9));
            prev = v;
        }
    }

    @Test
    @DisplayName("★は足し算で効く(Lv1 で★ごとに +attackPerStar)")
    void starsAreAdditive() {
        stats.setLevel(1);
        for (int s = 0; s <= 5; s++) {
            stats.setStars(s);
            assertThat(UnitService.attackRate(shipId))
                    .isCloseTo(1 + GameBalance.attackPerStar() * s, within(1e-9));
        }
        stats.setLevel(20);
        stats.setStars(0);
        double base = UnitService.attackRate(shipId);
        stats.setStars(4);
        assertThat(UnitService.attackRate(shipId))
                .isCloseTo(base * (1 + GameBalance.attackPerStar() * 4), within(1e-9));
    }
}
