package com.game.shooting_game.domain.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.module.Module.Sub;

class ModuleSubRatioTest {
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
    @DisplayName("subRatio は 0.25〜0.75 を 6 個に等間隔で分け、範囲外は丸まる")
    void subRatioValues() {
        assertThat(ModuleService.SUB_CANDIDATES).isEqualTo(6);
        assertThat(ModuleService.subRatio(0)).isCloseTo(0.25, within(1e-9));
        assertThat(ModuleService.subRatio(5)).isCloseTo(0.75, within(1e-9));
        double step = ModuleService.subRatio(1) - ModuleService.subRatio(0);
        assertThat(step).isCloseTo(0.1, within(1e-9));
        for (int i = 1; i < ModuleService.SUB_CANDIDATES; i++)
            assertThat(ModuleService.subRatio(i) - ModuleService.subRatio(i - 1)).isCloseTo(step, within(1e-9));
        assertThat(ModuleService.subRatio(-3)).isCloseTo(0.25, within(1e-9));
        assertThat(ModuleService.subRatio(99)).isCloseTo(0.75, within(1e-9));
    }

    @Test
    @DisplayName("Lv20 までに付いたサブ能力の値は、基準値 × subRatio(i) のどれか")
    @SuppressWarnings("unchecked")
    void subValuesAreCandidates() throws Exception {
        Field f = ModuleService.class.getDeclaredField("SUB_TABLE");
        f.setAccessible(true);
        Map<ModuleStat, double[]> table = (Map<ModuleStat, double[]>) f.get(null);

        DataManager.setAdminMode(true); // コストを無視してレベルを上げる
        for (int trial = 0; trial < 30; trial++) {
            for (Rarity r : Rarity.values()) {
                Module m = ModuleService.addNew(ModuleKind.ATC, r);
                while (m.getLevel() < ModuleService.MAX_LEVEL)
                    assertThat(ModuleService.levelUp(m.getId())).isTrue();
                assertThat(m.getSubs()).hasSize(ModuleService.SUB_COUNT);
                for (Sub s : m.getSubs()) {
                    assertThat(s.getStat()).isNotEqualTo(m.getKind().getMainStat());
                    double base = table.get(s.getStat())[r.ordinal()];
                    boolean match = false;
                    for (int i = 0; i < ModuleService.SUB_CANDIDATES; i++) {
                        double raw = base * ModuleService.subRatio(i);
                        double expected = Math.round(raw * 1000) / 1000.0;
                        // 割合の能力は、念のためメインの0.95倍で頭打ちになる
                        double capped = s.getStat().isPercent() ? Math.min(raw, m.getMainValue() * 0.95) : raw;
                        double expectedCapped = Math.round(capped * 1000) / 1000.0;
                        if (Math.abs(s.getValue() - expected) < 1e-9
                                || Math.abs(s.getValue() - expectedCapped) < 1e-9)
                            match = true;
                    }
                    assertThat(match).as(r + " " + s.getStat() + " = " + s.getValue()).isTrue();
                }
            }
        }
    }
}
