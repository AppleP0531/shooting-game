package com.game.shooting_game.gameScene.ship;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;

class ShipFactoryTest {
    private Map<ShipKinds, Map<String, ShipDefinition>> config() {
        Map<ShipKinds, Map<String, ShipDefinition>> config = new HashMap<>();
        Map<String, ShipDefinition> enemies = new HashMap<>();
        enemies.put("Enemy1", new ShipDefinition());
        config.put(ShipKinds.ENEMY, enemies);
        return config;
    }

    @Test
    @DisplayName("設定にある船は「定義あり」になる")
    void definedShip() {
        assertTrue(ShipFactory.isDefined(config(), ShipKinds.ENEMY, "Enemy1"));
    }

    @Test
    @DisplayName("設定にないID・種類・null は「定義なし」になり、例外を出さない")
    void undefinedShip() {
        assertFalse(ShipFactory.isDefined(config(), ShipKinds.ENEMY, "NoSuchEnemy"));
        assertFalse(ShipFactory.isDefined(config(), ShipKinds.BOSS, "Enemy1"));
        assertFalse(ShipFactory.isDefined(config(), null, "Enemy1"));
        assertFalse(ShipFactory.isDefined(config(), ShipKinds.ENEMY, null));
        assertFalse(ShipFactory.isDefined(null, ShipKinds.ENEMY, "Enemy1"));
    }
}
