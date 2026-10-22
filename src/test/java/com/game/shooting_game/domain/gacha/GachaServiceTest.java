package com.game.shooting_game.domain.gacha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.gacha.GachaService.Result;

/** pull / unlock の成功系は ShipManager(画面に依存)を使うので、ここでは純関数と、設定を読まずに終わる分岐だけを対象にする */
class GachaServiceTest {
    private static Map<String, Integer> costs() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Ship2", 10000);
        m.put("Ship3", 50000);
        m.put("Ship4", 1000000);
        return m;
    }

    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.load("tester", "create");
        DataManager.setAdminMode(false);
    }

    @Test
    @DisplayName("pickPieces は 1/2/3/5 のどれかで、すべて出る")
    void pickPieces() {
        Random rnd = new Random(1234);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            int p = GachaService.pickPieces(rnd);
            assertThat(p).isIn(1, 2, 3, 5);
            seen.add(p);
        }
        assertThat(seen).containsExactlyInAnyOrder(1, 2, 3, 5);
    }

    @Test
    @DisplayName("roll は指定回数ぶんの結果を返し、機体は候補の中から選ばれる")
    void rollCount() {
        Random rnd = new Random(7);
        assertThat(GachaService.roll(rnd, costs(), 1)).hasSize(1);
        List<Result> ten = GachaService.roll(rnd, costs(), 10);
        assertThat(ten).hasSize(10);
        assertThat(ten).allSatisfy(r -> {
            assertThat(costs()).containsKey(r.shipId());
            assertThat(r.pieces()).isIn(1, 2, 3, 5);
        });
    }

    @Test
    @DisplayName("10連は3個以上が最低1回は出る")
    void tenGuarantee() {
        for (long seed = 0; seed < 300; seed++) {
            List<Result> ten = GachaService.roll(new Random(seed), costs(), 10);
            assertThat(ten).as("seed " + seed).anyMatch(r -> r.pieces() >= GachaService.TEN_GUARANTEE_PIECES);
        }
    }

    @Test
    @DisplayName("weight は入手コインが高いほど小さい")
    void weightDecreases() {
        double prev = Double.MAX_VALUE;
        for (int c : new int[] { 0, 1000, 10000, 50000, 1000000, 20000000 }) {
            double w = GachaService.weight(c);
            assertThat(w).isPositive().isLessThan(prev);
            prev = w;
        }
    }

    @Test
    @DisplayName("pickShip はコインの低い機体を多く選ぶ")
    void pickShipPrefersCheap() {
        Random rnd = new Random(42);
        Map<String, Integer> count = new HashMap<>();
        for (int i = 0; i < 20000; i++)
            count.merge(GachaService.pickShip(rnd, costs()), 1, Integer::sum);
        assertThat(count.get("Ship2")).isGreaterThan(count.get("Ship3"));
        assertThat(count.get("Ship3")).isGreaterThan(count.get("Ship4"));
        assertThat(count.get("Ship4")).isPositive();
    }

    @Test
    @DisplayName("所持済みの機体は解放できない")
    void cannotUnlockOwned() {
        PieceService.add("Ship1", 100);
        assertThat(GachaService.canUnlock("Ship1")).isFalse();
        assertThat(GachaService.unlock("Ship1")).isFalse();
        assertThat(PieceService.owned("Ship1")).isEqualTo(100);
    }

    @Test
    @DisplayName("ピースが足りなければ解放できず、ピースは減らない")
    void cannotUnlockWithoutPieces() {
        DataManager.getUserData().getResources().setCoins(100_000_000);
        PieceService.add("Ship2", PieceService.UNLOCK_PIECES - 1);
        assertThat(GachaService.canUnlock("Ship2")).isFalse();
        assertThat(GachaService.unlock("Ship2")).isFalse();
        assertThat(PieceService.owned("Ship2")).isEqualTo(PieceService.UNLOCK_PIECES - 1);
        assertThat(DataManager.getUserData().getShips()).doesNotContainKey("Ship2");
    }
}
