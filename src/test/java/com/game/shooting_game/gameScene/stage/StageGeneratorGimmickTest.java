package com.game.shooting_game.gameScene.stage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.game.shooting_game.gameScene.stage.StageManager.StageData;

class StageGeneratorGimmickTest {
    private static final List<String> ENEMIES = enemies();
    private static final List<String> BOSSES = List.of("Boss1", "Boss2");

    private static List<String> enemies() {
        List<String> l = new ArrayList<>();
        for (int i = 1; i <= 40; i++)
            l.add("Enemy" + i);
        return l;
    }

    private static StageData gen(int id) {
        return StageGenerator.generate(id, ENEMIES, BOSSES);
    }

    @Test
    @DisplayName("序盤のステージ(35まで)には、ギミックが入らない")
    void noGimmickEarly() {
        for (int id = StageGenerator.FIRST_GENERATED; id <= 35; id++) {
            StageData d = gen(id);
            assertThat(d.getGimmicks()).isNull();
            assertThat(d.getMeteors()).isNull();
            assertThat(d.getBlocks()).isNull();
        }
    }

    @Test
    @DisplayName("同じ番号なら、毎回同じギミックになる")
    void deterministic() {
        for (int id : new int[] { 70, 120, 199 }) {
            StageData a = gen(id), b = gen(id);
            assertThat(a.getGimmicks() == null ? null : a.getGimmicks().keySet())
                    .isEqualTo(b.getGimmicks() == null ? null : b.getGimmicks().keySet());
            assertThat(a.getMeteors() == null ? null : a.getMeteors().keySet())
                    .isEqualTo(b.getMeteors() == null ? null : b.getMeteors().keySet());
            assertThat(a.getBlocks() == null ? null : a.getBlocks().keySet())
                    .isEqualTo(b.getBlocks() == null ? null : b.getBlocks().keySet());
        }
    }

    @Test
    @DisplayName("進んだステージには、3種類のギミックがどれも現れる")
    void allKindsAppear() {
        boolean fence = false, meteor = false, wall = false;
        for (int id = 60; id <= StageGenerator.LAST_STAGE; id++) {
            StageData d = gen(id);
            fence |= d.getGimmicks() != null;
            meteor |= d.getMeteors() != null;
            wall |= d.getBlocks() != null;
        }
        assertThat(fence).isTrue();
        assertThat(meteor).isTrue();
        assertThat(wall).isTrue();
    }

    @Test
    @DisplayName("最初のウェーブとボスのウェーブには、ギミックを入れない")
    void notOnFirstOrBossWave() {
        for (int id = StageGenerator.FIRST_GENERATED; id <= StageGenerator.LAST_STAGE; id++) {
            StageData d = gen(id);
            int waves = d.getEnemyList().size();
            boolean boss = id % 5 == 0;
            List<String> used = new ArrayList<>();
            if (d.getGimmicks() != null)
                used.addAll(d.getGimmicks().keySet());
            if (d.getMeteors() != null)
                used.addAll(d.getMeteors().keySet());
            if (d.getBlocks() != null)
                used.addAll(d.getBlocks().keySet());
            assertThat(used).doesNotContain("wave1");
            if (boss)
                assertThat(used).doesNotContain("wave" + waves);
            for (String k : used)
                assertThat(d.getEnemyList()).containsKey(k);
        }
    }

    @Test
    @DisplayName("動く壁は、左右の壁の間に隙間があり、ビームは撃たない")
    void wallHasGap() {
        var blocks = StageGenerator.movingWall(new java.util.Random(1));
        double minAbsX = Double.MAX_VALUE;
        for (var b : blocks) {
            minAbsX = Math.min(minAbsX, Math.abs(b.getX()));
            assertThat(b.getDirs()).isEmpty();
            assertThat(b.getSwing()).isPositive();
        }
        assertThat(minAbsX).isGreaterThan(40); // 隙間(両側の最も内側のブロックの外側)
    }
}
