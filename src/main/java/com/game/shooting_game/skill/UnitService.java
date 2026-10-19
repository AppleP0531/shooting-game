package com.game.shooting_game.skill;

import java.util.LinkedHashMap;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.DataManager.UserData;
import com.game.shooting_game.data.DataManager.UserData.ShipStats;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.skill.SkillService.Cost;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * 機体のレベル(1〜50)と★(0〜5)
 * 
 * - レベル: コインと強化素材で上げる。上がるほど火力と耐久力の基礎値が伸びる。
 * - ★: 同じ機体をもう1機(予備機)消費して上げる。レベルとは別に、さらに基礎値が伸びる。
 * 予備機は、ロビーの「機体を入手」で所持済みの機体をもう一度手に入れると増える。
 * スキルツリーの強化(%)とは別枠で、掛け算で効く。
 */
public final class UnitService {
    private static final String component = "[UnitService]";

    public static final int MAX_LEVEL = 50;
    public static final int MAX_STARS = 5;

    private static final double ATTACK_PER_LEVEL = 0.02; // レベル1つあたりの火力(Lv50で約2倍)
    private static final double HP_PER_LEVEL = 0.03; // レベル1つあたりの耐久力(Lv50で約2.5倍)
    private static final double ATTACK_PER_STAR = 0.05; // ★1つあたりの火力
    private static final double HP_PER_STAR = 0.06; // ★1つあたりの耐久力

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

    public static int getStars(String shipId) {
        ShipStats s = stats(shipId);
        return s == null ? 0 : Math.max(0, Math.min(MAX_STARS, s.getStars()));
    }

    public static int getCopies(String shipId) {
        ShipStats s = stats(shipId);
        return s == null ? 0 : Math.max(0, s.getCopies());
    }

    /**
     * 火力の倍率(レベルと★)
     */
    public static double attackRate(String shipId) {
        return (1 + ATTACK_PER_LEVEL * (getLevel(shipId) - 1)) * (1 + ATTACK_PER_STAR * getStars(shipId));
    }

    /**
     * 耐久力の倍率(レベルと★)
     */
    public static double hpRate(String shipId) {
        return (1 + HP_PER_LEVEL * (getLevel(shipId) - 1)) * (1 + HP_PER_STAR * getStars(shipId));
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
        int coins = (int) (Math.round(150 * Math.pow(1.11, level - 1) / 10.0) * 10);
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
        int count = 2 + step / 2 + band; // 帯が上がるほど、帯の中で進むほど多い
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
        if (!SkillService.canAfford(cost))
            return false;
        SkillService.pay(ud, cost);
        s.setLevel(getLevel(shipId) + 1);
        DataManager.save();
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + shipId + ANSI.RESET + " reached Lv" + s.getLevel() + ".");
        return true;
    }

    // ----- ★ -----
    /**
     * 予備の機体を1機追加する(所持済みの機体をもう一度入手したとき)
     */
    public static void addCopy(String shipId) {
        ShipStats s = stats(shipId);
        if (s == null)
            return;
        s.setCopies(s.getCopies() + 1);
        DataManager.save();
    }

    /**
     * 予備の機体を買う(コイン。★が最大のときは買えない)
     */
    public static boolean buyCopy(String shipId) {
        UserData ud = DataManager.getUserData();
        ShipStats s = stats(shipId);
        if (ud == null || s == null || getStars(shipId) + getCopies(shipId) >= MAX_STARS)
            return false;
        int price = copyPrice(shipId);
        if (ud.getResources().getCoins() < price)
            return false;
        ud.getResources().setCoins(ud.getResources().getCoins() - price);
        addCopy(shipId);
        return true;
    }

    /**
     * 予備の機体の値段(機体の入手価格の1.5倍)
     */
    public static int copyPrice(String shipId) {
        return (int) (shipPrice(shipId) * 1.5);
    }

    /**
     * 予備機を1機消費して★を1つ上げる
     */
    public static boolean starUp(String shipId) {
        ShipStats s = stats(shipId);
        if (s == null || getStars(shipId) >= MAX_STARS || getCopies(shipId) < 1)
            return false;
        s.setCopies(s.getCopies() - 1);
        s.setStars(getStars(shipId) + 1);
        DataManager.save();
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + shipId + ANSI.RESET + " is now " + starText(shipId) + ".");
        return true;
    }

    /**
     * ★の表示(例: ★★★☆☆)
     */
    public static String starText(String shipId) {
        int n = getStars(shipId);
        return "★".repeat(n) + "☆".repeat(MAX_STARS - n);
    }
}
