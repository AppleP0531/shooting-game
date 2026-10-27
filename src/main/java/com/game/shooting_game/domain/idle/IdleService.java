package com.game.shooting_game.domain.idle;

import java.util.LinkedHashMap;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.power.PowerService;
import com.game.shooting_game.domain.star.StarRewardService;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageManager.EnemyConfig;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;

/**
 * 放置(留守番の稼ぎ)
 * <p>
 * クリア済みのノーマルステージを「放置先」に選ぶと、アプリを閉じていても、時間がたつほどそのステージの報酬(コインと、いちばん下のレア度の素材)がたまる。
 * たまる量は、そのステージの1周ぶんの見積もりに、1時間あたりの周回数と効率({@link #EFFICIENCY})をかけたもの。
 * 上限の時間({@link #MAX_SECONDS})を超えた分はたまらない。「受け取る」で受け取り、数え直す。
 * <p>
 * 保存は progress.highScores の {@code idle:stage}(放置先のステージ番号。なければ放置していない)と
 * {@code idle:since}(数え始めた時刻。2026-01-01 00:00 UTC からの秒数)。
 */
public final class IdleService {
    /** 時刻の基準(2026-01-01 00:00 UTC。秒数を int に収めるため) */
    public static final long BASE_EPOCH_SEC = 1767225600L;
    /** たまる時間の上限(8時間) */
    public static final int MAX_SECONDS = 8 * 3600;
    /** 実際の周回に対する効率(30%) */
    public static final double EFFICIENCY = 0.3;
    /** 1ウェーブあたりの、周回にかかる時間の見積もり(秒) */
    public static final double SECONDS_PER_WAVE = 25;

    private static final String STAGE_KEY = "idle:stage";
    private static final String SINCE_KEY = "idle:since";

    /** 1周ぶんの見積もり */
    public record PerRun(double coins, Map<ResourceKinds, Double> resources, double runsPerHour) {
    }

    private IdleService() {
    }

    // ----- 純粋な計算 -----
    /** 「最小~最大」の平均(読めなければ0) */
    public static double averageOf(String range) {
        try {
            String[] n = range.split("~");
            return (Integer.parseInt(n[0].trim()) + Integer.parseInt(n[1].trim())) / 2.0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 放置の時間(上限で止まる。負は0) */
    public static int cappedSeconds(long elapsedSec) {
        return (int) Math.max(0, Math.min(elapsedSec, MAX_SECONDS));
    }

    /** 1ステージを周回する、1時間あたりの回数(ウェーブ数からの見積もり) */
    public static double runsPerHour(int waves) {
        return 3600.0 / (Math.max(1, waves) * SECONDS_PER_WAVE);
    }

    /**
     * たまった報酬(小数点以下は切り捨て)
     *
     * @param elapsedSec 放置した時間(秒。上限を超えた分は数えない)
     */
    public static Reward compute(long elapsedSec, PerRun per) {
        double runs = cappedSeconds(elapsedSec) / 3600.0 * per.runsPerHour() * EFFICIENCY;
        Map<ResourceKinds, Integer> res = new LinkedHashMap<>();
        per.resources().forEach((k, v) -> {
            int n = (int) Math.floor(v * runs);
            if (n > 0)
                res.put(k, n);
        });
        return new Reward((int) Math.floor(per.coins() * runs), res, 0);
    }

    // ----- ステージから見積もる -----
    /** そのステージの1周ぶんの見積もり(コインはノーマルの報酬倍率つき。素材は、いちばん下のレア度だけ) */
    public static PerRun estimate(int stageId) {
        StageData d = StageManager.getInstance().getStageConfig().get("stage" + stageId);
        if (d == null)
            return new PerRun(0, Map.of(), runsPerHour(1));
        Map<ResourceKinds, Double> res = new LinkedHashMap<>();
        if (d.getRewardList() != null)
            for (var e : d.getRewardList().entrySet()) {
                String range = e.getValue().get(Rarity.COMMON);
                double avg = range == null ? 0 : averageOf(range) * Difficulty.NORMAL.getRewardRate();
                if (avg > 0)
                    res.put(new ResourceKinds(e.getKey(), Rarity.COMMON), avg);
            }
        double coins = 0;
        int waves = d.getEnemyList() == null ? 1 : d.getEnemyList().size();
        if (d.getEnemyList() != null)
            for (EnemyConfig[] wave : d.getEnemyList().values())
                for (EnemyConfig e : wave)
                    coins += coinsOf(e);
        return new PerRun(coins * Difficulty.NORMAL.getRewardRate(), res, runsPerHour(waves));
    }

    /** 敵1体のコインの期待値(1枚100。ステージでの計算と同じ式) */
    private static double coinsOf(EnemyConfig e) {
        try {
            for (var kind : ShipManager.ShipKinds.values()) {
                var defs = ShipManager.getInstance().getShipConfig().get(kind);
                var def = defs == null ? null : defs.get(e.getEnemyId());
                if (def != null && def.getDatas() != null)
                    return averageOf(def.getDatas().getCoins()) * (e.getLevel() + 1) * 100;
            }
        } catch (RuntimeException ex) {
            // 設定が読めないときは数えない
        }
        return 0;
    }

    // ----- 保存 -----
    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    /** いまの時刻(基準からの秒数) */
    public static int nowSec() {
        return (int) (System.currentTimeMillis() / 1000 - BASE_EPOCH_SEC);
    }

    /** 放置先のステージ番号(放置していなければ0) */
    public static int stage() {
        Map<String, Integer> s = store();
        return s == null ? 0 : s.getOrDefault(STAGE_KEY, 0);
    }

    /** 放置を始めた(数え始めた)時刻 */
    public static int since() {
        Map<String, Integer> s = store();
        return s == null ? 0 : s.getOrDefault(SINCE_KEY, 0);
    }

    /** 放置してたまった時間(秒。上限まで) */
    public static int elapsed(int now) {
        return stage() <= 0 ? 0 : cappedSeconds((long) now - since());
    }

    /** いまたまっている報酬(放置していなければ空) */
    public static Reward pending(int now) {
        if (stage() <= 0)
            return new Reward(0, Map.of(), 0);
        return compute(elapsed(now), estimate(stage()));
    }

    // ----- 選ぶ・受け取る -----
    /** 放置先に選べるか(クリア済みの、ノーマルのステージで、自機の戦力値が必要戦力値以上) */
    public static boolean canSelect(int stageId) {
        UserData ud = DataManager.getUserData();
        if (ud == null || stageId < 1 || stageId > 200)
            return false;
        return stageId <= ud.getProgress().getClearedStage()
                && PowerService.currentPower() >= PowerService.required(stageId, Difficulty.NORMAL);
    }

    /**
     * 放置先を選ぶ(いまたまっている分は、先に受け取る)
     *
     * @return 選べたらtrue(選べないときは何も変えない)
     */
    public static boolean select(int stageId, int now) {
        Map<String, Integer> s = store();
        if (s == null || !canSelect(stageId))
            return false;
        claim(now);
        s.put(STAGE_KEY, stageId);
        s.put(SINCE_KEY, now);
        return true;
    }

    /** 放置をやめる(たまっている分は、先に受け取る) */
    public static void stop(int now) {
        Map<String, Integer> s = store();
        if (s == null)
            return;
        claim(now);
        s.remove(STAGE_KEY);
        s.remove(SINCE_KEY);
    }

    /**
     * たまった報酬を受け取る(数え直す)
     *
     * @return 受け取った報酬(放置していなければ null。たまっていなくても、数え直す)
     */
    public static Reward claim(int now) {
        Map<String, Integer> s = store();
        if (s == null || stage() <= 0)
            return null;
        Reward r = pending(now);
        StarRewardService.grant(r);
        s.put(SINCE_KEY, now);
        return r;
    }
}
