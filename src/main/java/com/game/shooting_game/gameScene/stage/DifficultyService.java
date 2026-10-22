package com.game.shooting_game.gameScene.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.gameScene.stage.StageManager.MissionSpec;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;

/**
 * 難易度ごとのクリア状況・解放・ミッションの管理
 *
 * - 記録は highScores に入れる。ノーマルは従来どおり "stage3"、ほかは "stage3:HARD"。
 * - ミッションは、HARD でクリアしたときの成績で判定する(1度達成したら残る)。
 * 全ミッション達成で、そのステージの NIGHTMARE が解放される。
 */
public final class DifficultyService {
    /** 難易度を選べるステージの番号の上限(これ未満が通常ステージ) */
    private static final int NORMAL_STAGE_LIMIT = 1000; // これ未満が通常ステージ(1〜200。エクストラは1000以上)
    private static final double BOSS_TIME_LIMIT = 150; // ボスチャレンジの時間制限(秒)

    /** ミッション1つ(表示文と、達成したか) */
    public record Mission(String text, boolean done) {
    }

    private DifficultyService() {
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null ? null : ud.getProgress().getHighScores();
    }

    private static StageData data(int stageId) {
        return StageManager.getInstance().getStageConfig().get("stage" + stageId);
    }

    /**
     * 難易度を選べるステージか(通常ステージと、エクストラのボス分類)
     */
    public static boolean selectable(int stageId) {
        if (stageId >= 1 && stageId < NORMAL_STAGE_LIMIT)
            return true;
        StageData d = data(stageId);
        return d != null && "BOSS".equals(d.getCategory());
    }

    /**
     * 記録に使うキー(ノーマルは従来と同じ)
     */
    public static String key(int stageId, Difficulty diff) {
        return diff == Difficulty.NORMAL ? "stage" + stageId : "stage" + stageId + ":" + diff.name();
    }

    public static int highScore(int stageId, Difficulty diff) {
        Map<String, Integer> s = store();
        return s == null ? 0 : s.getOrDefault(key(stageId, diff), 0);
    }

    public static boolean cleared(int stageId, Difficulty diff) {
        Map<String, Integer> s = store();
        if (s == null)
            return false;
        if (diff == Difficulty.NORMAL && stageId < NORMAL_STAGE_LIMIT
                && stageId <= DataManager.getUserData().getProgress().getClearedStage())
            return true;
        return s.containsKey(key(stageId, diff));
    }

    /**
     * その難易度を選べるか
     * EASY/NORMAL は常に、HARD はNORMALクリア後、NIGHTMARE は HARD のミッションをすべて達成後。
     */
    public static boolean unlocked(int stageId, Difficulty diff) {
        if (DataManager.isAdminMode())
            return true;
        return switch (diff) {
            case EASY, NORMAL -> true;
            case HARD -> cleared(stageId, Difficulty.NORMAL);
            case NIGHTMARE -> allMissionsDone(stageId);
        };
    }

    /**
     * 解放されていないときの理由(解放済みなら空)
     */
    public static String lockReason(int stageId, Difficulty diff) {
        if (unlocked(stageId, diff))
            return "";
        return diff == Difficulty.HARD ? "ノーマルでクリアすると解放されます"
                : "ハードのミッションをすべて達成すると解放されます";
    }

    // ----- ミッション -----
    private static MissionSpec spec(int stageId) {
        StageData d = data(stageId);
        return d == null || d.getMissions() == null ? new MissionSpec() : d.getMissions();
    }

    /**
     * 制限時間(秒)。ウェーブ数に応じて決まる(StageConfigの "missions" で上書きできる)
     */
    public static double timeLimit(int stageId) {
        MissionSpec m = spec(stageId);
        if (m.getMaxSeconds() > 0)
            return m.getMaxSeconds();
        StageData d = data(stageId);
        if (d != null && "BOSS".equals(d.getCategory()))
            return BOSS_TIME_LIMIT;
        int waves = d == null || d.getEnemyList() == null ? 1 : Math.max(1, d.getEnemyList().size());
        return waves * m.getSecondsPerWave();
    }

    /** 難易度ごとの記録キー。HARD は従来のキー(mission:stageN:i)をそのまま使い、これまでのセーブデータを引き継ぐ */
    static String missionKey(int stageId, Difficulty diff, int index) {
        return diff == Difficulty.HARD
                ? "mission:stage" + stageId + ":" + index
                : "mission:stage" + stageId + ":" + diff.name() + ":" + index;
    }

    private static List<String> missionTexts(int stageId) {
        MissionSpec m = spec(stageId);
        List<String> t = new ArrayList<>();
        t.add("被弾 " + m.getMaxHits() + "回以内");
        t.add("クリア時のHPが " + Math.round(m.getMinHpRate() * 100) + "% 以上");
        t.add(Math.round(timeLimit(stageId)) + "秒以内にクリア");
        return t;
    }

    /**
     * ステージのミッション一覧と、その難易度での達成状況(★)
     */
    public static List<Mission> missions(int stageId, Difficulty diff) {
        Map<String, Integer> s = store();
        List<String> texts = missionTexts(stageId);
        List<Mission> list = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++)
            list.add(new Mission(texts.get(i), s != null && s.containsKey(missionKey(stageId, diff, i))));
        return list;
    }

    /**
     * HARD でのミッション一覧と達成状況(NIGHTMARE の解放条件に使う)
     */
    public static List<Mission> missions(int stageId) {
        return missions(stageId, Difficulty.HARD);
    }

    /**
     * その難易度で獲得した★の数(0〜3)
     */
    public static int stars(int stageId, Difficulty diff) {
        return (int) missions(stageId, diff).stream().filter(Mission::done).count();
    }

    /**
     * 獲得した★の合計(全ステージ・全難易度。★の累計報酬に使う)
     * ミッションの記録キー(mission:...)の数がそのまま★の数になる。
     */
    public static int totalStars() {
        Map<String, Integer> s = store();
        if (s == null)
            return 0;
        return (int) s.keySet().stream().filter(k -> k.startsWith("mission:")).count();
    }

    /** HARD のミッションをすべて達成したか(NIGHTMARE の解放条件) */
    public static boolean allMissionsDone(int stageId) {
        return missions(stageId, Difficulty.HARD).stream().allMatch(Mission::done);
    }

    /**
     * クリアしたときの判定結果
     *
     * @param missions       判定後のミッション一覧と達成状況(これまでの分を含む)
     * @param achievedNow    今回の成績で満たしたか(ミッションの並び順)
     * @param newlyEarned    今回はじめて★になったか(ミッションの並び順)
     */
    public record MissionResult(List<Mission> missions, boolean[] achievedNow, boolean[] newlyEarned) {
        /** 今回はじめて★になった数 */
        public int newStarCount() {
            int n = 0;
            for (boolean b : newlyEarned)
                if (b)
                    n++;
            return n;
        }
    }

    /**
     * ミッションを判定してその難易度の★として記録する(1度達成したものは残る)
     */
    public static MissionResult recordMissions(int stageId, Difficulty diff, int hits, double hpRate,
            double seconds) {
        Map<String, Integer> s = store();
        MissionSpec m = spec(stageId);
        boolean[] ok = judge(m, timeLimit(stageId), hits, hpRate, seconds);
        boolean[] fresh = new boolean[ok.length];
        if (s != null) {
            for (int i = 0; i < ok.length; i++) {
                String key = missionKey(stageId, diff, i);
                if (ok[i] && !s.containsKey(key)) {
                    s.put(key, 1);
                    fresh[i] = true;
                }
            }
        }
        return new MissionResult(missions(stageId, diff), ok, fresh);
    }

    /** プレイ中の、ミッション1つの状況 */
    public enum LiveState {
        /** いまの成績なら満たせる */
        OK,
        /** もう満たせない(被弾が多すぎる・時間切れ) */
        FAILED,
        /** いまは満たしていないが、クリアまでに満たせる(HPの割合は回復で戻る) */
        PENDING
    }

    /**
     * プレイ中に出すミッション1つ
     *
     * @param text     ミッションの文
     * @param progress いまの数値(例: "被弾 1/3"、"HP 85%"、"32秒 / 60秒")
     * @param state    いまの状況
     * @param earned   これまでに★として記録済みか
     */
    public record LiveMission(String text, String progress, LiveState state, boolean earned) {
    }

    /**
     * プレイ中のミッションの状況(状態を読まない純粋な計算)
     *
     * @param hits      いまの被弾回数
     * @param hpRate    いまのHPの割合(0〜1)
     * @param seconds   いまの経過時間(秒)
     * @param timeLimit 制限時間(秒)
     * @param earned    これまでに達成済みか(ミッションの並び順)
     */
    static List<LiveMission> live(MissionSpec m, double timeLimit, int hits, double hpRate, double seconds,
            boolean[] earned) {
        List<LiveMission> list = new ArrayList<>();
        list.add(new LiveMission("被弾 " + m.getMaxHits() + "回以内", "被弾 " + hits + "/" + m.getMaxHits(),
                hits <= m.getMaxHits() ? LiveState.OK : LiveState.FAILED, earned.length > 0 && earned[0]));
        list.add(new LiveMission("クリア時のHPが " + Math.round(m.getMinHpRate() * 100) + "% 以上",
                "HP " + Math.round(hpRate * 100) + "%",
                hpRate >= m.getMinHpRate() ? LiveState.OK : LiveState.PENDING, earned.length > 1 && earned[1]));
        list.add(new LiveMission(Math.round(timeLimit) + "秒以内にクリア",
                Math.round(seconds) + "秒 / " + Math.round(timeLimit) + "秒",
                seconds <= timeLimit ? LiveState.OK : LiveState.FAILED, earned.length > 2 && earned[2]));
        return list;
    }

    /**
     * プレイ中に画面へ出す、ミッションの状況(記録済みかも含める)
     */
    public static List<LiveMission> liveMissions(int stageId, Difficulty diff, int hits, double hpRate,
            double seconds) {
        List<Mission> done = missions(stageId, diff);
        boolean[] earned = new boolean[done.size()];
        for (int i = 0; i < earned.length; i++)
            earned[i] = done.get(i).done();
        return live(spec(stageId), timeLimit(stageId), hits, hpRate, seconds, earned);
    }

    /**
     * 成績から、3つのミッションを満たしたかを判定する(状態を読まない純粋な判定)
     *
     * @return {被弾回数, HPの残り割合, クリア時間} の順の達成
     */
    static boolean[] judge(MissionSpec m, double timeLimit, int hits, double hpRate, double seconds) {
        return new boolean[] { hits <= m.getMaxHits(), hpRate >= m.getMinHpRate(), seconds <= timeLimit };
    }

    // ----- 報酬 -----
    /**
     * 個数の範囲("3~6")に倍率をかけた文字列
     */
    public static String scaleRange(String range, double rate) {
        try {
            String[] n = range.split("~");
            long min = Math.round(Integer.parseInt(n[0]) * rate);
            long max = Math.round(Integer.parseInt(n[1]) * rate);
            return min + "~" + Math.max(min, max);
        } catch (Exception e) {
            return range;
        }
    }
}
