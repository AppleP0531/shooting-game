package com.game.shooting_game.gameScene.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;

/**
 * 通常ステージ自動生成(StageGenerator)の純ロジック
 */
class StageGeneratorTest {
    private static final List<String> ENEMIES = List.of("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "E10",
            "E11", "E12", "E13", "E14", "E15", "E16", "E17", "E18", "E19", "E20", "E21", "E22", "E23", "E24", "E25");
    private static final List<String> BOSSES = List.of("Boss1", "Boss2", "Boss3");

    private static String dump(StageData d) {
        StringBuilder sb = new StringBuilder();
        d.getEnemyList().forEach((k, v) -> {
            sb.append(k).append(':');
            for (EnemyConfig c : v)
                sb.append(c.getEnemyId()).append(',').append(c.getMode()).append(',').append(c.getLevel()).append(',')
                        .append(c.getSpawnPos().getX()).append(',').append(c.getSpawnPos().getY()).append(',')
                        .append(c.getStayPos().getX()).append(',').append(c.getStayPos().getY()).append(',')
                        .append(c.getEntrance()).append(',').append(c.getDelay()).append(';');
        });
        sb.append(d.getRewardList());
        return sb.toString();
    }

    @Test
    @DisplayName("同じ番号なら毎回同じ中身")
    void deterministic() {
        for (int id = 31; id <= 200; id++)
            assertEquals(dump(StageGenerator.generate(id, ENEMIES, BOSSES)),
                    dump(StageGenerator.generate(id, ENEMIES, BOSSES)), "stage" + id);
    }

    @Test
    @DisplayName("31〜200すべてにウェーブがあり、数は12〜20")
    void wavesInRange() {
        for (int id = 31; id <= 200; id++) {
            StageData d = StageGenerator.generate(id, ENEMIES, BOSSES);
            assertNotNull(d.getEnemyList());
            int n = d.getEnemyList().size();
            assertTrue(n >= 12 && n <= 20, "stage" + id + " waves=" + n);
            for (EnemyConfig[] w : d.getEnemyList().values())
                assertTrue(w.length > 0, "stage" + id);
        }
    }

    @Test
    @DisplayName("5ステージごとに最後のウェーブがボス(順に巡る)、それ以外は通常の敵のみ")
    void bossEveryFifth() {
        for (int id = 31; id <= 200; id++) {
            StageData d = StageGenerator.generate(id, ENEMIES, BOSSES);
            int n = d.getEnemyList().size();
            EnemyConfig[] last = d.getEnemyList().get("wave" + n);
            if (id % 5 == 0) {
                assertEquals(1, last.length, "stage" + id);
                assertEquals(BOSSES.get((id / 5 - 1) % BOSSES.size()), last[0].getEnemyId());
                assertEquals(StageGenerator.bossFor(id, BOSSES), last[0].getEnemyId());
                for (int w = 1; w < n; w++)
                    for (EnemyConfig c : d.getEnemyList().get("wave" + w))
                        assertFalse(c.getEnemyId().startsWith("Boss"), "stage" + id);
            } else {
                assertNull(StageGenerator.bossFor(id, BOSSES));
                for (EnemyConfig[] w : d.getEnemyList().values())
                    for (EnemyConfig c : w)
                        assertFalse(c.getEnemyId().startsWith("Boss"), "stage" + id);
            }
        }
    }

    @Test
    @DisplayName("bossFor: ボス一覧が空ならnull、5の倍数で巡回")
    void bossFor() {
        assertNull(StageGenerator.bossFor(35, List.of()));
        assertEquals("Boss1", StageGenerator.bossFor(5, BOSSES));
        assertEquals("Boss2", StageGenerator.bossFor(10, BOSSES));
        assertEquals("Boss3", StageGenerator.bossFor(15, BOSSES));
        assertEquals("Boss1", StageGenerator.bossFor(20, BOSSES));
    }

    @Test
    @DisplayName("使う敵のIDは渡した一覧から")
    void enemiesFromList() {
        for (int id = 31; id <= 200; id++) {
            StageData d = StageGenerator.generate(id, ENEMIES, BOSSES);
            for (EnemyConfig[] w : d.getEnemyList().values())
                for (EnemyConfig c : w)
                    assertTrue(ENEMIES.contains(c.getEnemyId()) || BOSSES.contains(c.getEnemyId()),
                            "stage" + id + " " + c.getEnemyId());
        }
    }

    @Test
    @DisplayName("敵の一覧が少なくても落ちない")
    void smallEnemyList() {
        List<String> small = List.of("A", "B");
        for (int id = 31; id <= 200; id++) {
            StageData d = StageGenerator.generate(id, small, BOSSES);
            for (EnemyConfig[] w : d.getEnemyList().values())
                for (EnemyConfig c : w)
                    assertTrue(small.contains(c.getEnemyId()) || BOSSES.contains(c.getEnemyId()));
        }
    }

    @Test
    @DisplayName("報酬の範囲は a~b で a<=b")
    void rewardRanges() {
        for (int id = 31; id <= 200; id++) {
            StageData d = StageGenerator.generate(id, ENEMIES, BOSSES);
            assertFalse(d.getRewardList().isEmpty());
            for (Map<?, String> m : d.getRewardList().values()) {
                assertFalse(m.isEmpty());
                for (String s : m.values()) {
                    assertTrue(s.matches("\\d+~\\d+"), "stage" + id + " " + s);
                    String[] p = s.split("~");
                    assertTrue(Integer.parseInt(p[0]) <= Integer.parseInt(p[1]), "stage" + id + " " + s);
                }
            }
        }
    }

    @Test
    @DisplayName("進むほど報酬が増える")
    void rewardGrows() {
        String lo = StageGenerator.generate(31, ENEMIES, BOSSES).getRewardList().values().iterator().next()
                .values().iterator().next();
        String hi = StageGenerator.generate(200, ENEMIES, BOSSES).getRewardList().values().iterator().next()
                .values().iterator().next();
        assertTrue(Integer.parseInt(hi.split("~")[1]) > Integer.parseInt(lo.split("~")[1]));
    }
}
