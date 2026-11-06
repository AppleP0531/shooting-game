package com.game.shooting_game.gameScene.stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.beam.BlockSpec;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.ModuleReward;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;

/**
 * デイリーチャレンジ(毎日変わる条件のステージ)
 * 
 * - 日付から条件と中身を決めるので、同じ日は誰がやっても同じ内容になる。
 * - 中身は、プレイヤーの進行度(クリア済みステージ)に近い通常ステージのウェーブを2つ分混ぜたもの。
 * 進行度がボスステージに届いていれば、最後にそのボスが出る。
 * - 条件は、下の一覧から日替わりで2つ。
 * - 報酬は、1日1回だけモジュール(ModuleReward.once = DAILY)と、基板・素材。
 * - 起動中に日付をまたいでも、エクストラ画面に入るたびに作り直す。
 */
public final class DailyService {
    public static final int STAGE_ID = 1141;
    private static final int UNLOCK = 5;
    private static final int[] BOSS_STAGES = { 5, 10, 15, 20, 25 };

    private DailyService() {
    }

    private static final String[] CONDITION_NAMES = { "硬い敵", "猛攻", "ガラスの機体", "回復なし", "シールド停止", "ボーナスコイン" };

    private static void apply(int cond, StageModifiers.Spec s) {
        switch (cond) {
            case 0 -> s.setEnemyHpRate(s.getEnemyHpRate() * 1.6);
            case 1 -> s.setEnemyDamageRate(s.getEnemyDamageRate() * 1.6);
            case 2 -> s.setPlayerHpRate(s.getPlayerHpRate() * 0.5);
            case 3 -> s.setNoHeal(true);
            case 4 -> s.setNoShield(true);
            default -> { // ボーナスコイン(コイン2倍の代わりに敵が少し硬い)
                s.setCoinRate(s.getCoinRate() * 2.0);
                s.setEnemyHpRate(s.getEnemyHpRate() * 1.3);
            }
        }
    }

    private static int waveNo(String key) {
        return Integer.parseInt(key.substring(4));
    }

    /** ボスを含まないウェーブのキー(番号順) */
    private static List<String> normalWaveKeys(StageData d) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, EnemyConfig[]> e : d.getEnemyList().entrySet()) {
            boolean boss = false;
            for (EnemyConfig c : e.getValue())
                if (c.getEnemyId().startsWith("Boss"))
                    boss = true;
            if (!boss && e.getValue().length > 0)
                keys.add(e.getKey());
        }
        keys.sort((a, b) -> Integer.compare(waveNo(a), waveNo(b)));
        return keys;
    }

    private static String lastWaveKey(StageData d) {
        String last = "wave1";
        for (String k : d.getEnemyList().keySet())
            if (waveNo(k) > waveNo(last))
                last = k;
        return last;
    }

    /**
     * 今日のデイリーチャレンジを作ってステージ設定に登録する
     */
    public static void refresh() {
        StageManager sm = StageManager.getInstance();
        LocalDate today = LocalDate.now();
        Random rnd = new Random(today.toEpochDay() * 7919L + 13);

        int cleared = Math.max(1, Math.min(28, DataManager.getUserData().getProgress().getClearedStage()));

        // ----- 条件(日替わりで2つ) -----
        List<Integer> conds = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collections.shuffle(conds, rnd);
        conds = conds.subList(0, 2);
        StageModifiers.Spec spec = new StageModifiers.Spec();
        StringBuilder names = new StringBuilder();
        int hard = 0;
        for (int c : conds) {
            apply(c, spec);
            if (c != 5)
                hard++;
            names.append(names.length() > 0 ? " / " : "").append(CONDITION_NAMES[c]);
        }

        // ----- 中身(近いステージ2つのウェーブを混ぜる) -----
        int a = Math.max(1, cleared - rnd.nextInt(3));
        int b = Math.max(1, cleared - rnd.nextInt(3));
        if (b == a)
            b = Math.max(1, a - 1);
        List<String[]> picks = new ArrayList<>(); // {ステージ番号, ウェーブキー}
        for (int id : new int[] { a, b }) {
            List<String> keys = normalWaveKeys(sm.getStageConfig().get("stage" + id));
            Collections.shuffle(keys, rnd);
            for (String k : keys.subList(0, Math.min(3, keys.size())))
                picks.add(new String[] { String.valueOf(id), k });
        }
        Collections.shuffle(picks, rnd);

        Map<String, EnemyConfig[]> enemies = new LinkedHashMap<>();
        Map<String, BeamSpec[]> gimmicks = new LinkedHashMap<>();
        Map<String, BlockSpec[]> blocks = new LinkedHashMap<>();
        int n = 0;
        for (String[] p : picks) {
            StageData src = sm.getStageConfig().get("stage" + p[0]);
            n++;
            enemies.put("wave" + n, src.getEnemyList().get(p[1]));
            if (src.getGimmicks() != null && src.getGimmicks().get(p[1]) != null)
                gimmicks.put("wave" + n, src.getGimmicks().get(p[1]));
            if (src.getBlocks() != null && src.getBlocks().get(p[1]) != null)
                blocks.put("wave" + n, src.getBlocks().get(p[1]));
        }
        // 進行度がボスステージに届いていれば、最後にそのボス
        int bossStage = 0;
        for (int s : BOSS_STAGES)
            if (s <= cleared)
                bossStage = s;
        if (bossStage > 0) {
            StageData src = sm.getStageConfig().get("stage" + bossStage);
            String k = lastWaveKey(src);
            n++;
            enemies.put("wave" + n, src.getEnemyList().get(k));
            if (src.getGimmicks() != null && src.getGimmicks().get(k) != null)
                gimmicks.put("wave" + n, src.getGimmicks().get(k));
            if (src.getBlocks() != null && src.getBlocks().get(k) != null)
                blocks.put("wave" + n, src.getBlocks().get(k));
        }

        // ----- 報酬(進行度で上がる。条件が厳しい日はもう1段上) -----
        int tier = cleared < 10 ? 1 : cleared < 20 ? 2 : 3;
        if (hard >= 2)
            tier++;
        Rarity[] rar = Rarity.values();
        Rarity rarity = rar[Math.min(tier, rar.length - 1)];
        Rarity chipRarity = rar[Math.max(0, Math.min(tier - 1, rar.length - 1))];

        Map<ResourceName, Map<Rarity, String>> rewards = new LinkedHashMap<>();
        rewards.put(ResourceName.MODULE_CHIP, new LinkedHashMap<>(Map.of(chipRarity, "2~4")));
        ResourceName[] mats = { ResourceName.GEAR, ResourceName.SCREW, ResourceName.ENERGY_CELL, ResourceName.CIRCUIT,
                ResourceName.LENS };
        rewards.put(mats[rnd.nextInt(mats.length)], new LinkedHashMap<>(Map.of(chipRarity, "2~4")));

        ModuleReward mr = new ModuleReward();
        mr.setRarity(rarity.name());
        mr.setChance(1.0);
        mr.setOnce("DAILY");

        StageData d = new StageData();
        d.setTitle("デイリー " + today.getMonthValue() + "/" + today.getDayOfMonth());
        d.setDescription("毎日変わる条件のステージ。1日1回、クリアでモジュールが手に入る。\n\n【今日の条件】 " + names + "\n"
                + StageModifiers.describe(spec));
        d.setCategory("SPECIAL");
        d.setUnlock(UNLOCK);
        d.setModifiers(spec);
        d.setRewardList(rewards);
        d.setModuleRewards(List.of(mr));
        d.setEnemyList(enemies);
        d.setGimmicks(gimmicks);
        d.setBlocks(blocks);
        sm.putStage("stage" + STAGE_ID, d);
    }
}
