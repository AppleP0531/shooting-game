package com.game.shooting_game.domain.trial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.data.UserData.ShipStats;

class TrialServiceTest {
    private static final java.util.Set<String> KNOWN = java.util.Set.of("Ship1", "Ship2", "Ship3"); // 設定を読まずに使う船の一覧

    @BeforeEach
    void setUp() {
        TrialService.end();
        DataManager.create("tester");
    }

    @AfterEach
    void tearDown() {
        TrialService.end();
    }

    private static ShipStats stats(int level, int stars) {
        ShipStats s = new ShipStats();
        s.setLevel(level);
        s.setStars(stars);
        return s;
    }

    @Test
    @DisplayName("持っている船のうち、一番高いレベルとランクを選ぶ(別々の船でもよい)")
    void bestLevelAndRank() {
        Map<String, ShipStats> ships = new LinkedHashMap<>();
        ships.put("Ship1", stats(12, 0));
        ships.put("Ship2", stats(7, 3));
        assertThat(TrialService.bestLevelAndRank(ships)).containsExactly(12, 3);
        assertThat(TrialService.bestLevelAndRank(new LinkedHashMap<>())).containsExactly(1, 0);
    }

    @Test
    @DisplayName("お試しの船を始めると、選択中の船になり、終わると消えてもとに戻る")
    void beginAndEnd() {
        UserData ud = DataManager.getUserData();
        ud.getShips().get("Ship1").setLevel(20);
        ud.getShips().get("Ship1").setStars(2);
        String before = ud.getSelectedShip();

        assertThat(TrialService.canTry("Ship2", KNOWN)).isTrue();
        assertThat(TrialService.begin("Ship2", KNOWN)).isTrue();
        assertThat(TrialService.isActive()).isTrue();
        assertThat(TrialService.trialShip()).isEqualTo("Ship2");
        assertThat(ud.getSelectedShip()).isEqualTo("Ship2");
        // レベル・ランクは、持っている船で一番高いものに合う。スキル強化はなし
        assertThat(ud.getShips().get("Ship2").getLevel()).isEqualTo(20);
        assertThat(ud.getShips().get("Ship2").getStars()).isEqualTo(2);
        assertThat(ud.getShips().get("Ship2").getUpgrades()).isEmpty();

        TrialService.end();
        assertThat(TrialService.isActive()).isFalse();
        assertThat(TrialService.trialShip()).isNull();
        assertThat(ud.getShips()).doesNotContainKey("Ship2");
        assertThat(ud.getSelectedShip()).isEqualTo(before);
    }

    @Test
    @DisplayName("持っている船・存在しない船・お試し中は、お試しできない")
    void cannotTry() {
        assertThat(TrialService.canTry("Ship1", KNOWN)).isFalse(); // 持っている
        assertThat(TrialService.canTry("NoSuchShip", KNOWN)).isFalse();
        assertThat(TrialService.canTry(null, KNOWN)).isFalse();
        assertThat(TrialService.begin("Ship1", KNOWN)).isFalse();

        assertThat(TrialService.begin("Ship2", KNOWN)).isTrue();
        assertThat(TrialService.canTry("Ship3", KNOWN)).isFalse(); // お試し中は次を始められない
        assertThat(TrialService.begin("Ship3", KNOWN)).isFalse();
    }

    @Test
    @DisplayName("end は何度呼んでもよく、お試し中でなければ何も変えない")
    void endIsIdempotent() {
        UserData ud = DataManager.getUserData();
        String before = ud.getSelectedShip();
        TrialService.end();
        TrialService.end();
        assertThat(ud.getSelectedShip()).isEqualTo(before);
        assertThat(ud.getShips()).containsKey("Ship1");
    }

    @Test
    @DisplayName("お試し中は、実績の累計が増えない")
    void achievementsNotCounted() {
        var kills = com.game.shooting_game.domain.achievement.AchievementService.KILLS;
        com.game.shooting_game.domain.achievement.AchievementService.add(kills, 1);
        int before = com.game.shooting_game.domain.achievement.AchievementService.value(kills);

        assertThat(TrialService.begin("Ship2", KNOWN)).isTrue();
        com.game.shooting_game.domain.achievement.AchievementService.add(kills, 5);
        TrialService.end();

        assertThat(com.game.shooting_game.domain.achievement.AchievementService.value(kills)).isEqualTo(before);
        com.game.shooting_game.domain.achievement.AchievementService.add(kills, 2);
        assertThat(com.game.shooting_game.domain.achievement.AchievementService.value(kills)).isEqualTo(before + 2);
    }
}
