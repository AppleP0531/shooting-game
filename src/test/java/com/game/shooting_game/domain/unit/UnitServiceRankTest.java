package com.game.shooting_game.domain.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData.ShipStats;
import com.game.shooting_game.domain.gacha.PieceService;
import com.game.shooting_game.utils.GameBalance;

class UnitServiceRankTest {
    private static final ResourceKinds CORE = new ResourceKinds(ResourceName.RANK_CORE, Rarity.COMMON);
    private String shipId;
    private ShipStats stats;

    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.load("tester", "create");
        DataManager.setAdminMode(false);
        shipId = "rankTestShip";
        DataManager.getUserData().unlockShip(shipId);
        stats = DataManager.getUserData().getShips().get(shipId);
        stats.setLevel(UnitService.MAX_LEVEL);
        stats.setStars(0);
    }

    @AfterEach
    void tearDown() {
        DataManager.setAdminMode(false);
    }

    private void give(int cores, int pieces) {
        DataManager.getUserData().getResources().getEquipments().put(CORE, cores);
        if (pieces > 0)
            PieceService.add(shipId, pieces);
    }

    private int cores() {
        return DataManager.getUserData().getResources().getEquipments().getOrDefault(CORE, 0);
    }

    @Test
    @DisplayName("すべて足りていればランクアップでき、ランク+1・Lv1・素材とピースが減る")
    void rankUpSuccess() {
        int cost = UnitService.rankUpCost(0).items().get(CORE);
        int pieces = PieceService.rankUpPieces(0);
        give(cost + 3, pieces + 7);
        assertThat(UnitService.canRankUp(shipId)).isTrue();
        assertThat(UnitService.rankUp(shipId)).isTrue();
        assertThat(UnitService.getRank(shipId)).isEqualTo(1);
        assertThat(UnitService.getLevel(shipId)).isEqualTo(1);
        assertThat(cores()).isEqualTo(3);
        assertThat(PieceService.owned(shipId)).isEqualTo(7);
        assertThat(UnitService.rankText(shipId)).isEqualTo("Rank 1");
    }

    @Test
    @DisplayName("レベルが最大でなければランクアップできない")
    void levelNotMax() {
        give(1000, 5000);
        stats.setLevel(UnitService.MAX_LEVEL - 1);
        assertThat(UnitService.canRankUp(shipId)).isFalse();
        assertThat(UnitService.rankUp(shipId)).isFalse();
        assertThat(UnitService.getRank(shipId)).isZero();
    }

    @Test
    @DisplayName("ランクコアが足りなければランクアップできず、何も減らない")
    void notEnoughCores() {
        int cost = UnitService.rankUpCost(0).items().get(CORE);
        give(cost - 1, 5000);
        assertThat(UnitService.rankUp(shipId)).isFalse();
        assertThat(cores()).isEqualTo(cost - 1);
        assertThat(PieceService.owned(shipId)).isEqualTo(5000);
        assertThat(UnitService.getLevel(shipId)).isEqualTo(UnitService.MAX_LEVEL);
    }

    @Test
    @DisplayName("ピースが足りなければランクアップできず、何も減らない")
    void notEnoughPieces() {
        int pieces = PieceService.rankUpPieces(0);
        give(1000, pieces - 1);
        assertThat(UnitService.canRankUp(shipId)).isFalse();
        assertThat(UnitService.rankUp(shipId)).isFalse();
        assertThat(cores()).isEqualTo(1000);
        assertThat(PieceService.owned(shipId)).isEqualTo(pieces - 1);
    }

    @Test
    @DisplayName("最大ランクではランクアップできない")
    void maxRank() {
        stats.setStars(UnitService.MAX_RANK);
        give(100000, 100000);
        assertThat(UnitService.canRankUp(shipId)).isFalse();
        assertThat(UnitService.rankUp(shipId)).isFalse();
        assertThat(UnitService.getRank(shipId)).isEqualTo(UnitService.MAX_RANK);
    }

    @Test
    @DisplayName("ランクコアのコストはランクごとに2倍になる")
    void costDoubles() {
        assertThat(UnitService.rankUpCost(0).items().get(CORE)).isEqualTo(10);
        for (int r = 1; r < UnitService.MAX_RANK; r++)
            assertThat(UnitService.rankUpCost(r).items().get(CORE))
                    .isEqualTo(UnitService.rankUpCost(r - 1).items().get(CORE) * 2);
    }

    @Test
    @DisplayName("同じLvでもランクが1つ上がると火力が rankAttackGrowth 倍になる")
    void rankMultipliesAttack() {
        stats.setLevel(10);
        double r0 = UnitService.attackRate(shipId);
        stats.setStars(1);
        assertThat(UnitService.attackRate(shipId) / r0)
                .isCloseTo(GameBalance.rankAttackGrowth(), within(1e-9));
    }
}
