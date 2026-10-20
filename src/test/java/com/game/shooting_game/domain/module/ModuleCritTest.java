package com.game.shooting_game.domain.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.DamageCalculator;

class ModuleCritTest {
    @BeforeEach
    void setUp() {
        DataManager.setAdminMode(false);
        DataManager.create("tester");
    }

    @AfterEach
    void tearDown() {
        DataManager.setAdminMode(false);
    }

    @Test
    @DisplayName("会心の能力は割合で、ラベルが付いている")
    void critStats() {
        assertThat(ModuleStat.CRIT_RATE.isPercent()).isTrue();
        assertThat(ModuleStat.CRIT_DMG.isPercent()).isTrue();
        assertThat(ModuleStat.CRIT_RATE.getLabel()).isEqualTo("会心率");
        assertThat(ModuleStat.CRIT_DMG.getLabel()).isEqualTo("会心ダメージ");
        assertThat(ModuleStat.CRIT_RATE.format(0.05)).isEqualTo("+5.0%");
    }

    @Test
    @DisplayName("CRIT / CRITDMG のメイン能力が会心率 / 会心ダメージ")
    void critKinds() {
        assertThat(ModuleKind.CRIT.getMainStat()).isEqualTo(ModuleStat.CRIT_RATE);
        assertThat(ModuleKind.CRITDMG.getMainStat()).isEqualTo(ModuleStat.CRIT_DMG);
        assertThat(ModuleKind.CRIT.getId()).isEqualTo("crit_mod");
        assertThat(ModuleKind.CRITDMG.getId()).isEqualTo("critdmg_mod");
    }

    @Test
    @DisplayName("メイン能力の値はレア度が上がるほど増える(全種類)")
    void mainValueGrowsWithRarity() {
        for (ModuleKind k : ModuleKind.values()) {
            double prev = 0;
            for (Rarity r : Rarity.values()) {
                double v = ModuleService.mainValue(k, r);
                assertThat(v).as(k + " " + r).isGreaterThan(prev);
                prev = v;
            }
        }
        assertThat(ModuleService.mainValue(ModuleKind.CRIT, Rarity.COMMON)).isCloseTo(0.04 * 0.5, within(1e-9));
        assertThat(ModuleService.mainValue(ModuleKind.CRITDMG, Rarity.COMMON)).isCloseTo(0.04 * 1.5, within(1e-9));
    }


    @Test
    @DisplayName("装着した会心モジュールが、会心率・会心ダメージに足される(Lv最大)")
    void equippedAddsToCalculator() {
        assertThat(DamageCalculator.critRate()).isCloseTo(DamageCalculator.BASE_CRIT_RATE, within(1e-9));
        assertThat(DamageCalculator.critMult()).isCloseTo(DamageCalculator.BASE_CRIT_MULT, within(1e-9));

        Module a = ModuleService.addNew(ModuleKind.CRIT, Rarity.LEGENDARY);
        Module b = ModuleService.addNew(ModuleKind.CRITDMG, Rarity.LEGENDARY);
        a.setLevel(ModuleService.MAX_LEVEL);
        b.setLevel(ModuleService.MAX_LEVEL);
        assertThat(ModuleService.equip(a.getId())).isTrue();
        assertThat(ModuleService.equip(b.getId())).isTrue();

        assertThat(DamageCalculator.critRate())
                .isCloseTo(DamageCalculator.BASE_CRIT_RATE + ModuleService.mainValue(ModuleKind.CRIT, Rarity.LEGENDARY),
                        within(1e-9));
        assertThat(DamageCalculator.critMult())
                .isCloseTo(DamageCalculator.BASE_CRIT_MULT
                        + ModuleService.mainValue(ModuleKind.CRITDMG, Rarity.LEGENDARY), within(1e-9));
    }

    @Test
    @DisplayName("会心率は 0〜1 に収まる")
    void critRateClamped() {
        Module a = ModuleService.addNew(ModuleKind.CRIT, Rarity.LEGENDARY);
        a.setLevel(ModuleService.MAX_LEVEL);
        a.setMainValue(5.0); // 極端な値
        ModuleService.equip(a.getId());
        assertThat(DamageCalculator.critRate()).isEqualTo(1.0);
        a.setMainValue(-5.0);
        assertThat(DamageCalculator.critRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("会心の基礎値")
    void baseConstants() {
        assertThat(DamageCalculator.BASE_CRIT_RATE).isEqualTo(0.05);
        assertThat(DamageCalculator.BASE_CRIT_MULT).isEqualTo(1.5);
    }
}
