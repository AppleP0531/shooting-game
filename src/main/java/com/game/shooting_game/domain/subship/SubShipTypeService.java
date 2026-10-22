package com.game.shooting_game.domain.subship;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.utils.LogUtils;

/**
 * サブ機のタイプ(タイプごとに撃ち方と強化のしかたが違う)
 * <p>
 * タイプの一覧とレベルごとの撃ち方は SubShipTypeConfig.json で決める(先頭が標準で、いつでも使える)。
 * 解放したタイプと選択中のタイプは progress.highScores に記録する
 * (キー {@code "subtype:own:ID"} = 解放済み、{@code "subtype:sel"} = 選択中の番号)。
 * タイプごとのレベルは SubShipService が記録する。
 */
public final class SubShipTypeService {
    private static final String component = "[SubShipTypeService]";
    private static final String CONFIG_PATH = "/static/configs/SubShipTypeConfig.json";
    private static final String OWN_PREFIX = "subtype:own:";
    private static final String SELECTED_KEY = "subtype:sel";

    /** ビーム(あるレベルから撃つ) */
    public record BeamDef(double dps, double width, double duration, double interval) {
    }

    /**
     * 1レベル分の撃ち方
     *
     * @param straight      通常弾の数(0なら撃たない)
     * @param spread        通常弾の広がり(度。弾と弾の間の角度)
     * @param damage        弾のダメージ倍率
     * @param interval      通常弾の発射間隔(秒)
     * @param tracking      追尾弾の数(0なら撃たない)
     * @param trackInterval 追尾弾の発射間隔(秒)
     * @param beam          ビーム(なければ null)
     */
    public record LevelDef(int straight, double spread, double damage, double interval,
            int tracking, double trackInterval, BeamDef beam) {
    }

    /** タイプ1つ分 */
    public record TypeDef(String id, String title, String detail, int unlockCoin,
            int upgradeCoin, double upgradeGrowth, List<String> mats, List<LevelDef> levels) {
        public int maxLevel() {
            return levels.size();
        }

        /** レベル(1〜maxLevel。範囲外は丸める)の撃ち方 */
        public LevelDef at(int level) {
            return levels.get(Math.max(1, Math.min(level, levels.size())) - 1);
        }
    }

    private static List<TypeDef> types;

    private SubShipTypeService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (types == null)
            reload();
    }

    /** 設定を読み込み直す(失敗したら標準タイプだけ) */
    public static synchronized void reload() {
        List<TypeDef> loaded = new ArrayList<>();
        try (InputStream is = SubShipTypeService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            for (JsonNode n : new ObjectMapper().readTree(is))
                loaded.add(parseType(n));
        } catch (Exception e) {
            LogUtils.warn(component, "サブ機タイプの設定を読めないため標準のみにします: " + e.getMessage());
            loaded.clear();
        }
        if (loaded.isEmpty())
            loaded.add(new TypeDef("STANDARD", "スタンダード", "", 0, 12000, 1.75, List.of("IRON_BAR"),
                    List.of(new LevelDef(1, 0, 1.0, 0.6, 0, 2.0, null))));
        types = List.copyOf(loaded);
    }

    private static TypeDef parseType(JsonNode n) {
        List<LevelDef> levels = new ArrayList<>();
        for (JsonNode l : n.path("levels")) {
            BeamDef beam = null;
            JsonNode b = l.path("beam");
            if (b.isObject())
                beam = new BeamDef(b.path("dps").asDouble(20), b.path("width").asDouble(14),
                        b.path("duration").asDouble(0.8), b.path("interval").asDouble(8));
            levels.add(new LevelDef(
                    Math.max(0, l.path("straight").asInt(0)),
                    l.path("spread").asDouble(0),
                    l.path("damage").asDouble(1.0),
                    Math.max(0.05, l.path("interval").asDouble(0.6)),
                    Math.max(0, l.path("tracking").asInt(0)),
                    Math.max(0.05, l.path("trackInterval").asDouble(2.0)),
                    beam));
        }
        if (levels.isEmpty())
            levels.add(new LevelDef(1, 0, 1.0, 0.6, 0, 2.0, null));
        List<String> mats = new ArrayList<>();
        for (JsonNode m : n.path("mats"))
            mats.add(m.asText());
        return new TypeDef(n.path("id").asText(),
                n.path("title").asText(n.path("id").asText()),
                n.path("detail").asText(""),
                Math.max(0, n.path("unlockCoin").asInt(0)),
                Math.max(0, n.path("upgradeCoin").asInt(10000)),
                n.path("upgradeGrowth").asDouble(1.75),
                List.copyOf(mats),
                List.copyOf(levels));
    }

    public static List<TypeDef> types() {
        ensureLoaded();
        return types;
    }

    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    // ----- 状態 -----
    /** 解放済みか(解放コスト0のタイプは最初から使える) */
    public static boolean isUnlocked(TypeDef t) {
        Map<String, Integer> s = store();
        return t.unlockCoin() <= 0 || (s != null && s.containsKey(OWN_PREFIX + t.id()));
    }

    /** 選択中のタイプ(未解放を指していたら先頭の標準タイプ) */
    public static TypeDef current() {
        List<TypeDef> list = types();
        Map<String, Integer> s = store();
        int idx = s == null ? 0 : s.getOrDefault(SELECTED_KEY, 0);
        TypeDef t = idx >= 0 && idx < list.size() ? list.get(idx) : list.get(0);
        return isUnlocked(t) ? t : list.get(0);
    }

    /** 選択を切り替える(未解放のタイプは選べない) */
    public static boolean select(TypeDef t) {
        Map<String, Integer> s = store();
        int idx = types().indexOf(t);
        if (s == null || idx < 0 || !isUnlocked(t))
            return false;
        s.put(SELECTED_KEY, idx);
        return true;
    }

    /** 前後のタイプへ(解放済みかどうかに関係なく順に見られる) */
    public static TypeDef neighbor(TypeDef from, int delta) {
        List<TypeDef> list = types();
        int i = Math.floorMod(list.indexOf(from) + delta, list.size());
        return list.get(i);
    }

    public static boolean canUnlock(TypeDef t) {
        UserData ud = DataManager.getUserData();
        return ud != null && !isUnlocked(t)
                && (DataManager.isAdminMode() || ud.getResources().getCoins() >= t.unlockCoin());
    }

    /** コインを払って解放する(払えなければ false。管理者モードは無料) */
    public static boolean unlock(TypeDef t) {
        if (!canUnlock(t))
            return false;
        UserData ud = DataManager.getUserData();
        if (!DataManager.isAdminMode())
            ud.getResources().setCoins(ud.getResources().getCoins() - t.unlockCoin());
        store().put(OWN_PREFIX + t.id(), 1);
        return true;
    }
}
