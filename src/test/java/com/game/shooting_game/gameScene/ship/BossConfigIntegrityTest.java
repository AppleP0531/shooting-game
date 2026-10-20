package com.game.shooting_game.gameScene.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.gameScene.bullet.barrage.BarrageSpec;
import com.game.shooting_game.gameScene.ship.BossLibrary.BossDefinition;
import com.game.shooting_game.gameScene.ship.BossLibrary.GaugeDefinition;

/**
 * BossConfig / BulletConfig / StageConfig / ShipConfig の整合性を、JSONを直接読んで確認する
 */
class BossConfigIntegrityTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode readTree(String name) throws Exception {
        try (InputStream is = BossConfigIntegrityTest.class.getResourceAsStream("/static/configs/" + name)) {
            assertNotNull(is, name);
            return MAPPER.readTree(is);
        }
    }

    private static Map<String, BossDefinition> bosses() throws Exception {
        try (InputStream is = BossConfigIntegrityTest.class.getResourceAsStream("/static/configs/BossConfig.json")) {
            return MAPPER.readValue(is, new TypeReference<Map<String, BossDefinition>>() {
            });
        }
    }

    @Test
    @DisplayName("Boss7/8/9 は5ゲージで、各ゲージに弾幕がある")
    void newBossesHaveFiveGauges() throws Exception {
        Map<String, BossDefinition> map = bosses();
        for (String id : new String[] { "Boss7", "Boss8", "Boss9" }) {
            BossDefinition def = map.get(id);
            assertNotNull(def, id);
            assertEquals(5, def.getGauges().size(), id);
            for (GaugeDefinition g : def.getGauges())
                assertTrue(!g.getBarrages().isEmpty(), id + " has an empty gauge");
        }
    }

    @Test
    @DisplayName("すべてのボスの弾幕が使う弾の種類・IDが BulletConfig の BOSS にある(無いIDを撃つと止まる)")
    void barrageBulletsExist() throws Exception {
        JsonNode bossBullets = readTree("BulletConfig.json").get("BOSS");
        assertNotNull(bossBullets);
        for (Map.Entry<String, BossDefinition> e : bosses().entrySet()) {
            for (GaugeDefinition g : e.getValue().getGauges()) {
                for (BarrageSpec b : g.getBarrages()) {
                    String where = e.getKey() + " " + b.getType() + " " + b.getBulletKind() + "#" + b.getBulletId();
                    JsonNode kind = bossBullets.get(b.getBulletKind().name());
                    assertNotNull(kind, where);
                    assertNotNull(kind.get(String.valueOf(b.getBulletId())), where);
                }
            }
        }
    }

    @Test
    @DisplayName("ステージで使う Boss ID が ShipConfig にあり、新ステージが追加されている")
    void stageBossesExist() throws Exception {
        JsonNode stages = readTree("StageConfig.json");
        JsonNode ships = readTree("ShipConfig.json");
        Map<String, BossDefinition> defs = bosses();

        stages.fields().forEachRemaining(st -> {
            JsonNode waves = st.getValue().get("enemyList");
            if (waves == null)
                return;
            waves.fields().forEachRemaining(w -> w.getValue().forEach(en -> {
                String id = en.get("enemyId").asText();
                if (id.startsWith("Boss")) {
                    assertTrue(containsKey(ships, id), st.getKey() + ": " + id + " not in ShipConfig");
                    assertTrue(defs.containsKey(id), st.getKey() + ": " + id + " not in BossConfig");
                }
            }));
        });

        for (String stage : new String[] { "stage157", "stage158", "stage159" })
            assertEquals("BOSS", stages.get(stage).get("category").asText(), stage);
    }

    /** ShipConfig はトップ階層か1段下にIDを持つ */
    private static boolean containsKey(JsonNode ships, String id) {
        if (ships.has(id))
            return true;
        for (JsonNode child : ships)
            if (child.isObject() && child.has(id))
                return true;
        return false;
    }
}
