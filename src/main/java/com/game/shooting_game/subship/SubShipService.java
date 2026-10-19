package com.game.shooting_game.subship;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Family;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.DataManager.UserData;
import com.game.shooting_game.skill.SkillService;
import com.game.shooting_game.skill.SkillService.Cost;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;

/**
 * サブ機パーツの制作状況と、サブ機の性能計算
 * パーツの制作には敵のドロップ品のうち「パーツ素材」(Family.PART)とコインを使う。強化素材は使えない。
 * 必須パーツ(required)がすべて制作済みになるとサブ機が完成し、自機の両端に付く。
 */
public final class SubShipService {
    private static final String component = "[SubShipService]";
    private static final String CONFIG_PATH = "/static/configs/SubShipPartConfig.json";

    // サブ機の基本ダメージ倍率(自機の弾より弱く、パーツで育てる)
    public static final double BASE_DAMAGE_SCALE = 0.6;
    // 装備中を表す値
    private static final String EQUIPPED = "Custom";

    // ----- データクラスの定義 -----
    /**
     * パーツ設定
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class PartData {
        private String title;
        private boolean required;
        private String detail;
        private List<TierData> tiers;
    }

    /**
     * パーツのMkごとの設定
     */
    @Data // Getter, Setterの自動生成
    @NoArgsConstructor // 引数なしコンストラクタの生成
    @JsonIgnoreProperties(ignoreUnknown = true) // 余計な項目の無視
    public static class TierData {
        private String label;
        private int coin;
        private Map<String, Map<String, Integer>> mats; // 素材名 → レア度 → 個数
        private Map<String, Double> params;
    }

    private static Map<String, PartData> config;

    private SubShipService() {
    }

    public static synchronized Map<String, PartData> getConfig() {
        if (config == null) {
            config = new LinkedHashMap<>();
            try (InputStream is = SubShipService.class.getResourceAsStream(CONFIG_PATH)) {
                if (is == null)
                    throw new IOException("Resource not found: " + CONFIG_PATH);
                config = new ObjectMapper().readValue(is, new TypeReference<LinkedHashMap<String, PartData>>() {
                });
            } catch (IOException e) {
                LogUtils.error("[ERROR]", component,
                        "Failed to load configuration file: " + ANSI.YELLOW + CONFIG_PATH + ANSI.RESET, e);
            }
        }
        return config;
    }

    // ----- 状態 -----
    public static int getTier(String kind) {
        UserData ud = DataManager.getUserData();
        return ud == null ? 0 : ud.getSubShipParts().getOrDefault(kind, 0);
    }

    public static int maxTier(String kind) {
        PartData p = getConfig().get(kind);
        return p == null ? 0 : p.getTiers().size();
    }

    /**
     * 必須パーツがすべて揃っているか
     */
    public static boolean isAssembled() {
        for (Map.Entry<String, PartData> e : getConfig().entrySet())
            if (e.getValue().isRequired() && getTier(e.getKey()) < 1)
                return false;
        return true;
    }

    /**
     * サブ機が装備されているか(完成していて、外していない)
     */
    public static boolean isEquipped() {
        UserData ud = DataManager.getUserData();
        return ud != null && ud.getSubShip() != null && isAssembled();
    }

    public static void setEquipped(boolean on) {
        UserData ud = DataManager.getUserData();
        if (ud == null)
            return;
        ud.setSubShip(on ? equippedValue() : null);
        DataManager.save();
    }

    // 使うフレームの指定(0なら制作済みの最高Mk)。装備中は "Custom:N" として保存する
    private static int hullChoice = 0;

    private static String equippedValue() {
        return hullChoice > 0 ? EQUIPPED + ":" + hullChoice : EQUIPPED;
    }

    /**
     * 使うフレームの指定(0なら最高Mk)
     */
    public static int getHullChoice() {
        UserData ud = DataManager.getUserData();
        if (ud != null && ud.getSubShip() != null && ud.getSubShip().startsWith(EQUIPPED + ":")) {
            try {
                hullChoice = Integer.parseInt(ud.getSubShip().substring(EQUIPPED.length() + 1));
            } catch (NumberFormatException ignored) {
                hullChoice = 0;
            }
        }
        return hullChoice;
    }

    /**
     * 実際に使うフレームのMk(制作済みのMkの範囲内。未制作なら0)
     */
    public static int hullTier() {
        int crafted = getTier("HULL");
        if (crafted < 1)
            return 0;
        int c = getHullChoice();
        return c < 1 || c > crafted ? crafted : c;
    }

    /**
     * 使うフレームの切り替え(制作済みのMkの中で循環する)
     * 
     * @param delta +1で次、-1で前
     */
    public static void cycleHull(int delta) {
        int crafted = getTier("HULL");
        if (crafted < 2)
            return;
        int next = hullTier() + delta;
        if (next > crafted)
            next = 1;
        if (next < 1)
            next = crafted;
        hullChoice = next == crafted ? 0 : next;

        UserData ud = DataManager.getUserData();
        if (ud != null && ud.getSubShip() != null)
            ud.setSubShip(equippedValue());
        DataManager.save();
    }

    /**
     * 使うフレームの名前(例: "Mk3 ストライカー")
     */
    public static String hullLabel() {
        int t = hullTier();
        PartData p = getConfig().get("HULL");
        if (p == null || t < 1)
            return "未制作";
        return p.getTiers().get(t - 1).getLabel();
    }

    /**
     * 現在のMkのパラメータ値(未制作なら既定値)
     */
    public static double param(String kind, String key, double def) {
        int tier = "HULL".equals(kind) ? hullTier() : getTier(kind); // フレームは選んだMkを使う
        PartData p = getConfig().get(kind);
        if (p == null || tier < 1)
            return def;
        TierData t = p.getTiers().get(Math.min(tier, p.getTiers().size()) - 1);
        return t.getParams() == null ? def : t.getParams().getOrDefault(key, def);
    }

    /**
     * 指定したMkのパラメータ値(プレビューなど、まだ制作していないMkを見るとき用)
     */
    public static double tierParam(String kind, int tier, String key, double def) {
        PartData p = getConfig().get(kind);
        if (p == null || tier < 1)
            return def;
        TierData t = p.getTiers().get(Math.min(tier, p.getTiers().size()) - 1);
        return t.getParams() == null ? def : t.getParams().getOrDefault(key, def);
    }

    /**
     * フレームのMkごとの大きさ倍率
     */
    public static double hullSize(int tier) {
        return tierParam("HULL", tier, "size", 1.0);
    }

    /**
     * 自機の中心からサブ機までの横の距離(画面倍率なし。大きいフレームほど外側に付く)
     */
    public static double sideOffset() {
        return 40 + 9 * hullSize(Math.max(1, hullTier()));
    }

    /**
     * サブ機の弾のダメージ倍率(フレームとスキルを反映)
     */
    public static double damageScale() {
        return BASE_DAMAGE_SCALE * param("HULL", "damage", 1.0) * (1 + SkillService.getBonus("SUB_POWER"));
    }

    /**
     * サブ機の連射速度の倍率(推進機とスキルを反映)
     */
    public static double rateScale() {
        return param("ENGINE", "rate", 1.0) * (1 + SkillService.getBonus("SUB_RATE"));
    }

    /**
     * サブ機のビームのダメージ倍率(フレーム・スキルを反映)
     */
    public static double beamScale() {
        return param("HULL", "damage", 1.0) * (1 + SkillService.getBonus("SUB_POWER"))
                * (1 + SkillService.getBonus("BEAM_POWER"));
    }

    // ----- 制作 -----
    /**
     * 次のMkの制作コスト(最大Mkならnull)
     */
    public static Cost getCost(String kind) {
        PartData p = getConfig().get(kind);
        int tier = getTier(kind);
        if (p == null || tier >= p.getTiers().size())
            return null;

        TierData next = p.getTiers().get(tier);
        Map<ResourceKinds, Integer> items = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : next.getMats().entrySet()) {
            ResourceName name = ResourceName.valueOf(e.getKey());
            // パーツ素材以外はコストに指定できない
            if (name.getFamily() != Family.PART)
                continue;
            for (Map.Entry<String, Integer> r : e.getValue().entrySet())
                items.put(new ResourceKinds(name, Rarity.valueOf(r.getKey())), r.getValue());
        }
        return new Cost(next.getCoin(), items);
    }

    public static boolean canAfford(Cost cost) {
        // コインと素材の所持確認は強化と共通
        return cost != null && SkillService.canAfford(cost);
    }

    /**
     * パーツの制作(または次のMkへの改良)
     * 
     * @return 制作できたらtrue
     */
    public static synchronized boolean craft(String kind) {
        UserData ud = DataManager.getUserData();
        Cost cost = getCost(kind);
        if (ud == null || cost == null || !canAfford(cost))
            return false;

        boolean wasAssembled = isAssembled();

        SkillService.pay(ud, cost);
        ud.getSubShipParts().put(kind, getTier(kind) + 1);

        // フレームを強化したときは、新しいフレームを使う
        if ("HULL".equals(kind)) {
            hullChoice = 0;
            if (ud.getSubShip() != null)
                ud.setSubShip(equippedValue());
        }

        // 初めて完成したときは自動で装備する
        if (!wasAssembled && isAssembled())
            setEquipped(true);

        DataManager.save();
        LogUtils.systemLog("[INFO]", component,
                ANSI.YELLOW + kind + ANSI.RESET + " crafted: Mk" + getTier(kind) + ".");
        return true;
    }

    // ----- 表示 -----
    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9)
            return String.valueOf((long) Math.rint(v));
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * Mkごとの効果の説明
     */
    public static String effectText(String kind, TierData t) {
        Map<String, Double> p = t.getParams();
        if (p == null)
            return "";
        return switch (kind) {
            case "HULL" -> "弾ダメージ x" + fmt(p.getOrDefault("damage", 1.0))
                    + " ／ 機体サイズ x" + fmt(p.getOrDefault("size", 1.0));
            case "CANNON" -> "弾数 " + fmt(p.getOrDefault("shots", 1.0))
                    + "発 ／ 発射間隔 " + fmt(p.getOrDefault("interval", 1.0)) + "秒";
            case "ENGINE" -> "連射速度 x" + fmt(p.getOrDefault("rate", 1.0))
                    + " ／ 追従の遅れ " + fmt(p.getOrDefault("delay", 0.3)) + "秒";
            case "CORE" -> "誘導ミサイル " + fmt(p.getOrDefault("count", 1.0))
                    + "発 ／ " + fmt(p.getOrDefault("interval", 2.0)) + "秒ごと";
            case "EMITTER" -> "貫通ビーム 毎秒" + fmt(p.getOrDefault("dps", 0.0)) + "ダメージ ／ 照射 "
                    + fmt(p.getOrDefault("duration", 0.0)) + "秒 ／ " + fmt(p.getOrDefault("interval", 0.0)) + "秒ごと";
            default -> "";
        };
    }

    /**
     * 詳細パネルに表示する文章(コストは別の部品で表示する)
     */
    public static String describe(String kind) {
        PartData p = getConfig().get(kind);
        if (p == null)
            return "";
        int tier = getTier(kind);
        StringBuilder sb = new StringBuilder();
        sb.append(p.getTitle()).append(p.isRequired() ? "  [必須]" : "  [任意]").append("\n");
        sb.append(p.getDetail()).append("\n");

        if (tier < 1) {
            sb.append("現在: 未制作\n");
        } else {
            sb.append("現在: ").append(p.getTiers().get(tier - 1).getLabel()).append("\n");
            sb.append("  ").append(effectText(kind, p.getTiers().get(tier - 1))).append("\n");
        }

        if (tier >= p.getTiers().size())
            return sb.append("最大まで強化済み").toString();

        TierData next = p.getTiers().get(tier);
        sb.append(tier < 1 ? "制作すると: " : "強化すると: ").append(next.getLabel()).append("\n");
        sb.append("  ").append(effectText(kind, next));
        return sb.toString();
    }

    /**
     * コスト欄の下に出す一言(作れるかどうかなど)
     */
    public static String status(String kind) {
        Cost cost = getCost(kind);
        if (cost == null)
            return "これ以上は強化できません";
        String verb = getTier(kind) < 1 ? "制作" : "強化";
        if (canAfford(cost))
            return verb + "できます";
        return "コインかパーツ素材が足りません\n(強化用の素材はパーツに使えません)";
    }

    /**
     * 全体の状態の説明
     */
    public static String summary() {
        StringBuilder sb = new StringBuilder();
        if (!isAssembled()) {
            sb.append("サブ機: 未完成\n必須パーツ ");
            boolean first = true;
            for (Map.Entry<String, PartData> e : getConfig().entrySet()) {
                if (!e.getValue().isRequired())
                    continue;
                if (!first)
                    sb.append("  ");
                sb.append(e.getValue().getTitle()).append(getTier(e.getKey()) >= 1 ? "○" : "×");
                first = false;
            }
        } else {
            sb.append("サブ機: ").append(isEquipped() ? "装備中" : "取り外し中").append("\n");
            sb.append("弾ダメージ x").append(fmt(damageScale()));
            sb.append(" ／ 連射 x").append(fmt(rateScale()));
        }
        return sb.toString();
    }
}
