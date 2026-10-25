package com.game.shooting_game.domain.farm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;

class MaterialFarmServiceTest {
    @Test
    @DisplayName("ステージ番号と素材が、行き来できる")
    void stageIdRoundTrip() {
        for (ResourceName res : MaterialFarmService.MATERIALS) {
            int id = MaterialFarmService.stageId(res);
            assertThat(MaterialFarmService.isFarmStage(id)).isTrue();
            assertThat(MaterialFarmService.resourceOf(id)).isEqualTo(res);
        }
        assertThat(MaterialFarmService.isFarmStage(MaterialFarmService.STAGE_BASE - 1)).isFalse();
        assertThat(MaterialFarmService.resourceOf(1131)).isNull();
    }

    @Test
    @DisplayName("価値の高い素材ほど、解放が先になる")
    void unlockIncreases() {
        int prev = 0;
        for (ResourceName res : MaterialFarmService.MATERIALS) {
            int u = MaterialFarmService.unlockStage(res);
            assertThat(u).isGreaterThan(prev);
            prev = u;
        }
    }

    @Test
    @DisplayName("報酬はその素材だけで、価値が高いほど上のレア度も出る")
    void rewards() {
        var low = MaterialFarmService.rewards(ResourceName.GEAR);
        assertThat(low).containsOnlyKeys(ResourceName.GEAR);
        assertThat(low.get(ResourceName.GEAR)).containsOnlyKeys(Rarity.COMMON);

        var high = MaterialFarmService.rewards(ResourceName.PLASMA_CORE);
        assertThat(high).containsOnlyKeys(ResourceName.PLASMA_CORE);
        assertThat(high.get(ResourceName.PLASMA_CORE)).containsKeys(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE,
                Rarity.EPIC);
    }

    @Test
    @DisplayName("報酬の範囲は「最小~最大」で、最小は最大以下")
    void rangeFormat() {
        for (ResourceName res : MaterialFarmService.MATERIALS)
            for (String r : MaterialFarmService.rewards(res).get(res).values()) {
                String[] p = r.split("~");
                assertThat(p).hasSize(2);
                assertThat(Integer.parseInt(p[0])).isLessThanOrEqualTo(Integer.parseInt(p[1]));
            }
    }
}
