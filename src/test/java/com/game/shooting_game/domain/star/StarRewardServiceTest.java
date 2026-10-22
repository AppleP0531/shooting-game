package com.game.shooting_game.domain.star;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;

class StarRewardServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.create("tester");
    }

    private void setStars(int n) {
        Map<String, Integer> hs = DataManager.getUserData().getProgress().getHighScores();
        hs.keySet().removeIf(k -> k.startsWith("mission:"));
        for (int i = 0; i < n; i++)
            hs.put("mission:stage1:EASY:" + i, 1); // イージーだけの記録(1つ=★1つ)
    }

    @Test
    @DisplayName("設定どおり interval は36、★0なら次の目標は36")
    void intervalAndTarget() {
        assertThat(StarRewardService.interval()).isEqualTo(36);
        assertThat(StarRewardService.nextTarget()).isEqualTo(36);
        setStars(72);
        assertThat(StarRewardService.milestoneCount()).isEqualTo(2);
        assertThat(StarRewardService.nextTarget()).isEqualTo(108);
    }

    @Test
    @DisplayName("表を超えた段は最後の段を繰り返す")
    void lastTierRepeats() {
        Reward first = StarRewardService.rewardOf(1);
        assertThat(first.coins()).isPositive();
        Reward last = StarRewardService.rewardOf(1000);
        assertThat(StarRewardService.rewardOf(1001)).isEqualTo(last);
        assertThat(last.coins()).isGreaterThanOrEqualTo(first.coins());
    }

    @Test
    @DisplayName("未達成は受け取れず、達成後は一度だけ受け取れる")
    void claimOnce() {
        assertThat(StarRewardService.canClaim(1)).isFalse();
        assertThat(StarRewardService.claim(1)).isNull();
        setStars(36);
        assertThat(StarRewardService.canClaim(1)).isTrue();
        assertThat(StarRewardService.claimableCount()).isEqualTo(1);
        int coins = DataManager.getUserData().getResources().getCoins();
        Reward r = StarRewardService.claim(1);
        assertThat(r).isNotNull();
        assertThat(DataManager.getUserData().getResources().getCoins()).isEqualTo(coins + r.coins());
        r.resources().forEach((k, v) -> assertThat(
                DataManager.getUserData().getResources().getEquipments().get(k)).isGreaterThanOrEqualTo(v));
        assertThat(StarRewardService.isClaimed(1)).isTrue();
        assertThat(StarRewardService.claim(1)).isNull();
        assertThat(DataManager.getUserData().getResources().getCoins()).isEqualTo(coins + r.coins());
        assertThat(StarRewardService.claimableCount()).isZero();
    }

    @Test
    @DisplayName("claimAll は受け取れる分の合計を返し、二度目は null")
    void claimAll() {
        assertThat(StarRewardService.claimAll()).isNull();
        setStars(108);
        int expected = StarRewardService.rewardOf(1).coins() + StarRewardService.rewardOf(2).coins()
                + StarRewardService.rewardOf(3).coins();
        Reward sum = StarRewardService.claimAll();
        assertThat(sum.coins()).isEqualTo(expected);
        assertThat(StarRewardService.claimableCount()).isZero();
        assertThat(StarRewardService.claimAll()).isNull();
    }

    @Test
    @DisplayName("どの段も研究スキップ券を含む(1段目・2段目)")
    void skipTicketTiers() {
        assertThat(StarRewardService.rewardOf(1).skipTickets()).isEqualTo(1);
        assertThat(StarRewardService.rewardOf(2).skipTickets()).isEqualTo(1);
        assertThat(StarRewardService.rewardOf(16).skipTickets()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("claim で研究スキップ券が増え、二重には受け取れない")
    void claimGivesSkipTicket() {
        setStars(72);
        int before = ShopService.owned(ItemKinds.SKIP_TICKET);
        Reward r1 = StarRewardService.claim(1);
        assertThat(r1.skipTickets()).isEqualTo(1);
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(before + 1);
        Reward r2 = StarRewardService.claim(2);
        assertThat(r2.skipTickets()).isEqualTo(1);
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(before + 2);
        assertThat(StarRewardService.claim(2)).isNull();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(before + 2);
    }

    @Test
    @DisplayName("claimAll は券も合計して受け取り、二度目は増えない")
    void claimAllGivesSkipTickets() {
        setStars(72);
        Reward sum = StarRewardService.claimAll();
        assertThat(sum.skipTickets()).isEqualTo(2);
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(2);
        assertThat(StarRewardService.claimAll()).isNull();
        assertThat(ShopService.owned(ItemKinds.SKIP_TICKET)).isEqualTo(2);
    }

    @Test
    @DisplayName("受取済みキーは★の数に影響しない")
    void claimKeyDoesNotChangeStars() {
        setStars(36);
        StarRewardService.claimAll();
        assertThat(StarRewardService.totalStars()).isEqualTo(36);
        assertThat(DataManager.getUserData().getProgress().getHighScores()).containsKey("starReward:1");
    }

    @Test
    @DisplayName("16段(★36〜576。★600まで)そろっていて、段ごとに報酬が違い、増えていく")
    void sixteenDistinctTiers() {
        for (int n = 1; n <= 16; n++)
            assertThat(StarRewardService.rewardOf(n).coins()).isGreaterThan(0);
        for (int n = 1; n < 16; n++) {
            assertThat(StarRewardService.rewardOf(n)).isNotEqualTo(StarRewardService.rewardOf(n + 1));
            assertThat(StarRewardService.rewardOf(n + 1).coins()).isGreaterThan(StarRewardService.rewardOf(n).coins());
        }
        // 表を超えた段は最後の段を繰り返す
        assertThat(StarRewardService.rewardOf(17)).isEqualTo(StarRewardService.rewardOf(16));
        // ★600までに受け取れる段の数
        setStars(600);
        assertThat(StarRewardService.milestoneCount()).isEqualTo(16);
    }
}
