package com.game.shooting_game.domain.module;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleSubTableTest {
    @Test
    @DisplayName("SUB_TABLE にすべての ModuleStat が入っていて、レア度ごとに増える")
    @SuppressWarnings("unchecked")
    void subTableCoversAllStats() throws Exception {
        Field f = ModuleService.class.getDeclaredField("SUB_TABLE");
        f.setAccessible(true);
        Map<ModuleStat, double[]> table = (Map<ModuleStat, double[]>) f.get(null);
        assertThat(table).containsKeys(ModuleStat.values());
        for (var e : table.entrySet()) {
            double[] v = e.getValue();
            assertThat(v).as(e.getKey().name()).hasSize(5);
            for (int i = 1; i < v.length; i++)
                assertThat(v[i]).as(e.getKey().name()).isGreaterThan(v[i - 1]);
        }
    }
}
