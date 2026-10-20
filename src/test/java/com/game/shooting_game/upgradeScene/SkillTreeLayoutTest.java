package com.game.shooting_game.upgradeScene;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.upgradeScene.SkillTreeLayout.Node;
import com.game.shooting_game.upgradeScene.SkillTreeLayout.Pattern;

/**
 * スキルツリーの模様ごとの配置(実際の SkillTreeConfig.json で、ボタンが重ならないか)
 */
class SkillTreeLayoutTest {
    private static List<Node> nodes() throws Exception {
        try (InputStream is = SkillTreeLayoutTest.class.getResourceAsStream("/static/configs/SkillTreeConfig.json")) {
            List<Node> r = new ArrayList<>();
            for (JsonNode n : new ObjectMapper().readTree(is))
                r.add(new Node(n.get("name").asText(), n.hasNonNull("rootNode") ? n.get("rootNode").asText() : null,
                        n.get("pos").asInt()));
            return r;
        }
    }

    private static double minGap(Map<String, double[]> pos) {
        double min = Double.MAX_VALUE;
        List<double[]> l = new ArrayList<>(pos.values());
        for (int i = 0; i < l.size(); i++)
            for (int j = i + 1; j < l.size(); j++)
                min = Math.min(min, Math.hypot(l.get(i)[0] - l.get(j)[0], l.get(i)[1] - l.get(j)[1]));
        return min;
    }

    @Test
    @DisplayName("どの模様でも、全ボタンに位置が付き、ボタンどうしが重ならない")
    void noOverlap() throws Exception {
        List<Node> nodes = nodes();
        for (Pattern p : Pattern.values()) {
            Map<String, double[]> pos = SkillTreeLayout.compute(p, nodes);
            assertThat(pos).as(p + " の個数").hasSize(nodes.size());
            System.out.println(p + " minGap=" + minGap(pos));
            assertThat(minGap(pos)).as(p + " の最小間隔").isGreaterThanOrEqualTo(SkillTreeLayout.MIN_GAP);
        }
    }

    private static boolean segmentsCross(double[] a, double[] b, double[] c, double[] d) {
        double d1 = cross(c, d, a), d2 = cross(c, d, b), d3 = cross(a, b, c), d4 = cross(a, b, d);
        return d1 * d2 < -1e-9 && d3 * d4 < -1e-9;
    }

    private static double cross(double[] p, double[] q, double[] r) {
        return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0]);
    }

    @Test
    @DisplayName("枝(親子の線)どうしが交差しない(花・渦巻き・星・同心円)")
    void noCrossingEdges() throws Exception {
        List<Node> nodes = nodes();
        for (Pattern p : new Pattern[] { Pattern.FLOWER, Pattern.SPIRAL, Pattern.STAR, Pattern.RING }) {
            Map<String, double[]> pos = SkillTreeLayout.compute(p, nodes);
            double[] ext = SkillTreeLayout.extent(pos);
            System.out.println(p + " extent=" + Math.round(ext[0]) + "," + Math.round(ext[1]));
            for (Node a : nodes)
                for (Node b : nodes) {
                    if (a.parent() == null || b.parent() == null || a.name().compareTo(b.name()) >= 0)
                        continue;
                    assertThat(segmentsCross(pos.get(a.name()), pos.get(a.parent()), pos.get(b.name()), pos.get(b.parent())))
                            .as(p + ": " + a.name() + " と " + b.name() + " の枝").isFalse();
                }
        }
    }

    @Test
    @DisplayName("機体ごとに模様が違い、5機体で5種類そろう")
    void patternPerShip() {
        assertThat(java.util.stream.Stream.of("Ship1", "Ship2", "Ship3", "Ship4", "Ship5")
                .map(SkillTreeLayout::patternFor).distinct()).hasSize(5);
        assertThat(SkillTreeLayout.patternFor(null)).isEqualTo(Pattern.FLOWER);
    }

    @Test
    @DisplayName("模様によって実際に位置が変わる")
    void patternsDiffer() throws Exception {
        List<Node> nodes = nodes();
        Map<String, double[]> a = SkillTreeLayout.compute(Pattern.FLOWER, nodes);
        for (Pattern p : Pattern.values()) {
            if (p == Pattern.FLOWER)
                continue;
            Map<String, double[]> b = SkillTreeLayout.compute(p, nodes);
            boolean differs = a.keySet().stream().anyMatch(k -> Math.hypot(a.get(k)[0] - b.get(k)[0], a.get(k)[1] - b.get(k)[1]) > 1);
            assertThat(differs).as(p + " は花と違う配置").isTrue();
        }
    }
}
