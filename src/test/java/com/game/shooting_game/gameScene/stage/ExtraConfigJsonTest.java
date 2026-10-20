package com.game.shooting_game.gameScene.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 追加した設定ファイル(ステージボタン・追加の敵/ボス・ステージID)の整合性
 */
class ExtraConfigJsonTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RES = "src/main/resources/static/";

    private static JsonNode read(String name) throws Exception {
        return MAPPER.readTree(new File(RES + "configs/" + name));
    }

    @Test
    @DisplayName("StageBtnConfig は 1〜200 が重複なしで並ぶ")
    void stageBtn() throws Exception {
        JsonNode n = read("StageBtnConfig.json");
        assertTrue(n.isArray());
        Set<Integer> ids = new HashSet<>();
        for (JsonNode e : n) {
            assertTrue(ids.add(e.get("id").asInt()), "dup " + e.get("id"));
            assertEquals(2, e.get("pos").size());
        }
        assertEquals(200, ids.size());
        for (int i = 1; i <= 200; i++)
            assertTrue(ids.contains(i), "missing " + i);
    }

    @Test
    @DisplayName("ShipConfig_Enemy2 の敵は ShipConfig と重ならず、datas と画像がある")
    void enemy2() throws Exception {
        JsonNode base = read("ShipConfig.json").get("ENEMY");
        JsonNode extra = read("ShipConfig_Enemy2.json").get("ENEMY");
        assertEquals(14, extra.size());
        Iterator<String> it = extra.fieldNames();
        while (it.hasNext()) {
            String id = it.next();
            assertFalse(base.has(id), "collision " + id);
            JsonNode datas = extra.get(id).get("datas");
            assertTrue(datas != null && !datas.isNull(), id + " datas");
            String img = datas.get("imagePath").asText();
            assertTrue(new File(RES + "img/ship" + img.replace(".png", "_blue.png")).exists(), id + " image " + img);
            assertTrue(datas.get("hp").asInt() > 0, id);
            if (datas.has("hue")) {
                double h = datas.get("hue").asDouble();
                assertTrue(h >= 0 && h <= 1, id + " hue");
            }
        }
    }

    @Test
    @DisplayName("BossConfig_Extra は Boss10〜19、ゲージ3つ以上、ShipConfig_Boss2 と対応し BossConfig と重ならない")
    void bossExtra() throws Exception {
        JsonNode extra = read("BossConfig_Extra.json");
        JsonNode ships = read("ShipConfig_Boss2.json").get("BOSS");
        JsonNode baseBoss = read("BossConfig.json");
        JsonNode baseShip = read("ShipConfig.json").get("BOSS");
        assertEquals(10, extra.size());
        for (int i = 10; i <= 19; i++) {
            String id = "Boss" + i;
            assertTrue(extra.has(id), id);
            assertTrue(extra.get(id).get("gauges").size() >= 3, id + " gauges");
            assertTrue(ships.has(id), id + " in ShipConfig_Boss2");
            assertFalse(baseBoss.has(id), id + " collides BossConfig");
            assertFalse(baseShip.has(id), id + " collides ShipConfig BOSS");
            String img = ships.get(id).get("datas").get("imagePath").asText();
            assertTrue(new File(RES + "img/ship" + img.replace(".png", "_blue.png")).exists(), id + " image " + img);
        }
        assertEquals(10, ships.size());
    }

    @Test
    @DisplayName("StageConfig のキーは stage0/sample/stage1〜30/stage1100以上のみ")
    void stageKeys() throws Exception {
        JsonNode n = read("StageConfig.json");
        Iterator<String> it = n.fieldNames();
        int extra = 0;
        while (it.hasNext()) {
            String k = it.next();
            if (k.equals("stage0") || k.equals("sample"))
                continue;
            assertTrue(k.matches("stage\\d+"), k);
            int id = Integer.parseInt(k.substring(5));
            assertTrue(id <= 30 || id >= 1100, k);
            if (id >= 1100)
                extra++;
        }
        assertTrue(extra > 0);
        assertTrue(n.has("stage1101"));
        assertTrue(n.has("stage1141"));
        assertTrue(n.has("stage1200"));
    }

    @Test
    @DisplayName("追加ステージIDの定数")
    void constants() {
        assertEquals(1141, DailyService.STAGE_ID);
        assertEquals(1200, EndlessWaves.STAGE_ID);
    }
}
