package com.game.shooting_game.domain.subship;

import java.util.LinkedHashMap;
import java.util.Map;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Family;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.subship.SubShipTypeService.BeamDef;
import com.game.shooting_game.domain.subship.SubShipTypeService.LevelDef;
import com.game.shooting_game.domain.subship.SubShipTypeService.TypeDef;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * サブ機の強化と、サブ機の性能計算
 * <p>
 * サブ機はタイプ(SubShipTypeService)ごとにレベルを持ち、コインとパーツ素材(Family.PART)で上げる。
 * レベルが上がると、撃つ弾の数と種類(通常弾・追尾弾・ビーム)が増える。強化素材は使えない。
 * タイプごとのレベルは userData.subShipParts のキー {@code "LV:ID"} に記録する(なければ Lv1)。
 */
public final class SubShipService {
    private static final String component = "[SubShipService]";

    // サブ機の基本ダメージ倍率(自機の弾より弱く、強化で育てる)
    public static final double BASE_DAMAGE_SCALE = 0.6;
    // 装備中を表す値
    private static final String EQUIPPED = "Custom";
    private static final String LEVEL_PREFIX = "LV:";
    // 見た目の機体(Mk1〜5の画像)の大きさ倍率
    private static final double[] HULL_SIZE = { 1.0, 1.2, 1.4, 1.6, 1.8 };

    /** ゲーム中・ロビーのプレビューで、サブ機を自機に対して小さく描く倍率(工房のプレビューは大きく見せる) */
    public static final double WORLD_SCALE = 0.5;

    private SubShipService() {
    }

    // ----- 装備 -----
    /**
     * サブ機が装備されているか
     */
    public static boolean isEquipped() {
        UserData ud = DataManager.getUserData();
        return ud != null && ud.getSubShip() != null;
    }

    public static void setEquipped(boolean on) {
        UserData ud = DataManager.getUserData();
        if (ud != null)
            ud.setSubShip(on ? EQUIPPED : null);
    }

    // ----- レベル -----
    /** タイプのレベル(1〜最大) */
    public static int level(TypeDef t) {
        UserData ud = DataManager.getUserData();
        int lv = ud == null ? 1 : ud.getSubShipParts().getOrDefault(LEVEL_PREFIX + t.id(), 1);
        return Math.max(1, Math.min(lv, t.maxLevel()));
    }

    /** タイプの番号(設定の並びの順。0=標準) */
    public static int typeIndex(TypeDef t) {
        return Math.max(0, SubShipTypeService.types().indexOf(t));
    }

    /** 使っているタイプのレベル */
    public static int level() {
        return level(SubShipTypeService.current());
    }

    public static boolean isMaxLevel(TypeDef t) {
        return level(t) >= t.maxLevel();
    }

    /** 使っているタイプ・レベルでの撃ち方 */
    public static LevelDef def() {
        TypeDef t = SubShipTypeService.current();
        return t.at(level(t));
    }

    /** 「ガンナー Lv7」のような表記 */
    public static String levelLabel() {
        return SubShipTypeService.current().title() + " Lv" + level();
    }

    // ----- 見た目 -----
    /** 見た目の機体の段階(Mk1〜5。2レベルごとに変わる) */
    public static int visualTier(int level) {
        return Math.max(1, Math.min(5, (level + 1) / 2));
    }

    /** 機体の大きさ倍率(Mk) */
    public static double hullSize(int tier) {
        return HULL_SIZE[Math.max(1, Math.min(5, tier)) - 1];
    }

    /**
     * 見た目の部品の段階 {機体Mk, 主砲, 推進機, コア, エミッター}(SubShipRenderer に渡す)
     * 主砲は通常弾の数、コアは追尾弾の数、エミッターはビームの有無で決まる。
     */
    public static int[] look(TypeDef t, int level) {
        LevelDef d = t.at(level);
        int mk = visualTier(level);
        return new int[] { mk, Math.min(5, d.straight()), mk, Math.min(5, d.tracking()),
                d.beam() == null ? 0 : mk };
    }

    /**
     * 自機の中心からサブ機までの横の距離(画面倍率なし。大きい機体ほど外側に付く)
     */
    public static double sideOffset() {
        return 40 + 9 * hullSize(visualTier(level()));
    }

    // ----- 性能 -----
    /**
     * サブ機の弾のダメージ倍率(レベルとスキルを反映)
     */
    public static double damageScale() {
        return BASE_DAMAGE_SCALE * def().damage() * (1 + SkillService.getBonus("SUB_POWER"));
    }

    /**
     * 発射間隔にかける倍率(スキルの連射強化で短くなる)
     */
    public static double rateScale() {
        return 1.0 / (1 + SkillService.getBonus("SUB_RATE"));
    }

    /**
     * サブ機のビームのダメージ倍率(スキルを反映。ビーム自体の強さはレベルの設定)
     */
    public static double beamScale() {
        return (1 + SkillService.getBonus("SUB_POWER")) * (1 + SkillService.getBonus("BEAM_POWER"));
    }

    /** ビームの設定(いまのレベルで撃てなければ null) */
    public static BeamDef beam() {
        return def().beam();
    }

    /** 自機の動きから遅れて付いてくる時間(秒。強化すると機敏になる) */
    public static double followDelay() {
        return 0.35 - 0.02 * level();
    }

    // ----- 強化 -----
    /**
     * 次のレベルへの強化コスト(最大レベルならnull)
     */
    public static Cost getCost(TypeDef t) {
        int lv = level(t);
        if (lv >= t.maxLevel())
            return null;
        int coin = (int) Math.round(t.upgradeCoin() * Math.pow(t.upgradeGrowth(), lv - 1));

        // 素材: 1種目はレベルに応じて数が増え、高いレベルほど上のレア度も要る。2種目は少なめ
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        int i = 0;
        for (String m : t.mats()) {
            ResourceName name;
            try {
                name = ResourceName.valueOf(m);
            } catch (IllegalArgumentException e) {
                continue;
            }
            // パーツ素材以外はコストに指定できない
            if (name.getFamily() != Family.PART)
                continue;
            double k = i == 0 ? 1.0 : 0.7;
            items.put(new ResourceKinds(name, Rarity.COMMON), (int) Math.round((6 + 2 * (lv - 1)) * k));
            if (lv >= 4)
                items.put(new ResourceKinds(name, Rarity.UNCOMMON), (int) Math.round((lv - 3) * k + 0.4));
            if (lv >= 7)
                items.put(new ResourceKinds(name, Rarity.RARE), (int) Math.round((lv - 6) * k + 0.4));
            if (lv >= 9 && i == 0)
                items.put(new ResourceKinds(name, Rarity.EPIC), 1);
            i++;
        }
        return new Cost(coin, items);
    }

    public static boolean canAfford(Cost cost) {
        // コインと素材の所持確認は強化と共通
        return cost != null && CostService.canAfford(cost);
    }

    /**
     * タイプを1レベル強化する(コインと素材を払う)
     *
     * @return 強化できたらtrue(未解放・最大レベル・足りないときはfalse)
     */
    public static synchronized boolean upgrade(TypeDef t) {
        UserData ud = DataManager.getUserData();
        Cost cost = getCost(t);
        if (ud == null || cost == null || !SubShipTypeService.isUnlocked(t) || !canAfford(cost))
            return false;
        CostService.pay(ud, cost);
        ud.getSubShipParts().put(LEVEL_PREFIX + t.id(), level(t) + 1);
        LogUtils.info(component,
                ANSI.YELLOW + t.id() + ANSI.RESET + " upgraded: Lv" + level(t) + ".");
        return true;
    }

    // ----- 表示 -----
    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9)
            return String.valueOf((long) Math.rint(v));
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * そのレベルの撃ち方の説明(例: 「通常弾×3 / 追尾弾×1 / ビーム」)
     */
    public static String shotText(LevelDef d) {
        StringBuilder sb = new StringBuilder();
        if (d.straight() > 0)
            sb.append("通常弾×").append(d.straight());
        if (d.tracking() > 0)
            sb.append(sb.length() > 0 ? " / " : "").append("追尾弾×").append(d.tracking());
        if (d.beam() != null)
            sb.append(sb.length() > 0 ? " / " : "").append("ビーム");
        return sb.toString();
    }

    /**
     * タイプのいまのレベルと、次のレベルで変わること
     */
    public static String describe(TypeDef t) {
        int lv = level(t);
        LevelDef now = t.at(lv);
        StringBuilder sb = new StringBuilder();
        sb.append("Lv").append(lv).append(" / ").append(t.maxLevel()).append("\n");
        sb.append("いま: ").append(shotText(now)).append("\n");
        sb.append("  弾ダメージ x").append(fmt(now.damage()));
        if (now.straight() > 0)
            sb.append(" ／ 通常弾 ").append(fmt(now.interval())).append("秒ごと");
        if (now.tracking() > 0)
            sb.append(" ／ 追尾弾 ").append(fmt(now.trackInterval())).append("秒ごと");
        sb.append("\n");
        if (lv >= t.maxLevel())
            return sb.append("最大まで強化済み").toString();

        LevelDef next = t.at(lv + 1);
        sb.append("Lv").append(lv + 1).append("で: ").append(shotText(next));
        if (!shotText(next).equals(shotText(now)))
            sb.append("  ★変化");
        return sb.toString();
    }

    /**
     * コスト欄の下に出す一言(強化できるかどうかなど)
     */
    public static String status(TypeDef t) {
        if (!SubShipTypeService.isUnlocked(t))
            return "このタイプはまだ解放していません";
        Cost cost = getCost(t);
        if (cost == null)
            return "これ以上は強化できません";
        if (canAfford(cost))
            return "強化できます";
        return "コインかパーツ素材が足りません\n(強化用の素材はサブ機に使えません)";
    }

    /**
     * 全体の状態の説明
     */
    public static String summary() {
        TypeDef cur = SubShipTypeService.current();
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(cur.title()).append(" Lv").append(level(cur)).append("】サブ機: ")
                .append(isEquipped() ? "装備中" : "取り外し中").append("\n");
        sb.append("弾ダメージ x").append(fmt(damageScale()));
        sb.append(" ／ ").append(shotText(def()));
        return sb.toString();
    }
}
