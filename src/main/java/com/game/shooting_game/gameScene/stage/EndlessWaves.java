package com.game.shooting_game.gameScene.stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.beam.BlockSpec;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.utils.Vector2;

/**
 * 無限モードのウェーブの生成
 * 
 * - 既存のステージ(1〜28)のウェーブを、進み具合に合わせて取り出して並べる。ウェーブが進むほど後ろのステージから出る。
 * - 10ウェーブごとにボスが出る。ふつうのボスは、すべてのゲージを削りきるまで戦う(進むほど強いボスも出る)。
 *   低い確率でコインボスが出る。コインボスは、1ゲージ壊されるか時間が経つと撤収し、ダメージを受けるたびにコインを落とす。
 * - 28ステージ分を超えてもウェーブは続き、敵のHPとダメージが上がり続ける(StageModifiers.setEndlessScale)。
 * - ゲームオーバーまで続く。到達したウェーブに応じて報酬が出る。
 */
public final class EndlessWaves {
    public static final int STAGE_ID = 200;
    public static final String BEST_WAVE_KEY = "endless:wave"; // 最高到達ウェーブ(highScoresに保存)
    private static final int MAX_SOURCE_STAGE = 28;
    private static final int BOSS_EVERY = 10;
    private static final int NORMAL_BOSS_COUNT = 6; // Boss1〜Boss6
    private static final double BLOCK_RATE = 0.25; // ブロック付きのウェーブのうち、ブロックを実際に出す確率
    private static final double COIN_BOSS_RATE = 0.15; // ボスの出番のうち、コインボスになる確率

    /**
     * 1ウェーブ分(敵・ビームギミック・ビームブロック)
     */
    public record Wave(EnemyConfig[] enemies, BeamSpec[] gimmicks, BlockSpec[] blocks) {
    }

    private EndlessWaves() {
    }

    private static boolean hasBoss(EnemyConfig[] enemies) {
        for (EnemyConfig e : enemies)
            if (e.getEnemyId().startsWith("Boss"))
                return true;
        return false;
    }

    /**
     * ウェーブの生成
     * 
     * @param wave 1から始まるウェーブ番号
     */
    public static Wave generate(int wave) {
        StageModifiers.setEndlessScale(wave);

        if (wave % BOSS_EVERY == 0)
            return new Wave(new EnemyConfig[] { pickBoss(wave) }, null, null);

        // 2ウェーブごとに1ステージ進む。直近の3ステージの中から選ぶ
        StageManager sm = StageManager.getInstance();
        int top = Math.min(MAX_SOURCE_STAGE, 1 + (wave - 1) / 2);
        int stage = Math.max(1, top - ThreadLocalRandom.current().nextInt(3));
        StageData d = sm.getStageConfig().get("stage" + stage);

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, EnemyConfig[]> e : d.getEnemyList().entrySet())
            if (e.getValue().length > 0 && !hasBoss(e.getValue()))
                keys.add(e.getKey());
        String key = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));

        // ブロックは元のステージで付いていても、一定の確率でしか出さない(出ないときはブロックなしのウェーブ)
        BlockSpec[] blocks = d.getBlocks() == null ? null : d.getBlocks().get(key);
        if (blocks != null && ThreadLocalRandom.current().nextDouble() >= BLOCK_RATE)
            blocks = null;

        return new Wave(d.getEnemyList().get(key),
                d.getGimmicks() == null ? null : d.getGimmicks().get(key),
                blocks);
    }

    /**
     * 出すボスを選ぶ
     * 低い確率(COIN_BOSS_RATE)でコインボス。それ以外は、ウェーブが進むほど強いボスも候補に入るふつうのボス。
     */
    private static EnemyConfig pickBoss(int wave) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (rnd.nextDouble() < COIN_BOSS_RATE)
            return boss("BossCoin");
        int available = Math.min(NORMAL_BOSS_COUNT, 1 + wave / BOSS_EVERY);
        return boss("Boss" + (1 + rnd.nextInt(available)));
    }

    private static EnemyConfig boss(String bossId) {
        EnemyConfig c = new EnemyConfig();
        c.setEnemyId(bossId);
        c.setMode(ColorMode.BLUE);
        c.setLevel(0);
        c.setSpawnPos(new Vector2(0, -120));
        c.setStayPos(new Vector2(0, 130));
        return c;
    }

    // ----- 報酬(到達したウェーブに応じて) -----
    private static Rarity tier(int cleared) {
        if (cleared < 20)
            return Rarity.COMMON;
        if (cleared < 40)
            return Rarity.UNCOMMON;
        if (cleared < 70)
            return Rarity.RARE;
        return Rarity.EPIC;
    }

    /**
     * 到達したウェーブに応じた素材の報酬(5ウェーブごとに基板と素材が増える)
     * 
     * @param cleared 突破したウェーブ数
     */
    public static Map<ResourceKinds, Integer> materialRewards(int cleared) {
        Map<ResourceKinds, Integer> r = new LinkedHashMap<>();
        int n = Math.min(12, cleared / 5);
        if (n <= 0)
            return r;
        Rarity rarity = tier(cleared);
        r.put(new ResourceKinds(ResourceName.MODULE_CHIP, rarity), n);
        ResourceName[] pool = { ResourceName.GEAR, ResourceName.SCREW, ResourceName.COPPER_PLATE,
                ResourceName.IRON_BAR };
        r.merge(new ResourceKinds(pool[ThreadLocalRandom.current().nextInt(pool.length)], rarity),
                Math.max(1, n / 2), Integer::sum);
        return r;
    }

    /**
     * 到達したウェーブに応じたモジュールの報酬(25ウェーブごとに1つ。後ろほどレアが出る)
     * 
     * @param cleared 突破したウェーブ数
     */
    public static List<Rarity> moduleRewards(int cleared) {
        List<Rarity> r = new ArrayList<>();
        Rarity[] table = { Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.EPIC, Rarity.LEGENDARY };
        for (int i = 1; i <= Math.min(5, cleared / 25); i++)
            r.add(table[i - 1]);
        return r;
    }
}
