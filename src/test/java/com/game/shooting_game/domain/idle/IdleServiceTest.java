package com.game.shooting_game.domain.idle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.idle.IdleService.PerRun;
import com.game.shooting_game.domain.star.StarRewardService.Reward;

class IdleServiceTest {
    private static final ResourceKinds GEAR = new ResourceKinds(ResourceName.GEAR, Rarity.COMMON);

    @BeforeEach
    void setUp() {
        DataManager.create("tester");
    }

    private static PerRun per(double coins, double gear, double runsPerHour) {
        Map<ResourceKinds, Double> res = new LinkedHashMap<>();
        res.put(GEAR, gear);
        return new PerRun(coins, res, runsPerHour);
    }

    @Test
    @DisplayName("範囲の平均(読めなければ0)")
    void averageOf() {
        assertThat(IdleService.averageOf("3~6")).isEqualTo(4.5);
        assertThat(IdleService.averageOf("5~5")).isEqualTo(5.0);
        assertThat(IdleService.averageOf("x")).isZero();
        assertThat(IdleService.averageOf(null)).isZero();
    }

    @Test
    @DisplayName("放置の時間は、上限で止まり、負は0")
    void cappedSeconds() {
        assertThat(IdleService.cappedSeconds(-5)).isZero();
        assertThat(IdleService.cappedSeconds(100)).isEqualTo(100);
        assertThat(IdleService.cappedSeconds(10L * 24 * 3600)).isEqualTo(IdleService.MAX_SECONDS);
    }

    @Test
    @DisplayName("たまる量は、時間 × 1時間あたりの周回数 × 効率 に比例し、小数点以下は切り捨て")
    void computeProportional() {
        // 1時間・1時間に10周・効率30% → 3周ぶん
        Reward r = IdleService.compute(3600, per(100, 4, 10));
        assertThat(r.coins()).isEqualTo((int) Math.floor(100 * 10 * IdleService.EFFICIENCY));
        assertThat(r.resources().get(GEAR)).isEqualTo((int) Math.floor(4 * 10 * IdleService.EFFICIENCY));
        // 2倍の時間なら、2倍
        Reward r2 = IdleService.compute(7200, per(100, 4, 10));
        assertThat(r2.coins()).isEqualTo((int) Math.floor(100 * 20 * IdleService.EFFICIENCY));
    }

    @Test
    @DisplayName("上限を超えた分はたまらない。時間が0なら何もない")
    void computeCapped() {
        Reward atCap = IdleService.compute(IdleService.MAX_SECONDS, per(100, 4, 10));
        Reward beyond = IdleService.compute(IdleService.MAX_SECONDS * 5L, per(100, 4, 10));
        assertThat(beyond).isEqualTo(atCap);
        Reward none = IdleService.compute(0, per(100, 4, 10));
        assertThat(none.coins()).isZero();
        assertThat(none.resources()).isEmpty();
    }

    @Test
    @DisplayName("1時間あたりの周回数は、ウェーブが多いほど減る")
    void runsPerHour() {
        assertThat(IdleService.runsPerHour(12)).isCloseTo(3600.0 / (12 * IdleService.SECONDS_PER_WAVE), within(1e-9));
        assertThat(IdleService.runsPerHour(20)).isLessThan(IdleService.runsPerHour(12));
        assertThat(IdleService.runsPerHour(0)).isGreaterThan(0); // 0ウェーブでも割り算で壊れない
    }

    @Test
    @DisplayName("放置していないときは、何もたまらず、受け取れない")
    void notIdle() {
        assertThat(IdleService.stage()).isZero();
        assertThat(IdleService.elapsed(1000)).isZero();
        assertThat(IdleService.pending(1000).coins()).isZero();
        assertThat(IdleService.claim(1000)).isNull();
    }

    @Test
    @DisplayName("クリアしていない・範囲外のステージは選べない")
    void cannotSelect() {
        assertThat(IdleService.canSelect(0)).isFalse();
        assertThat(IdleService.canSelect(201)).isFalse();
        assertThat(IdleService.canSelect(1)).isFalse(); // 新規データは未クリア
        assertThat(IdleService.select(1, 100)).isFalse();
        assertThat(IdleService.stage()).isZero();
    }
}
