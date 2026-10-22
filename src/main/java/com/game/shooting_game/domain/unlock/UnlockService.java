package com.game.shooting_game.domain.unlock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.utils.LogUtils;

/**
 * プレイヤーレベルによる機能の解放
 * <p>
 * 解放レベルは UnlockConfig.json(機能名 → レベル)で決める。書かれていない機能は既定値を使う。
 * 未開放のボタンは {@link #guard} で登録しておくと、グレーアウトして押すと解放条件を案内する。
 * 「どこまで案内したか」は progress.highScores のキー {@code "unlock:seen"} に記録する。
 */
public final class UnlockService {
    private static final String component = "[UnlockService]";
    private static final String CONFIG_PATH = "/static/configs/UnlockConfig.json";
    private static final String SEEN_KEY = "unlock:seen";

    /** 解放される機能 */
    public enum Feature {
        SHOP("ショップ", 5),
        MODULE("モジュール", 10),
        EXTRA_MODULE("MODULE ステージ", 10),
        SUBSHIP("サブ機工房", 15),
        INFINITY("INFINITY MODE", 20),
        EXTRA_BOSS("BOSS ステージ", 25),
        EXTRA_MATERIAL("MATERIAL ステージ", 25),
        EXTRA_SPECIAL("SPECIAL ステージ", 30);

        private final String label;
        private final int defaultLevel;

        Feature(String label, int defaultLevel) {
            this.label = label;
            this.defaultLevel = defaultLevel;
        }

        public String label() {
            return label;
        }
    }

    private static Map<Feature, Integer> levels;

    private record Guarded(AbstractButton button, Feature feature, String text) {
    }

    private static final List<Guarded> guarded = new ArrayList<>();

    private UnlockService() {
    }

    // ----- 設定 -----
    private static synchronized void ensureLoaded() {
        if (levels == null)
            reload();
    }

    /** 設定を読み込み直す(失敗したら既定値) */
    public static synchronized void reload() {
        Map<Feature, Integer> m = new EnumMap<>(Feature.class);
        for (Feature f : Feature.values())
            m.put(f, f.defaultLevel);
        try (InputStream is = UnlockService.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null)
                throw new IllegalStateException("設定ファイルが見つかりません: " + CONFIG_PATH);
            JsonNode root = new ObjectMapper().readTree(is);
            for (Feature f : Feature.values())
                if (root.has(f.name()))
                    m.put(f, Math.max(1, root.get(f.name()).asInt(f.defaultLevel)));
        } catch (Exception e) {
            LogUtils.warn(component, "解放の設定を読めないため既定値を使います: " + e.getMessage());
        }
        levels = m;
    }

    // ----- 判定 -----
    /** その機能の解放レベル */
    public static int requiredLevel(Feature f) {
        ensureLoaded();
        return levels.get(f);
    }

    /** 解放済みか判定する純粋関数 */
    public static boolean isUnlocked(int playerLevel, int requiredLevel) {
        return playerLevel >= requiredLevel;
    }

    public static int playerLevel() {
        return DataManager.getUserData() == null ? 1 : DataManager.getUserLevel().level();
    }

    public static boolean isUnlocked(Feature f) {
        return isUnlocked(playerLevel(), requiredLevel(f));
    }

    /** 未開放のときの案内文 */
    public static String lockedMessage(Feature f) {
        return f.label() + " は プレイヤー Lv" + requiredLevel(f) + " で解放されます(いま Lv" + playerLevel() + ")";
    }

    // ----- ボタン -----
    /**
     * ボタンを解放条件つきにする(未開放ならグレーアウトし、押すと案内を出す)
     */
    public static void guard(AbstractButton button, Feature feature) {
        guarded.removeIf(g -> g.button == button);
        guarded.add(new Guarded(button, feature, button.getText()));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isUnlocked(feature))
                    JOptionPane.showMessageDialog(ShootingGameMain.getInstance(), lockedMessage(feature),
                            "未開放", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        refresh(guarded.getLast());
    }

    /** 登録したボタンの見た目を、いまのレベルに合わせ直す(画面に入るたびに呼ぶ) */
    public static void refreshAll() {
        for (Guarded g : guarded)
            refresh(g);
    }

    private static void refresh(Guarded g) {
        boolean ok = isUnlocked(g.feature);
        g.button.setEnabled(ok);
        g.button.setToolTipText(ok ? null : "Lv" + requiredLevel(g.feature) + " で解放");
        if (g.text != null && !g.text.isEmpty())
            g.button.setText(ok ? g.text : g.text + " 🔒Lv" + requiredLevel(g.feature));
    }

    // ----- 解放の通知 -----
    /**
     * 前回の案内から新しく解放された機能の一覧を返し、案内済みにする(なければ空)
     */
    public static List<Feature> takeNewlyUnlocked() {
        List<Feature> result = new ArrayList<>();
        if (DataManager.getUserData() == null)
            return result;
        Map<String, Integer> store = DataManager.getUserData().getProgress().getHighScores();
        int seen = store.getOrDefault(SEEN_KEY, 0);
        int now = playerLevel();
        for (Feature f : Feature.values())
            if (requiredLevel(f) > seen && requiredLevel(f) <= now)
                result.add(f);
        store.put(SEEN_KEY, now);
        return result;
    }

    /** 新しく解放された機能があれば案内する */
    public static void announceNewlyUnlocked() {
        List<Feature> list = takeNewlyUnlocked();
        if (list.isEmpty())
            return;
        StringBuilder sb = new StringBuilder("新しい機能が解放されました!\n");
        for (Feature f : list)
            sb.append("・").append(f.label()).append("(Lv").append(requiredLevel(f)).append(")\n");
        JOptionPane.showMessageDialog(ShootingGameMain.getInstance(), sb.toString(), "解放",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
