package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SkillTreeConfig.json の整合性を、JSONを直接読んで確認する
 */
class SkillTreeConfigTest {
    private static Map<String, JsonNode> nodes() throws Exception {
        try (InputStream is = SkillTreeConfigTest.class.getResourceAsStream("/static/configs/SkillTreeConfig.json")) {
            assertThat(is).isNotNull();
            Map<String, JsonNode> r = new HashMap<>();
            for (JsonNode n : new ObjectMapper().readTree(is))
                assertThat(r.put(n.get("name").asText(), n)).as("名前の重複: " + n.get("name")).isNull();
            return r;
        }
    }

    @Test
    @DisplayName("全ノード(根のOriginを除く)で matLevels の件数が maxLevel と合う")
    void matLevelsMatchMaxLevel() throws Exception {
        for (JsonNode n : nodes().values()) {
            if (!n.hasNonNull("rootNode")) // 根(Origin)は強化できないので matLevels が空
                continue;
            assertThat(n.get("matLevels").size()).as(n.get("name").asText()).isEqualTo(n.get("maxLevel").asInt());
        }
    }

    @Test
    @DisplayName("rootNode は実在するノードを指す")
    void rootNodesExist() throws Exception {
        Map<String, JsonNode> m = nodes();
        for (JsonNode n : m.values())
            if (n.hasNonNull("rootNode"))
                assertThat(m).as(n.get("name").asText() + " の rootNode").containsKey(n.get("rootNode").asText());
    }

    @Test
    @DisplayName("会心(Critical)と会心威力(Fatal)の設定")
    void critNodes() throws Exception {
        Map<String, JsonNode> m = nodes();
        JsonNode c = m.get("Critical");
        JsonNode f = m.get("Fatal");
        assertThat(c).isNotNull();
        assertThat(f).isNotNull();
        assertThat(c.get("effect").asText()).isEqualTo("CRIT_RATE");
        assertThat(c.get("perLevel").asDouble()).isCloseTo(0.01, within(1e-9));
        assertThat(c.get("maxLevel").asInt()).isEqualTo(10);
        assertThat(f.get("effect").asText()).isEqualTo("CRIT_DMG");
        assertThat(f.get("perLevel").asDouble()).isCloseTo(0.05, within(1e-9));
        assertThat(f.get("maxLevel").asInt()).isEqualTo(10);
        assertThat(f.get("rootNode").asText()).isEqualTo("Critical");
    }

    @Test
    @DisplayName("助走(Runup)の設定")
    void runupNode() throws Exception {
        JsonNode r = nodes().get("Runup");
        assertThat(r).isNotNull();
        assertThat(r.get("effect").asText()).isEqualTo("ENDLESS_SKIP");
        assertThat(r.get("perLevel").asDouble()).isCloseTo(50, within(1e-9));
        assertThat(r.get("maxLevel").asInt()).isEqualTo(1);
        assertThat(r.get("rootNode").asText()).isEqualTo("Thruster");
        assertThat(r.get("matLevels").size()).isEqualTo(1);
    }
}
