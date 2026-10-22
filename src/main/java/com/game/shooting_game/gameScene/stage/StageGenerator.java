package com.game.shooting_game.gameScene.stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.utils.Vector2;

/**
 * 通常ステージ(31〜200)の自動生成
 * ステージ番号から決まった中身(同じ番号なら毎回同じ)を作る。進むほど、ウェーブが増え、出てくる敵の種類が広がる。
 * 5ステージごとにボスが最後のウェーブに出る(ボスの種類は順に巡る)。
 * 敵・ボスのHPやダメージの伸びは StageModifiers(ステージ番号に応じた倍率)が担当する。
 */
public final class StageGenerator {
    public static final int FIRST_GENERATED = 31;
    public static final int LAST_STAGE = 200;
    private static final int BOSS_EVERY = 5;
    private static final ResourceName[] MATERIALS = { ResourceName.GEAR, ResourceName.SCREW, ResourceName.ENERGY_CELL,
            ResourceName.CIRCUIT, ResourceName.LENS, ResourceName.COPPER_PLATE, ResourceName.IRON_BAR,
            ResourceName.ALLOY_PLATE, ResourceName.CRYSTAL_SHARD, ResourceName.PLASMA_CORE };
    private static final EntranceType[] ENTRANCES = { EntranceType.ZIGZAG, EntranceType.SPIRAL, EntranceType.SWOOP,
            EntranceType.ARC_LEFT, EntranceType.ARC_RIGHT, EntranceType.BOUNCE, EntranceType.LOOP_IN };

    private StageGenerator() {
    }

    /**
     * そのステージ番号のボス(5ステージごと。それ以外はnull)
     *
     * @param bosses 使えるボスのID(Boss1,Boss2…の順)
     */
    public static String bossFor(int stageId, List<String> bosses) {
        if (stageId % BOSS_EVERY != 0 || bosses.isEmpty())
            return null;
        return bosses.get((stageId / BOSS_EVERY - 1) % bosses.size());
    }

    /**
     * 1ステージ分を作る
     *
     * @param stageId 31〜200
     * @param enemies 通常の敵のID(弱い順)
     * @param bosses  ボスのID
     */
    public static StageData generate(int stageId, List<String> enemies, List<String> bosses) {
        Random rnd = new Random(stageId * 7919L);
        StageData d = new StageData();
        d.setRewardList(rewards(stageId));

        int waves = Math.min(20, 12 + (stageId - FIRST_GENERATED) / 15);
        String boss = bossFor(stageId, bosses);
        // 進むほど使える敵が増える窓(はじめは前半、のちに全種)
        int open = Math.min(enemies.size(), 12 + (stageId - FIRST_GENERATED) * enemies.size() / 130);

        Map<String, EnemyConfig[]> list = new LinkedHashMap<>();
        for (int w = 1; w <= waves; w++) {
            if (boss != null && w == waves)
                list.put("wave" + w, new EnemyConfig[] { bossConfig(boss) });
            else
                list.put("wave" + w, wave(rnd, enemies, open, stageId, w == waves));
        }
        d.setEnemyList(list);
        return d;
    }

    private static EnemyConfig[] wave(Random rnd, List<String> enemies, int open, int stageId, boolean last) {
        int count = Math.min(7, 3 + rnd.nextInt(3) + (stageId - FIRST_GENERATED) / 50 + (last ? 1 : 0));
        // 同じ波は2〜3種類に絞る(同じ敵がそろうと見映えがよい)
        List<String> kinds = new ArrayList<>();
        int kindCount = 2 + rnd.nextInt(2);
        for (int i = 0; i < kindCount; i++)
            kinds.add(enemies.get(rnd.nextInt(open)));
        EntranceType entrance = ENTRANCES[rnd.nextInt(ENTRANCES.length)];
        int level = stageId >= 100 && rnd.nextInt(3) == 0 ? 2 : stageId >= 60 ? 1 : 0;

        EnemyConfig[] out = new EnemyConfig[count];
        for (int i = 0; i < count; i++) {
            EnemyConfig c = new EnemyConfig();
            c.setEnemyId(kinds.get(i % kinds.size()));
            c.setMode(i % 2 == 0 ? ColorMode.BLUE : ColorMode.RED);
            c.setLevel(level);
            double x = count == 1 ? 0 : -150 + 300.0 * i / (count - 1);
            boolean fromLeft = i % 2 == 0;
            c.setSpawnPos(new Vector2(fromLeft ? -300 : 300, 40 + rnd.nextInt(100)));
            c.setStayPos(new Vector2(x, 90 + rnd.nextInt(80)));
            c.setEntrance(entrance);
            c.setDelay(0.35 * i);
            out[i] = c;
        }
        return out;
    }

    private static EnemyConfig bossConfig(String bossId) {
        EnemyConfig c = new EnemyConfig();
        c.setEnemyId(bossId);
        c.setMode(ColorMode.BLUE);
        c.setLevel(0);
        c.setSpawnPos(new Vector2(0, -120));
        c.setStayPos(new Vector2(0, 130));
        return c;
    }

    /**
     * クリア報酬(進むほど量が増え、レア度の高い素材も出る)
     */
    private static Map<ResourceName, Map<Rarity, String>> rewards(int stageId) {
        double k = 1.0 + (stageId - FIRST_GENERATED) / 40.0;
        Map<ResourceName, Map<Rarity, String>> r = new LinkedHashMap<>();
        for (ResourceName n : MATERIALS) {
            Map<Rarity, String> m = new LinkedHashMap<>();
            m.put(Rarity.COMMON, range(8 * k, 14 * k));
            if (stageId >= 60)
                m.put(Rarity.UNCOMMON, range(1 * k, 3 * k));
            if (stageId >= 110)
                m.put(Rarity.RARE, range(0.5 * k, 1.5 * k));
            if (stageId >= 160)
                m.put(Rarity.EPIC, range(0, 0.6 * k));
            r.put(n, m);
        }
        return r;
    }

    private static String range(double lo, double hi) {
        int a = (int) Math.round(lo), b = Math.max(a, (int) Math.round(hi));
        return a + "~" + b;
    }
}
