package com.game.shooting_game.domain.subship;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.subship.SubShipTypeService.TypeDef;

class SubShipTypeServiceTest {
    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
    }

    @AfterEach
    void tearDown() {
        DataManager.setAdminMode(false);
    }

    private static TypeDef type(String id) {
        return SubShipTypeService.types().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("標準 + 追加3種の合計4タイプが読め、標準は最初から使える")
    void types() {
        assertThat(SubShipTypeService.types()).extracting(TypeDef::id)
                .containsExactly("STANDARD", "GUNNER", "LANCER", "SEEKER");
        assertThat(SubShipTypeService.isUnlocked(type("STANDARD"))).isTrue();
        assertThat(SubShipTypeService.current().id()).isEqualTo("STANDARD");
        assertThat(type("SEEKER").at(1).tracking()).isPositive();
    }

    @Test
    @DisplayName("未解放のタイプは選べず、コインを払うと解放して選べる")
    void unlockAndSelect() {
        TypeDef g = type("GUNNER");
        assertThat(SubShipTypeService.select(g)).isFalse();
        DataManager.getUserData().getResources().setCoins(g.unlockCoin() - 1);
        assertThat(SubShipTypeService.unlock(g)).isFalse();

        DataManager.getUserData().getResources().setCoins(g.unlockCoin() + 5);
        assertThat(SubShipTypeService.unlock(g)).isTrue();
        assertThat(DataManager.getUserData().getResources().getCoins()).isEqualTo(5);
        assertThat(SubShipTypeService.select(g)).isTrue();
        assertThat(SubShipTypeService.current()).isEqualTo(g);
    }

    @Test
    @DisplayName("管理者モードはコインを払わずに解放できる")
    void adminUnlock() {
        DataManager.setAdminMode(true);
        DataManager.getUserData().getResources().setCoins(0);
        assertThat(SubShipTypeService.unlock(type("LANCER"))).isTrue();
        assertThat(DataManager.getUserData().getResources().getCoins()).isZero();
    }

    @Test
    @DisplayName("全タイプが10レベルあり、強化しても撃つ弾の数は減らない(種類は増えるだけ)")
    void levelTables() {
        for (TypeDef t : SubShipTypeService.types()) {
            assertThat(t.maxLevel()).as(t.id()).isEqualTo(10);
            for (int lv = 2; lv <= t.maxLevel(); lv++) {
                var prev = t.at(lv - 1);
                var cur = t.at(lv);
                assertThat(cur.straight()).as(t.id() + " Lv" + lv + " 通常弾").isGreaterThanOrEqualTo(prev.straight());
                assertThat(cur.tracking()).as(t.id() + " Lv" + lv + " 追尾弾").isGreaterThanOrEqualTo(prev.tracking());
                if (prev.beam() != null)
                    assertThat(cur.beam()).as(t.id() + " Lv" + lv + " ビーム").isNotNull();
            }
            // 最大レベルでは Lv1 より弾が増えている
            int first = t.at(1).straight() + t.at(1).tracking();
            int last = t.at(10).straight() + t.at(10).tracking();
            assertThat(last).as(t.id()).isGreaterThan(first);
        }
    }

    @Test
    @DisplayName("タイプごとの特徴: ガンナーは連射、ランサーはビーム、シーカーは追尾弾が先に出る")
    void typeCharacter() {
        assertThat(type("GUNNER").at(1).interval()).isLessThan(type("STANDARD").at(1).interval());
        assertThat(type("LANCER").at(1).damage()).isGreaterThan(type("STANDARD").at(1).damage());
        assertThat(type("LANCER").at(5).beam()).isNotNull();
        assertThat(type("SEEKER").at(1).tracking()).isPositive();
        assertThat(type("SEEKER").at(1).straight()).isZero();
        assertThat(type("STANDARD").at(10).beam()).isNotNull();
    }

    @Test
    @DisplayName("レベルはタイプごとに別々で、標準は最初からLv1")
    void levelPerType() {
        TypeDef std = SubShipTypeService.types().get(0);
        assertThat(SubShipService.level(std)).isEqualTo(1);
        DataManager.getUserData().getSubShipParts().put("LV:GUNNER", 4);
        assertThat(SubShipService.level(type("GUNNER"))).isEqualTo(4);
        assertThat(SubShipService.level(std)).isEqualTo(1);
        // 範囲外の値は丸める
        DataManager.getUserData().getSubShipParts().put("LV:GUNNER", 99);
        assertThat(SubShipService.level(type("GUNNER"))).isEqualTo(10);
    }

    @Test
    @DisplayName("強化にはコインと素材が要り、払うとレベルが上がる。最大レベルと未解放では強化できない")
    void upgrade() {
        TypeDef std = SubShipTypeService.types().get(0);
        var cost = SubShipService.getCost(std);
        assertThat(cost.coins()).isEqualTo(std.upgradeCoin());
        assertThat(cost.items()).isNotEmpty();

        DataManager.getUserData().getResources().setCoins(0);
        assertThat(SubShipService.upgrade(std)).isFalse();

        DataManager.setAdminMode(true); // 管理者モードは無制限
        assertThat(SubShipService.upgrade(std)).isTrue();
        assertThat(SubShipService.level(std)).isEqualTo(2);
        for (int i = 0; i < 20; i++)
            SubShipService.upgrade(std);
        assertThat(SubShipService.level(std)).isEqualTo(10);
        assertThat(SubShipService.getCost(std)).isNull();
        assertThat(SubShipService.isMaxLevel(std)).isTrue();

        DataManager.setAdminMode(false);
        assertThat(SubShipService.upgrade(type("LANCER"))).isFalse(); // 未解放
    }

    @Test
    @DisplayName("強化コストはレベルが上がるほど増える")
    void costGrows() {
        TypeDef std = SubShipTypeService.types().get(0);
        DataManager.getUserData().getSubShipParts().put("LV:STANDARD", 1);
        int c1 = SubShipService.getCost(std).coins();
        DataManager.getUserData().getSubShipParts().put("LV:STANDARD", 5);
        int c5 = SubShipService.getCost(std).coins();
        assertThat(c5).isGreaterThan(c1);
    }

    @Test
    @DisplayName("前後のタイプへ循環して移れる")
    void neighbor() {
        TypeDef first = SubShipTypeService.types().get(0);
        assertThat(SubShipTypeService.neighbor(first, -1).id()).isEqualTo("SEEKER");
        assertThat(SubShipTypeService.neighbor(type("SEEKER"), 1)).isEqualTo(first);
    }
}
