package com.game.shooting_game.domain.star;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.star.StarRewardService.Reward;

class StarRewardServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.create("tester");
    }

    private void setStars(int n) {
        Map<String, Integer> hs = DataManager.getUserData().getProgress().getHighScores();
        hs.keySet().removeIf(k -> k.startsWith("mission:"));
        for (int i = 0; i < n; i++)
            hs.put("mission:stage1:HARD:" + i, 1);
    }

    @Test
    @DisplayName("設定どおり interval は12、★0なら次の目標は12")
    void intervalAndTarget() {
        assertThat(StarRewardService.interval()).isEqualTo(12);
        assertThat(StarRewardService.nextTarget()).isEqualTo(12);
        setStars(24);
        assertThat(StarRewardService.milestoneCount()).isEqualTo(2);
        assertThat(StarRewardService.nextTarget()).isEqualTo(36);
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
        setStars(12);
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
        setStars(36);
        int expected = StarRewardService.rewardOf(1).coins() + StarRewardService.rewardOf(2).coins()
                + StarRewardService.rewardOf(3).coins();
        Reward sum = StarRewardService.claimAll();
        assertThat(sum.coins()).isEqualTo(expected);
        assertThat(StarRewardService.claimableCount()).isZero();
        assertThat(StarRewardService.claimAll()).isNull();
    }

    @Test
    @DisplayName("受取済みキーは★の数に影響しない")
    void claimKeyDoesNotChangeStars() {
        setStars(24);
        StarRewardService.claimAll();
        assertThat(StarRewardService.totalStars()).isEqualTo(24);
        assertThat(DataManager.getUserData().getProgress().getHighScores()).containsKey("starReward:1");
    }
}
