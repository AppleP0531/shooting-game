package com.game.shooting_game.domain.dex;

import java.util.Collection;
import java.util.Map;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;

/**
 * 敵図鑑の遭遇記録
 * <p>
 * 既存の progress.highScores を流用し、キー {@code "seen:" + 機体ID}、値 1 で遭遇済みを記録する
 * (DB・エンティティの変更は不要)。ユーザーデータが無い場合は何もしない。
 */
public final class DexService {
    private static final String SEEN_PREFIX = "seen:";

    private DexService() {
    }

    /**
     * 遭遇済みか
     *
     * @param shipId 機体ID
     */
    public static boolean isSeen(String shipId) {
        Map<String, Integer> map = records();
        return map != null && shipId != null && map.containsKey(SEEN_PREFIX + shipId);
    }

    /**
     * 遭遇を記録する(何度呼んでも同じ)
     *
     * @param shipId 機体ID
     */
    public static void markSeen(String shipId) {
        Map<String, Integer> map = records();
        if (map == null || shipId == null)
            return;
        map.putIfAbsent(SEEN_PREFIX + shipId, 1);
    }

    /**
     * 指定した機体のうち遭遇済みの数
     *
     * @param shipIds 機体IDの一覧
     */
    public static int seenCount(Collection<String> shipIds) {
        if (shipIds == null)
            return 0;
        int count = 0;
        for (String id : shipIds) {
            if (isSeen(id))
                count++;
        }
        return count;
    }

    private static Map<String, Integer> records() {
        UserData data = DataManager.getUserData();
        if (data == null || data.getProgress() == null)
            return null;
        return data.getProgress().getHighScores();
    }
}
