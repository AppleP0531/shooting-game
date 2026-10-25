package com.game.shooting_game.gameScene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.DamageCalculator.DurableAbsorb;

class DurableAbsorbTest {
    @Test
    @DisplayName("耐久が足りれば、全部吸って残りは0")
    void enough() {
        DurableAbsorb r = DamageCalculator.absorbDurable(100, 30);
        assertEquals(70, r.durability(), 1e-12);
        assertEquals(0, r.restDamage(), 1e-12);
    }

    @Test
    @DisplayName("ちょうど尽きる")
    void exact() {
        DurableAbsorb r = DamageCalculator.absorbDurable(100, 100);
        assertEquals(0, r.durability(), 1e-12);
        assertEquals(0, r.restDamage(), 1e-12);
    }

    @Test
    @DisplayName("足りなければ、耐久は0になり残りが回る")
    void short_() {
        DurableAbsorb r = DamageCalculator.absorbDurable(40, 100);
        assertEquals(0, r.durability(), 1e-12);
        assertEquals(60, r.restDamage(), 1e-12);
    }

    @Test
    @DisplayName("耐久が0なら、全ダメージが素通り")
    void none() {
        DurableAbsorb r = DamageCalculator.absorbDurable(0, 25);
        assertEquals(0, r.durability(), 1e-12);
        assertEquals(25, r.restDamage(), 1e-12);
    }

    @Test
    @DisplayName("ダメージが0なら、耐久は減らない")
    void zeroDamage() {
        DurableAbsorb r = DamageCalculator.absorbDurable(50, 0);
        assertEquals(50, r.durability(), 1e-12);
        assertEquals(0, r.restDamage(), 1e-12);
    }

    @Test
    @DisplayName("続けて吸うと累積して減り、尽きたあとは素通り")
    void sequence() {
        DurableAbsorb a = DamageCalculator.absorbDurable(50, 30);
        DurableAbsorb b = DamageCalculator.absorbDurable(a.durability(), 30);
        DurableAbsorb c = DamageCalculator.absorbDurable(b.durability(), 30);
        assertEquals(20, a.durability(), 1e-12);
        assertEquals(0, b.durability(), 1e-12);
        assertEquals(10, b.restDamage(), 1e-12);
        assertEquals(30, c.restDamage(), 1e-12);
    }
}
