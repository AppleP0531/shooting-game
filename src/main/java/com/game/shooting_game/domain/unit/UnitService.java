package com.game.shooting_game.domain.unit;

import com.game.shooting_game.domain.cost.CostService;
import java.util.LinkedHashMap;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.data.UserData.ShipStats;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.gacha.PieceService;
import com.game.shooting_game.utils.GameBalance;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * 機体のレベル(1〜50)とランク(0〜5)
 * 
 * - レベル: コインと強化素材で上げる。上がるほど火力と耐久力の基礎値が伸びる。

 * - ランク: レベルが最大(50)になったら、ランクコア(ボスなどが落とす)を使ってランクを1つ上げる。上げるとレベルは1に戻るが、
 *   火力と耐久力の倍率が大きく上がる。ランクが高いほど、必要なランクコアが増える。
 *   (保存の欄は、以前の★(ShipStats.stars)を使う。予備機(copies)は使わない)
 * スキルツリーの強化(%)とは別枠で、掛け算で効く。
 */
public final class UnitService {
    private static final String component = "[UnitService]";

    public static final int MAX_LEVEL = 50;
    public static final int MAX_RANK = 5;

    // 序盤は軽く(Lv1→2で60、Lv10→11で約280)、Lv30あたりから以前より重くなる(Lv49→50で約20万)
    private static final double LEVEL_COIN_BASE = 60; // Lv1→2に必要なコイン
    private static final double LEVEL_COIN_GROWTH = 1.185; // レベルごとのコイン倍率
    private static final double ITEM_BAND_EXTRA = 1.2; // 帯が上がるごとに素材が増える勢い(帯の2乗に比例)

    private static final double HP_PER_LEVEL = 0.03; // レベル1つあたりの耐久力(Lv50で約2.5倍)
    private static final double HP_PER_RANK = 0.5; // ランク1つあたりの耐久力

    private UnitService() {
    }

    private static ShipStats stats(String shipId) {
        UserData ud = DataManager.getUserData();
        return ud == null || shipId == null ? null : ud.getShips().get(shipId);
    }

    public static int getLevel(String shipId) {
        ShipStats s = stats(shipId);
        return s == null ? 1 : Math.max(1, Math.min(MAX_LEVEL, s.getLevel()));
    }

    /** ランク(0〜5)。保存の欄は、以前の★(stars)をそのまま使う */
    public static int getRank(String shipId) {
        ShipStats s = stats(shipId);
        return s == null ? 0 : Math.max(0, Math.min(MAX_RANK, s.getStars()));
    }

    /**
     * 火力の倍率(レベルとランク)
     */
    public static double attackRate(String shipId) {
        // レベルとランクの両方が指数的に伸びる(係数は BalanceConfig.json)。ランクが1つ上がると、レベルが1に戻っても、上がる前のLv50より強い
        return Math.pow(GameBalance.unitAttackGrowth(), getLevel(shipId) - 1)
                * Math.pow(GameBalance.rankAttackGrowth(), getRank(shipId));
    }

    /**
     * 耐久力の倍率(レベルとランク)
     */
    public static double hpRate(String shipId) {
        return (1 + HP_PER_LEVEL * (getLevel(shipId) - 1)) * (1 + HP_PER_RANK * getRank(shipId));
    }

    /**
     * 基礎の耐久力(スキル強化前)
     */
    public static double baseHp(String shipId) {
        var def = ShipManager.getInstance().getShipConfig().get(ShipKinds.PLAYER).get(shipId);
        double hp = def != null && def.getDatas() != null ? def.getDatas().getHp() : 0;
        return hp > 0 ? hp : 100;
    }

    /**
     * 機体入手に必要なコイン
     */
    public static int shipPrice(String shipId) {
        var def = ShipManager.getInstance().getShipConfig().get(ShipKinds.PLAYER).get(shipId);
        return def != null && def.getDatas() != null ? def.getDatas().getCost() : 0;
    }

    // ----- レベル -----
    /**
     * 今のレベルから次のレベルへ上げるコスト
     * 10レベルごとに必要な素材のレア度が上がる。
     */
    public static Cost levelCost(int level) {
        int coins = (int) (Math.round(LEVEL_COIN_BASE * Math.pow(LEVEL_COIN_GROWTH, level - 1) / 10.0) * 10);
        int band = Math.min(4, (level - 1) / 10); // 1〜10: 0, 11〜20: 1, ...
        int step = (level - 1) % 10;
        ResourceName name = band % 2 == 0 ? ResourceName.GEAR : ResourceName.SCREW;
        Rarity rarity = switch (band) {
            case 0 -> Rarity.COMMON;
            case 1 -> Rarity.COMMON;
            case 2 -> Rarity.UNCOMMON;
            case 3 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
        // 序盤の帯は軽く、後半の帯ほど急に重くなる
        int count = (int) Math.round(2 + step * (0.6 + 0.2 * band) + ITEM_BAND_EXTRA * band * band);
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        items.put(new ResourceKinds(name, rarity), count);
        return new Cost(coins, items);
    }

    public static boolean levelUp(String shipId) {
        ShipStats s = stats(shipId);
        UserData ud = DataManager.getUserData();
        if (s == null || ud == null || getLevel(shipId) >= MAX_LEVEL)
            return false;
        Cost cost = levelCost(getLevel(shipId));
        if (!CostService.canAfford(cost))
            return false;
        CostService.pay(ud, cost);
        s.setLevel(getLevel(shipId) + 1);
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + shipId + ANSI.RESET + " reached Lv" + s.getLevel() + ".");
        return true;
    }

    // ----- ランク -----
    /** ランクアップに使う素材の数(ランク0→1は RANK_BASE 個、以降はランクごとに2倍) */
    public static final int RANK_BASE = 10;

    /**
     * 次のランクへ上げるのに必要な素材(ランクコア。ランクが上がるほど多い)
     * 
     * @param rank 今のランク(0〜MAX_RANK-1)
     */
    public static Cost rankUpCost(int rank) {
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        items.put(new ResourceKinds(ResourceName.RANK_CORE, Rarity.COMMON), RANK_BASE * (1 << Math.max(0, rank)));
        return new Cost(0, items);
    }

    /**
     * ランクアップできるか(レベルが最大で、ランクが最大ではなく、ランクコアとその機体のピースが足りている)
     */
    public static boolean canRankUp(String shipId) {
        return getLevel(shipId) >= MAX_LEVEL && getRank(shipId) < MAX_RANK
                && CostService.canAfford(rankUpCost(getRank(shipId)))
                && PieceService.owned(shipId) >= PieceService.rankUpPieces(getRank(shipId));
    }

    /**
     * ランクアップする(素材を払い、ランクが1つ上がって、レベルは1に戻る)
     * 
     * @return できたらtrue
     */
    public static boolean rankUp(String shipId) {
        ShipStats s = stats(shipId);
        UserData ud = DataManager.getUserData();
        if (s == null || ud == null || !canRankUp(shipId))
            return false;
        CostService.pay(ud, rankUpCost(getRank(shipId)));
        PieceService.consume(shipId, PieceService.rankUpPieces(getRank(shipId)));
        s.setStars(getRank(shipId) + 1);
        s.setLevel(1);
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + shipId + ANSI.RESET + " is now " + rankText(shipId) + ".");
        return true;
    }

    /**
     * ランクの表示(例: Rank 2)
     */
    public static String rankText(String shipId) {
        return "Rank " + getRank(shipId);
    }
}
