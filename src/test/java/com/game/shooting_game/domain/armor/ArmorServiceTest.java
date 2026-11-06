package com.game.shooting_game.domain.armor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.armor.ArmorService.Type;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.unit.UnitService;

class ArmorServiceTest {

    @BeforeEach
    void setUp() {
        DataManager.load("tester", "create");
    }

    @Test
    @DisplayName("clampLevel は 0〜最大レベルに収める")
    void clampLevelRange() {
        assertThat(ArmorService.clampLevel(-5)).isEqualTo(0);
        assertThat(ArmorService.clampLevel(0)).isEqualTo(0);
        assertThat(ArmorService.clampLevel(1)).isEqualTo(1);
        assertThat(ArmorService.clampLevel(ArmorService.MAX_LEVEL)).isEqualTo(ArmorService.MAX_LEVEL);
        assertThat(ArmorService.clampLevel(999)).isEqualTo(ArmorService.MAX_LEVEL);
    }

    @Test
    @DisplayName("★の数は、レベルを STAR_STEP で割った数(境界)")
    void starsBoundaries() {
        assertThat(ArmorService.stars(1)).isEqualTo(0);
        assertThat(ArmorService.stars(3)).isEqualTo(0);
        assertThat(ArmorService.stars(4)).isEqualTo(1);
        assertThat(ArmorService.stars(7)).isEqualTo(1);
        assertThat(ArmorService.stars(8)).isEqualTo(2);
        assertThat(ArmorService.stars(20)).isEqualTo(5);
        assertThat(ArmorService.stars(999)).isEqualTo(ArmorService.MAX_STARS);
        assertThat(ArmorService.MAX_STARS).isEqualTo(5);
    }

    @Test
    @DisplayName("value はレベルに比例し、その装甲にない能力は 0")
    void valueIsProportional() {
        double perLv = Type.DEFENSE.perLevel().get(ModuleStat.HP_PCT);
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.HP_PCT, 1)).isCloseTo(perLv, within(1e-9));
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.HP_PCT, 10)).isCloseTo(perLv * 10, within(1e-9));
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.HP_PCT, 20)).isCloseTo(perLv * 20, within(1e-9));
        // 範囲外のレベルは収める
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.HP_PCT, 999)).isCloseTo(perLv * 20, within(1e-9));
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.HP_PCT, 0)).isEqualTo(0.0);
        // 火力装甲にない能力
        assertThat(ArmorService.value(Type.FIREPOWER, ModuleStat.HP_PCT, 10)).isEqualTo(0.0);
        assertThat(ArmorService.value(Type.DEFENSE, ModuleStat.ATK_PCT, 10)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Type.perLevel は、能力の宣言順に並び、決めた値を持つ")
    void perLevelContents() {
        assertThat(Type.DEFENSE.perLevel().keySet())
                .containsExactlyInAnyOrder(ModuleStat.HP_PCT, ModuleStat.DEF_PCT, ModuleStat.SHIELD_PCT);
        assertThat(Type.DEFENSE.perLevel().get(ModuleStat.HP_PCT)).isEqualTo(0.04);
        assertThat(Type.DEFENSE.perLevel().get(ModuleStat.DEF_PCT)).isEqualTo(0.01);
        assertThat(Type.DEFENSE.perLevel().get(ModuleStat.SHIELD_PCT)).isEqualTo(0.04);

        assertThat(Type.FIREPOWER.perLevel().keySet())
                .containsExactlyInAnyOrder(ModuleStat.ATK_PCT, ModuleStat.CRIT_RATE, ModuleStat.CRIT_DMG);
        assertThat(Type.FIREPOWER.perLevel().get(ModuleStat.ATK_PCT)).isEqualTo(0.05);
        assertThat(Type.FIREPOWER.perLevel().get(ModuleStat.CRIT_RATE)).isEqualTo(0.005);
        assertThat(Type.FIREPOWER.perLevel().get(ModuleStat.CRIT_DMG)).isEqualTo(0.02);

        for (Type t : Type.values()) {
            List<ModuleStat> order = new ArrayList<>();
            for (ModuleStat s : ModuleStat.values())
                if (t.perLevel().containsKey(s))
                    order.add(s);
            assertThat(new ArrayList<>(t.perLevel().keySet())).as(t.name()).isEqualTo(order);
            assertThat(t.title()).isNotBlank();
            assertThat(t.detail()).isNotBlank();
        }
    }

    @Test
    @DisplayName("levelCost は、機体のレベルのコストを2倍のレベル相当で使い、上限を超えない")
    void levelCostMatchesUnit() {
        assertThat(ArmorService.levelCost(1)).isEqualTo(UnitService.levelCost(2));
        assertThat(ArmorService.levelCost(5)).isEqualTo(UnitService.levelCost(10));
        assertThat(ArmorService.levelCost(ArmorService.MAX_LEVEL))
                .isEqualTo(UnitService.levelCost(Math.min(UnitService.MAX_LEVEL, ArmorService.MAX_LEVEL * 2)));
        // 範囲外は収める
        assertThat(ArmorService.levelCost(999)).isEqualTo(ArmorService.levelCost(ArmorService.MAX_LEVEL));
        assertThat(ArmorService.levelCost(-3)).isEqualTo(ArmorService.levelCost(0));
    }

    @Test
    @DisplayName("未保存のレベルは0(未強化)で、setLevel は範囲に収めて保存する")
    void setLevelAndLevel() {
        for (Type t : Type.values())
            assertThat(ArmorService.level(t)).isEqualTo(0);

        assertThat(ArmorService.setLevel(Type.DEFENSE, 7)).isEqualTo(7);
        assertThat(ArmorService.level(Type.DEFENSE)).isEqualTo(7);
        assertThat(ArmorService.stars(Type.DEFENSE)).isEqualTo(1);
        assertThat(ArmorService.level(Type.FIREPOWER)).isEqualTo(0); // 種類ごとに別

        assertThat(ArmorService.setLevel(Type.DEFENSE, 999)).isEqualTo(ArmorService.MAX_LEVEL);
        assertThat(ArmorService.level(Type.DEFENSE)).isEqualTo(ArmorService.MAX_LEVEL);
        assertThat(ArmorService.setLevel(Type.DEFENSE, -5)).isEqualTo(0);
        assertThat(ArmorService.level(Type.DEFENSE)).isEqualTo(0);
    }

    @Test
    @DisplayName("stat は、全種類の装甲の効果の合計")
    void statSumsTypes() {
        ArmorService.setLevel(Type.DEFENSE, 10);
        ArmorService.setLevel(Type.FIREPOWER, 20);
        assertThat(ArmorService.stat(ModuleStat.HP_PCT)).isCloseTo(0.04 * 10, within(1e-9));
        assertThat(ArmorService.stat(ModuleStat.ATK_PCT)).isCloseTo(0.05 * 20, within(1e-9));
        assertThat(ArmorService.stat(ModuleStat.CRIT_RATE)).isCloseTo(0.005 * 20, within(1e-9));
        // 装甲にない能力は 0
        for (ModuleStat s : ModuleStat.values()) {
            if (!Type.DEFENSE.perLevel().containsKey(s) && !Type.FIREPOWER.perLevel().containsKey(s))
                assertThat(ArmorService.stat(s)).as(s.name()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("最大レベルでは levelUp できず、レベルは変わらない")
    void levelUpAtMax() {
        ArmorService.setLevel(Type.FIREPOWER, ArmorService.MAX_LEVEL);
        assertThat(ArmorService.levelUp(Type.FIREPOWER)).isFalse();
        assertThat(ArmorService.level(Type.FIREPOWER)).isEqualTo(ArmorService.MAX_LEVEL);
    }
}
