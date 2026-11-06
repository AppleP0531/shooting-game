package com.game.shooting_game.domain.tutorial;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.utils.LogUtils;

/**
 * チュートリアル(1度だけ出す操作ガイド)
 * <p>
 * ガイドは TutorialConfig.json に、ID・きっかけ・ページの説明文で書く。きっかけは2種類:
 * <ul>
 * <li>{@code scene} 画面の名前(SceneType)。その画面に初めて入ったとき</li>
 * <li>{@code event} できごとの名前(例: stage1.start)。ゲーム中などで、そのできごとが初めて起きたとき</li>
 * </ul>
 * 見たガイドは progress.highScores の {@code tutorial:ID}(値1)に記録する(★の数に混ざらない接頭辞)。
 */
public final class TutorialService {
    private static final String component = "[TutorialService]";
    private static final String CONFIG_PATH = "/static/configs/TutorialConfig.json";
    private static final String SEEN_PREFIX = "tutorial:";

    /** 1つのガイド(scene と event はどちらか一方。ないほうは null) */
    public record Guide(String id, String scene, String event, String title, List<String> pages) {
    }

    private static List<Guide> guides;

    private TutorialService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (guides == null)
            reload();
    }

    /** 設定を読み込み直す(読めなければ空) */
    public static synchronized void reload() {
        List<Guide> loaded = new ArrayList<>();
        try (InputStream is = TutorialService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            for (JsonNode n : new ObjectMapper().readTree(is).path("guides")) {
                List<String> pages = new ArrayList<>();
                for (JsonNode p : n.path("pages"))
                    pages.add(p.asText());
                if (pages.isEmpty())
                    continue;
                loaded.add(new Guide(n.path("id").asText(), blankToNull(n.path("scene").asText(null)),
                        blankToNull(n.path("event").asText(null)), n.path("title").asText(""), List.copyOf(pages)));
            }
        } catch (Exception e) {
            LogUtils.warn(component, "チュートリアルの設定を読めません: " + e.getMessage());
            loaded.clear();
        }
        guides = List.copyOf(loaded);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public static List<Guide> all() {
        ensureLoaded();
        return guides;
    }

    // ----- 見たか -----
    private static Map<String, Integer> store() {
        UserData ud = DataManager.getUserData();
        return ud == null || ud.getProgress() == null ? null : ud.getProgress().getHighScores();
    }

    public static boolean seen(Guide g) {
        Map<String, Integer> s = store();
        return s != null && s.containsKey(SEEN_PREFIX + g.id());
    }

    public static void markSeen(Guide g) {
        Map<String, Integer> s = store();
        if (s != null)
            s.put(SEEN_PREFIX + g.id(), 1);
    }

    /** 見た記録をすべて消す(もう一度ガイドを出す)。消した数を返す */
    public static int reset() {
        Map<String, Integer> s = store();
        if (s == null)
            return 0;
        int before = s.size();
        s.keySet().removeIf(k -> k.startsWith(SEEN_PREFIX));
        return before - s.size();
    }

    // ----- 探す -----
    /** その画面のまだ見ていないガイド(なければ null。データがなければ出さない) */
    public static Guide forScene(String sceneName) {
        if (store() == null)
            return null;
        for (Guide g : all())
            if (sceneName.equals(g.scene()) && !seen(g))
                return g;
        return null;
    }

    /** そのできごとのまだ見ていないガイド(なければ null。データがなければ出さない) */
    public static Guide forEvent(String event) {
        if (store() == null)
            return null;
        for (Guide g : all())
            if (event.equals(g.event()) && !seen(g))
                return g;
        return null;
    }

    /** 見ていないガイドの数 */
    public static int unseenCount() {
        Map<String, Integer> s = store();
        if (s == null)
            return 0;
        int n = 0;
        for (Guide g : all())
            if (!seen(g))
                n++;
        return n;
    }

    /** ガイドの一覧(ID → 見たか。コマンドの表示用) */
    public static Map<String, Boolean> status() {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (Guide g : all())
            m.put(g.id(), seen(g));
        return m;
    }
}
