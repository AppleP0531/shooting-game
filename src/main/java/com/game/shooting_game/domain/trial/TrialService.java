package com.game.shooting_game.domain.trial;

import java.util.Map;
import java.util.Set;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.data.UserData.ShipStats;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.LogUtils;

/**
 * お試し出撃(まだ持っていない船を、短いトレーニングステージで試す)
 * <p>
 * 試している間だけ、その船のデータ(ShipStats)を一時的に作り、選択中の船にする。
 * 船のレベル・ランクは、持っている船のうち一番高いものに合わせる(スキル強化はなし)。
 * 終わったら({@link #end})、一時的な船のデータを消して、もとの選択中の船に戻す。
 * 報酬・クリア記録・実績の累計・鍵は、お試しのあいだは出さない(呼ぶ側が {@link #isActive} を見て止める)。
 */
public final class TrialService {
    private static final String component = "[TrialService]";

    /** お試しのトレーニングステージの番号(エクストラの一覧には出さない) */
    public static final int TRIAL_STAGE_ID = 1990;
    /** トレーニングステージのウェーブ数 */
    public static final int TRIAL_WAVES = 3;

    private static boolean active = false;
    private static String trialShip;
    private static String previousShip;

    private TrialService() {
    }

    /** お試し中か */
    public static synchronized boolean isActive() {
        return active;
    }

    /** お試し中の船のID(お試し中でなければ null) */
    public static synchronized String trialShip() {
        return active ? trialShip : null;
    }

    /** お試しできる船か(設定にある、まだ持っていない船。お試し中でない) */
    public static boolean canTry(String shipId) {
        return canTry(shipId, configuredShips());
    }

    /** お試しできる船か(設定にある船の一覧を渡す。設定を読まずに確かめたいとき用) */
    public static boolean canTry(String shipId, Set<String> configuredShips) {
        UserData ud = DataManager.getUserData();
        if (ud == null || shipId == null || isActive() || ud.getShips().containsKey(shipId))
            return false;
        return configuredShips.contains(shipId);
    }

    private static Set<String> configuredShips() {
        try {
            return ShipManager.getInstance().getShipConfig().get(ShipKinds.PLAYER).keySet();
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    /**
     * 持っている船のうち、一番高いレベルとランク(★)
     *
     * @return {レベル, ランク}。船がなければ {1, 0}
     */
    public static int[] bestLevelAndRank(Map<String, ShipStats> ships) {
        int level = 1;
        int rank = 0;
        for (ShipStats s : ships.values()) {
            level = Math.max(level, s.getLevel());
            rank = Math.max(rank, s.getStars());
        }
        return new int[] { level, rank };
    }

    /**
     * お試しを始める(一時的な船のデータを作り、選択中の船にする)
     *
     * @return 始められたらtrue
     */
    public static synchronized boolean begin(String shipId) {
        return begin(shipId, configuredShips());
    }

    /** お試しを始める(設定にある船の一覧を渡す。設定を読まずに確かめたいとき用) */
    public static synchronized boolean begin(String shipId, Set<String> configuredShips) {
        if (!canTry(shipId, configuredShips))
            return false;
        UserData ud = DataManager.getUserData();
        int[] best = bestLevelAndRank(ud.getShips());
        ShipStats stats = new ShipStats();
        stats.setLevel(best[0]);
        stats.setStars(best[1]);
        previousShip = ud.getSelectedShip();
        ud.getShips().put(shipId, stats);
        ud.setSelectedShip(shipId);
        trialShip = shipId;
        active = true;
        LogUtils.info(component, "Trial started: " + shipId + " (Lv" + best[0] + ", rank " + best[1] + ")");
        return true;
    }

    /**
     * お試しを終える(一時的な船のデータを消し、もとの選択中の船に戻す。何度呼んでもよい)
     */
    public static synchronized void end() {
        if (!active)
            return;
        active = false;
        UserData ud = DataManager.getUserData();
        if (ud != null) {
            ud.getShips().remove(trialShip);
            if (previousShip != null)
                ud.setSelectedShip(previousShip);
        }
        LogUtils.info(component, "Trial ended: " + trialShip);
        trialShip = null;
        previousShip = null;
    }
}
