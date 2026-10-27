package com.game.shooting_game.gameScene.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;

class CoinBossMemoryTest {
    @BeforeEach
    void setUp() {
        CoinBossMemory.reset();
    }

    @Test
    void はじめは満タンで撤収の残りを覚える() {
        assertEquals(1.0, CoinBossMemory.remainingRatio(), 1e-9);
        CoinBossMemory.save(0.4);
        assertEquals(0.4, CoinBossMemory.remainingRatio(), 1e-9);
        CoinBossMemory.save(0.1); // 削るほど減る(次の撤収でさらに覚える)
        assertEquals(0.1, CoinBossMemory.remainingRatio(), 1e-9);
    }

    @Test
    void 割合は0より大きく1までに収める() {
        CoinBossMemory.save(0.5);
        CoinBossMemory.save(0); // 0以下は覚えない(倒れたことになる)
        assertEquals(0.5, CoinBossMemory.remainingRatio(), 1e-9);
        CoinBossMemory.save(3);
        assertEquals(1.0, CoinBossMemory.remainingRatio(), 1e-9);
    }

    @Test
    void 倒すと覚えを消して特殊ドロップを溜める() {
        CoinBossMemory.save(0.3);
        CoinBossMemory.defeated(20);
        assertEquals(1.0, CoinBossMemory.remainingRatio(), 1e-9);
        CoinBossMemory.defeated(60);
        var drops = CoinBossMemory.takeSpecialDrops();
        assertEquals(2, drops.size());
        assertEquals(Rarity.EPIC, drops.get(0));
        assertEquals(Rarity.LEGENDARY, drops.get(1));
        assertTrue(CoinBossMemory.takeSpecialDrops().isEmpty()); // 受け取ると空
    }

    @Test
    void 挑戦が変わると全部消える() {
        CoinBossMemory.save(0.2);
        CoinBossMemory.defeated(10);
        CoinBossMemory.reset();
        assertEquals(1.0, CoinBossMemory.remainingRatio(), 1e-9);
        assertTrue(CoinBossMemory.takeSpecialDrops().isEmpty());
    }

    @Test
    void レア度はウェーブで決まる() {
        assertEquals(Rarity.EPIC, CoinBossMemory.rarityFor(1));
        assertEquals(Rarity.EPIC, CoinBossMemory.rarityFor(CoinBossMemory.LEGENDARY_WAVE - 1));
        assertEquals(Rarity.LEGENDARY, CoinBossMemory.rarityFor(CoinBossMemory.LEGENDARY_WAVE));
    }
}
