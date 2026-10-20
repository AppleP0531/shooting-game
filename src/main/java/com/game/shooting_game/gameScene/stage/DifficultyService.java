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
    private static final int NORMAL_STAGE_LIMIT = 100;
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

    private static String missionKey(int stageId, int index) {
        return "mission:stage" + stageId + ":" + index;
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
     * ステージのミッション一覧と達成状況
     */
    public static List<Mission> missions(int stageId) {
        Map<String, Integer> s = store();
        List<String> texts = missionTexts(stageId);
        List<Mission> list = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++)
            list.add(new Mission(texts.get(i), s != null && s.containsKey(missionKey(stageId, i))));
        return list;
    }

    public static boolean allMissionsDone(int stageId) {
        return missions(stageId).stream().allMatch(Mission::done);
    }

    /**
     * HARD でクリアしたときの成績からミッションを判定して記録する
     *
     * @return 今回新しく達成したミッションの文
     */
    public static List<String> recordMissions(int stageId, int hits, double hpRate, double seconds) {
        Map<String, Integer> s = store();
        List<String> newly = new ArrayList<>();
        if (s == null)
            return newly;
        MissionSpec m = spec(stageId);
        boolean[] ok = { hits <= m.getMaxHits(), hpRate >= m.getMinHpRate(), seconds <= timeLimit(stageId) };
        List<String> texts = missionTexts(stageId);
        for (int i = 0; i < ok.length; i++) {
            if (ok[i] && !s.containsKey(missionKey(stageId, i))) {
                s.put(missionKey(stageId, i), 1);
                newly.add(texts.get(i));
            }
        }
        return newly;
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
