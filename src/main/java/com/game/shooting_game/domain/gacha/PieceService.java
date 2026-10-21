package com.game.shooting_game.domain.gacha;

import java.util.Map;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.cost.CostService;

/**
 * 機体のピース(ガチャで集める)
 * <p>
 * 機体ごとの所持数を、progress.highScores のキー {@code "piece:<船ID>"} に記録する。
 * 使い道は2つ: 未開放の機体の解放(UNLOCK_PIECES 個)と、ランクアップ(ランクごとに決まった数)。
 * 管理者モードでは、所持数が無制限になり、払っても減らない({@link CostService} と同じ)。
 * 注意: "mission:" で始まるキーは★の数に数えられるので、その接頭辞にはしないこと。
 */
public final class PieceService {
    private static final String PREFIX = "piece:";

    /** 機体を解放するのに必要なピース */
    public static final int UNLOCK_PIECES = 50;

    /** ランクアップに必要なピース(ランク0→1、1→2、…の順) */
    private static final int[] RANK_UP_PIECES = { 100, 200, 500, 1000, 1500 };

    private PieceService() {
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    /** ランク rank から rank+1 へ上げるのに必要なピース(最大ランクなら0) */
    public static int rankUpPieces(int rank) {
        return rank < 0 || rank >= RANK_UP_PIECES.length ? 0 : RANK_UP_PIECES[rank];
    }

    /** 表示・判定に使うピースの所持数(管理者モードは無制限) */
    public static int owned(String shipId) {
        if (DataManager.isAdminMode())
            return CostService.UNLIMITED;
        return ownedReal(shipId);
    }

    /** 実際に持っているピースの数(管理者モードでも実数) */
    public static int ownedReal(String shipId) {
        Map<String, Integer> s = store();
        return s == null || shipId == null ? 0 : Math.max(0, s.getOrDefault(PREFIX + shipId, 0));
    }

    /** ピースを増やす */
    public static void add(String shipId, int amount) {
        Map<String, Integer> s = store();
        if (s == null || shipId == null || amount <= 0)
            return;
        s.merge(PREFIX + shipId, amount, Integer::sum);
    }

    /** ピースを払う(足りなければ false で、何も減らさない。管理者モードは何も減らさず true) */
    public static boolean consume(String shipId, int amount) {
        if (DataManager.isAdminMode())
            return shipId != null && amount >= 0;
        Map<String, Integer> s = store();
        if (s == null || shipId == null || amount < 0 || ownedReal(shipId) < amount)
            return false;
        s.put(PREFIX + shipId, ownedReal(shipId) - amount);
        return true;
    }
}
