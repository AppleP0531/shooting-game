package com.game.shooting_game.stageScene.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * StageBtnConfig.json の配置の確認(台どうしが近すぎず、経路が交差せず、画面の幅に収まる)
 */
class StageLayoutTest {
    private static List<double[]> load() throws Exception {
        JsonNode n = new ObjectMapper().readTree(new File("src/main/resources/static/configs/StageBtnConfig.json"));
        List<double[]> pts = new ArrayList<>();
        for (JsonNode e : n)
            pts.add(new double[] { e.get("pos").get(0).asDouble(), e.get("pos").get(1).asDouble() });
        return pts;
    }

    private static double side(double[] p, double[] q, double[] r) {
        return Math.signum((q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0]));
    }

    private static boolean cross(double[] a, double[] b, double[] c, double[] d) {
        return side(a, b, c) != side(a, b, d) && side(c, d, a) != side(c, d, b);
    }

    @Test
    void 台どうしは近すぎず画面の幅に収まる() throws Exception {
        List<double[]> pts = load();
        assertEquals(200, pts.size());
        for (int i = 0; i < pts.size(); i++) {
            assertTrue(Math.abs(pts.get(i)[0]) <= 170, "x of " + (i + 1));
            assertTrue(pts.get(i)[1] >= pts.get(0)[1], "stage " + (i + 1) + " is below stage 1");
            for (int j = i + 1; j < pts.size(); j++)
                assertTrue(Math.hypot(pts.get(i)[0] - pts.get(j)[0], pts.get(i)[1] - pts.get(j)[1]) >= 60,
                        "too close: " + (i + 1) + " and " + (j + 1));
        }
    }

    @Test
    void 経路は交差しない() throws Exception {
        List<double[]> p = load();
        for (int i = 0; i + 1 < p.size(); i++)
            for (int j = i + 2; j + 1 < p.size(); j++)
                assertFalse(cross(p.get(i), p.get(i + 1), p.get(j), p.get(j + 1)),
                        "cross: " + (i + 1) + "-" + (i + 2) + " and " + (j + 1) + "-" + (j + 2));
    }

    @Test
    void ときどき下へ戻る区間がある() throws Exception {
        List<double[]> p = load();
        int down = 0;
        for (int i = 1; i < p.size(); i++)
            if (p.get(i)[1] < p.get(i - 1)[1])
                down++;
        assertTrue(down > 0, "no downward step");
    }
}
