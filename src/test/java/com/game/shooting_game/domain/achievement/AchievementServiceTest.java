package com.game.shooting_game.domain.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.achievement.AchievementService.Achievement;
import com.game.shooting_game.domain.achievement.AchievementService.Tier;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;

class AchievementServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.create("tester");
    }

    private static Achievement kills() {
        return AchievementService.all().stream().filter(a -> a.id().equals("kills")).findFirst().orElseThrow();
    }

    private static int coins() {
        return DataManager.getUserData().getResources().getCoins();
    }

    @Test
    @DisplayName("設定: id が重複せず、metric は stat:/derived: で始まる")
    void configIdsAndMetrics() {
        assertThat(AchievementService.all()).isNotEmpty();
        Set<String> ids = new HashSet<>();
        for (Achievement a : AchievementService.all()) {
            assertThat(ids.add(a.id())).as("id 重複: " + a.id()).isTrue();
            assertThat(a.metric()).matches("^(stat|derived):.+");
        }
    }

    @Test
    @DisplayName("設定: 各実績に段があり、goal は正で昇順、報酬のコインは0以上")
    void configTiers() {
        for (Achievement a : AchievementService.all()) {
            assertThat(a.tiers()).as(a.id()).isNotEmpty();
            int prev = 0;
            for (Tier t : a.tiers()) {
                assertThat(t.goal()).as(a.id()).isPositive().isGreaterThan(prev);
                prev = t.goal();
                assertThat(t.reward().coins()).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    @DisplayName("add で累計が増え、0以下は無視される")
    void addAndValue() {
        assertThat(AchievementService.value(AchievementService.KILLS)).isZero();
        AchievementService.add(AchievementService.KILLS, 5);
        AchievementService.add(AchievementService.KILLS, 3);
        assertThat(AchievementService.value(AchievementService.KILLS)).isEqualTo(8);
        AchievementService.add(AchievementService.KILLS, 0);
        AchievementService.add(AchievementService.KILLS, -4);
        assertThat(AchievementService.value(kills())).isEqualTo(8);
    }

    @Test
    @DisplayName("届いた段・受取済みの段・canClaim・nextTier")
    void tierState() {
        Achievement a = kills();
        assertThat(AchievementService.reachedTiers(a)).isZero();
        assertThat(AchievementService.canClaim(a)).isFalse();
        assertThat(AchievementService.nextTier(a)).isEqualTo(a.tiers().get(0));
        assertThat(AchievementService.claim(a)).isNull();

        AchievementService.add(AchievementService.KILLS, a.tiers().get(0).goal());
        assertThat(AchievementService.reachedTiers(a)).isEqualTo(1);
        assertThat(AchievementService.claimedTiers(a)).isZero();
        assertThat(AchievementService.canClaim(a)).isTrue();

        AchievementService.claim(a);
        assertThat(AchievementService.claimedTiers(a)).isEqualTo(1);
        assertThat(AchievementService.canClaim(a)).isFalse();
        assertThat(AchievementService.nextTier(a)).isEqualTo(a.tiers().get(1));
    }

    @Test
    @DisplayName("claim で報酬が所持に入り、二重には受け取れない")
    void claimOnce() {
        Achievement a = kills();
        AchievementService.add(AchievementService.KILLS, a.tiers().get(0).goal());
        int before = coins();
        Reward r = AchievementService.claim(a);
        assertThat(r).isNotNull();
        assertThat(r).isEqualTo(a.tiers().get(0).reward());
        assertThat(coins()).isEqualTo(before + r.coins());
        r.resources().forEach((k, v) -> assertThat(
                DataManager.getUserData().getResources().getEquipments().get(k)).isGreaterThanOrEqualTo(v));
        assertThat(AchievementService.claim(a)).isNull();
        assertThat(coins()).isEqualTo(before + r.coins());
    }

    @Test
    @DisplayName("段をまたいで受け取ると合計になり、スキップ券も入る")
    void claimMultipleTiers() {
        Achievement a = kills();
        int n = 3;
        AchievementService.add(AchievementService.KILLS, a.tiers().get(n - 1).goal());
        int expectedCoins = 0;
        int expectedTickets = 0;
        for (int i = 0; i < n; i++) {
            expectedCoins += a.tiers().get(i).reward().coins();
            expectedTickets += a.tiers().get(i).reward().skipTickets();
        }
        assertThat(expectedTickets).isPositive();
        int before = coins();
        int ticketsBefore = ShopService.owned(ItemKinds.SKIP_TICKET);
        assertThat(AchievementService.reachedTiers(a)).isEqualTo(n);

        Reward sum = AchievementService.claim(a);
        assertThat(sum.coins()).isEqualTo(expectedCoins);
        assertThat(sum.skipTickets()).isEqualTo(expectedTickets);
        assertThat(coins()).isEqualTo(before + expectedCoins);
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(ticketsBefore + expectedTickets);
        assertThat(DataManager.getUserData().getProgress().getHighScores().get("inv:SKIP_TICKET"))
                .isEqualTo(ticketsBefore + expectedTickets);
        assertThat(AchievementService.claimedTiers(a)).isEqualTo(n);
        assertThat(AchievementService.claim(a)).isNull();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(ticketsBefore + expectedTickets);
    }

    @Test
    @DisplayName("段ごとに分けて受け取っても、合計は同じ")
    void claimInSteps() {
        Achievement a = kills();
        int before = coins();
        AchievementService.add(AchievementService.KILLS, a.tiers().get(0).goal());
        Reward r1 = AchievementService.claim(a);
        AchievementService.add(AchievementService.KILLS, a.tiers().get(1).goal());
        Reward r2 = AchievementService.claim(a);
        assertThat(r2).isEqualTo(a.tiers().get(1).reward());
        assertThat(coins()).isEqualTo(before + r1.coins() + r2.coins());
    }

    @Test
    @DisplayName("claimableCount は受け取れる実績の数")
    void claimableCount() {
        assertThat(AchievementService.claimableCount()).isZero();
        Achievement a = kills();
        AchievementService.add(AchievementService.KILLS, a.tiers().get(0).goal());
        assertThat(AchievementService.claimableCount()).isEqualTo(1);
        AchievementService.add(AchievementService.BOSS_KILLS, 1);
        assertThat(AchievementService.claimableCount()).isEqualTo(2);
        AchievementService.claim(a);
        assertThat(AchievementService.claimableCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("derived:cleared は clearedStage に追従する")
    void derivedCleared() {
        Achievement a = AchievementService.all().stream().filter(x -> x.metric().equals("derived:cleared"))
                .findFirst().orElseThrow();
        assertThat(AchievementService.value(a)).isZero();
        DataManager.getUserData().getProgress().setClearedStage(a.tiers().get(0).goal());
        assertThat(AchievementService.value(a)).isEqualTo(a.tiers().get(0).goal());
        assertThat(AchievementService.reachedTiers(a)).isEqualTo(1);
        assertThat(AchievementService.canClaim(a)).isTrue();
    }
}
