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
    @DisplayName("Lv1・ランク0 の火力は 1.0")
    void baseIsOne() {
        stats.setLevel(1);
        stats.setStars(0);
        assertThat(UnitService.attackRate(shipId)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("レベルを上げると火力は指数的に増える(Lv50 は unitAttackGrowth^49 付近)")
    void levelGrowsExponentially() {
        stats.setStars(0);
        stats.setLevel(50);
        double lv50 = UnitService.attackRate(shipId);
        assertThat(lv50).isCloseTo(Math.pow(GameBalance.unitAttackGrowth(), 49), within(1e-6 * lv50));
        assertThat(lv50).isBetween(3.5, 5.0);

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
    @DisplayName("ランクは掛け算で効く(ランクごとに rankAttackGrowth 倍)")
    void ranksAreMultiplicative() {
        stats.setLevel(1);
        for (int r = 0; r <= 5; r++) {
            stats.setStars(r);
            double expected = Math.pow(GameBalance.rankAttackGrowth(), r);
            assertThat(UnitService.attackRate(shipId)).isCloseTo(expected, within(1e-6 * expected));
        }
        stats.setLevel(20);
        stats.setStars(0);
        double base = UnitService.attackRate(shipId);
        stats.setStars(4);
        double expected = base * Math.pow(GameBalance.rankAttackGrowth(), 4);
        assertThat(UnitService.attackRate(shipId)).isCloseTo(expected, within(1e-6 * expected));
    }

    @Test
    @DisplayName("最大強化(Lv50・ランク5)の火力倍率は約4,000(基礎ダメージ10 × 火力スキル最大2倍で、1撃が約8万)")
    void maxedAttackAboutEightyThousandPerHit() {
        stats.setLevel(50);
        stats.setStars(5);
        double rate = UnitService.attackRate(shipId);
        assertThat(rate).isBetween(3_800.0, 4_200.0);
        assertThat(10 * 2.0 * rate).isBetween(76_000.0, 84_000.0);
    }
}
