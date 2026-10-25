package com.game.shooting_game.gameScene.stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.beam.BlockSpec;
import com.game.shooting_game.gameScene.hazard.MeteorSpec;
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
        addGimmicks(d, stageId, waves, boss != null);
        return d;
    }

    // ギミックが入り始めるステージ番号
    private static final int FENCE_FROM = 36; // レーザー柵
    private static final int METEOR_FROM = 45; // 隕石
    private static final int WALL_FROM = 60; // 動く壁

    /**
     * 通常ステージにギミック(レーザー柵・隕石・動く壁)を混ぜる。ウェーブごとに、起きるかと種類を決める。
     * 敵の並びの乱数とは別の乱数を使うので、敵の中身は変わらない。最初のウェーブとボスのウェーブには入れない。
     */
    private static void addGimmicks(StageData d, int stageId, int waves, boolean hasBoss) {
        Random rnd = new Random(stageId * 104729L);
        double chance = Math.min(0.5, 0.2 + (stageId - FIRST_GENERATED) / 400.0); // 進むほど増える
        Map<String, BeamSpec[]> gimmicks = new LinkedHashMap<>();
        Map<String, MeteorSpec[]> meteors = new LinkedHashMap<>();
        Map<String, BlockSpec[]> blocks = new LinkedHashMap<>();
        int last = hasBoss ? waves - 1 : waves;
        for (int w = 2; w <= last; w++) {
            if (rnd.nextDouble() >= chance)
                continue;
            List<Integer> kinds = new ArrayList<>();
            if (stageId >= FENCE_FROM)
                kinds.add(0);
            if (stageId >= METEOR_FROM)
                kinds.add(1);
            if (stageId >= WALL_FROM)
                kinds.add(2);
            if (kinds.isEmpty())
                continue;
            switch (kinds.get(rnd.nextInt(kinds.size()))) {
                case 0 -> gimmicks.put("wave" + w, fence(stageId, rnd));
                case 1 -> meteors.put("wave" + w, meteorShower(stageId, rnd));
                default -> blocks.put("wave" + w, movingWall(rnd));
            }
        }
        if (!gimmicks.isEmpty())
            d.setGimmicks(gimmicks);
        if (!meteors.isEmpty())
            d.setMeteors(meteors);
        if (!blocks.isEmpty())
            d.setBlocks(blocks);
    }

    /** レーザー柵: 左右の壁から画面を横切るレーザーが、高さを変えて交互に、一定の間隔で光る(色が同じなら当たらない) */
    static BeamSpec[] fence(int stageId, Random rnd) {
        BeamSpec[] out = new BeamSpec[2];
        double interval = Math.max(3.0, 4.6 - (stageId - FENCE_FROM) / 100.0);
        double y = 230 + rnd.nextInt(80);
        for (int i = 0; i < 2; i++) {
            BeamSpec s = new BeamSpec();
            s.setOrigin(i == 0 ? BeamSpec.Origin.WALL_LEFT : BeamSpec.Origin.WALL_RIGHT);
            s.setY(i == 0 ? y : y + 120 + rnd.nextInt(50));
            s.setColor("ALT");
            s.setWarn(1.4);
            s.setDuration(1.0);
            s.setWidth(30);
            s.setDamage(10 + stageId / 12.0);
            s.setDelay(2.0 + i * interval / 2);
            s.setInterval(interval);
            out[i] = s;
        }
        return out;
    }

    /** 隕石: 上からランダムな位置に、一定の間隔で降ってくる(弾で壊せる) */
    static MeteorSpec[] meteorShower(int stageId, Random rnd) {
        MeteorSpec m = new MeteorSpec();
        m.setDelay(2.0);
        m.setInterval(Math.max(0.8, 1.7 - (stageId - METEOR_FROM) / 150.0));
        m.setCount(0);
        m.setRadius(15 + rnd.nextInt(10));
        m.setVy(95 + stageId / 3.0);
        m.setHp(25 + stageId / 2.0);
        m.setDamage(14 + stageId / 8.0);
        return new MeteorSpec[] { m };
    }

    /** 動く壁: 隙間のある壁が上から降りてきて、隙間が左右に往復する(ビームは撃たない。弾も船も通せない) */
    static BlockSpec[] movingWall(Random rnd) {
        double gapHalf = 55 + rnd.nextInt(15);
        double swing = 70 + rnd.nextInt(30);
        double period = 4.0 + rnd.nextInt(3);
        List<BlockSpec> out = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2)
            for (int k = 0; k < 7; k++) {
                BlockSpec b = new BlockSpec();
                b.setX(side * (gapHalf + 20 + 40 * k));
                b.setY(-30);
                b.setVx(0);
                b.setVy(22);
                b.setSize(40);
                b.setDirs(new String[0]);
                b.setSwing(swing);
                b.setSwingPeriod(period);
                out.add(b);
            }
        return out.toArray(new BlockSpec[0]);
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
