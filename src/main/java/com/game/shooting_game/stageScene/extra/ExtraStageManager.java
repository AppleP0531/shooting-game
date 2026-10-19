package com.game.shooting_game.stageScene.extra;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.stage.DailyService;
import com.game.shooting_game.gameScene.stage.EndlessWaves;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.gameScene.stage.StageManager.ModuleReward;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.module.ModuleKind;
import com.game.shooting_game.module.ModuleService;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * エクストラモード
 * 通常ステージとは別の、特別なステージの一覧。番号は100以上(StageConfig.jsonの "stage101" など)。
 * クリアで素材やモジュールが手に入る。通常ステージの進行度(クリア済みステージ)には影響しない。
 * 解放条件は、通常ステージのクリア状況(StageData.unlock)。
 */
public final class ExtraStageManager extends JPanel implements Manager {
    private static final String component = "[ExtraStageManager]";
    private static final int EXTRA_BASE_ID = 100;
    private static ExtraStageManager instance;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final StageManager STAGE_M;

    private final List<JButton> stageBtns = new ArrayList<>();
    private JButton goBackBtn;
    private JPanel detailPanel;
    private JTextArea detailText;
    private JButton startBtn;
    private int selectedId = -1;
    private final List<JButton> tabBtns = new ArrayList<>();
    private String category = "MODULE";

    /** 分類(キー, 表示名) */
    private static final String[][] CATEGORIES = { { "MODULE", "モジュール" }, { "BOSS", "ボス" },
            { "MATERIAL", "素材" }, { "SPECIAL", "特別" } };

    private ExtraStageManager() {
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        STAGE_M = StageManager.getInstance();
    }

    public static ExtraStageManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, ExtraStageManager::new);
    }

    private static int px(double base) {
        return (int) (base * ShootingGameMain.getScreenSizeAdj());
    }

    @Override
    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        Vector2 size = new Vector2(px(200), px(44));
        goBackBtn = UIBuilder.createImageBtn(component, "/static/img/UI/selectStageScene/goBackImage.png", size,
                "GO BACK");
        goBackBtn.setBounds(MAIN.getWidth() / 2 - (int) size.getX() / 2,
                MAIN.getHeight() - (int) size.getY() - px(10), (int) size.getX(), (int) size.getY());
        goBackBtn.addActionListener(e -> SM.changeToPrevScene());
        this.add(goBackBtn);

        // 分類の切り替え(モジュール系統 / ボス系統 / 素材系統 / 特別)
        int tw = px(88);
        for (int i = 0; i < CATEGORIES.length; i++) {
            String key = CATEGORIES[i][0];
            JButton t = new JButton(CATEGORIES[i][1]);
            t.setMargin(new Insets(0, 0, 0, 0));
            t.setFocusPainted(false);
            t.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(13))));
            t.setBounds(MAIN.getWidth() / 2 - px(180) + i * (tw + px(2)), px(76), tw, px(34));
            t.addActionListener(e -> {
                category = key;
                rebuild();
            });
            this.add(t);
            tabBtns.add(t);
        }

        // ステージの詳細(押したステージの説明と、報酬の内容)
        detailPanel = new JPanel();
        detailPanel.setLayout(null);
        detailPanel.setVisible(false);
        int dw = px(350);
        int dh = px(330);
        detailPanel.setBounds(MAIN.getWidth() / 2 - dw / 2, MAIN.getHeight() / 2 - dh / 2, dw, dh);
        detailText = new JTextArea();
        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setOpaque(false);
        detailText.setForeground(Color.WHITE);
        detailText.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
        detailText.setBounds(px(8), px(6), dw - px(16), dh - px(66));
        detailPanel.add(detailText);
        startBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/startStage.png",
                new Vector2(px(100), px(46)), "START");
        startBtn.setBounds(dw / 2 - startBtn.getWidth() / 2, dh - startBtn.getHeight() - px(8),
                startBtn.getWidth(), startBtn.getHeight());
        startBtn.addActionListener(e -> {
            UIBuilder.closePanel(detailPanel);
            STAGE_M.getStage().setStageId(selectedId);
            SM.changeScene(SceneType.GAME);
        });
        detailPanel.add(startBtn);
        this.add(detailPanel);
    }

    /**
     * エクストラステージの一覧(番号順)
     */
    private List<Integer> extraIds() {
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<String, StageData> e : STAGE_M.getStageConfig().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("stage") || !key.substring(5).chars().allMatch(Character::isDigit)
                    || key.length() == 5)
                continue;
            int id = Integer.parseInt(key.substring(5));
            if (id > EXTRA_BASE_ID && id != EndlessWaves.STAGE_ID
                    && category.equals(STAGE_M.getStageConfig().get(key).getCategory()))
                ids.add(id);
        }
        ids.sort(Integer::compare);
        return ids;
    }

    private boolean unlocked(StageData d) {
        return DataManager.getUserData().getProgress().getClearedStage() >= d.getUnlock();
    }

    /**
     * ボタンの作り直し(クリア状況が変わるので、入るたびに更新)
     */
    private void rebuild() {
        for (JButton b : stageBtns)
            this.remove(b);
        stageBtns.clear();

        // 選んでいる分類を見分けられるようにする
        for (int i = 0; i < tabBtns.size(); i++) {
            boolean sel = CATEGORIES[i][0].equals(category);
            tabBtns.get(i).setText(sel ? "【" + CATEGORIES[i][1] + "】" : CATEGORIES[i][1]);
        }

        int y = 122;
        for (int id : extraIds()) {
            StageData d = STAGE_M.getStageConfig().get("stage" + id);
            boolean open = unlocked(d);
            String score = String.valueOf(DataManager.getUserData().getProgress().getHighScores()
                    .getOrDefault("stage" + id, 0));
            String text = open
                    ? "<html><b>" + d.getTitle() + "</b>　<font size=-2>最高スコア " + score + "</font></html>"
                    : "<html><b>" + d.getTitle() + "</b>　<font size=-2>ステージ" + d.getUnlock()
                            + "クリアで解放</font></html>";
            JButton b = new JButton(text);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(13))));
            b.setBounds(MAIN.getWidth() / 2 - px(180), px(y), px(360), px(40));
            b.setEnabled(open);
            b.addActionListener(e -> showDetail(id));
            this.add(b);
            stageBtns.add(b);
            y += 46;
        }
        repaint();
    }

    private static String chipLine(StageData d) {
        StringBuilder sb = new StringBuilder();
        if (d.getRewardList() == null)
            return "";
        for (Map.Entry<ResourceName, Map<Rarity, String>> e : d.getRewardList().entrySet())
            for (Map.Entry<Rarity, String> r : e.getValue().entrySet())
                sb.append(e.getKey().getDisplayName()).append(" ").append(ModuleService.rarityName(r.getKey()))
                        .append(" ").append(r.getValue()).append("\n");
        return sb.toString();
    }

    private void showDetail(int id) {
        selectedId = id;
        StageData d = STAGE_M.getStageConfig().get("stage" + id);
        StringBuilder sb = new StringBuilder();
        sb.append(d.getDescription()).append("\n");
        // ボスチャレンジなどの固定条件(デイリーは説明文に含まれている)
        if (id != DailyService.STAGE_ID && d.getModifiers() != null)
            sb.append("\n【条件】\n").append(StageModifiers.describe(d.getModifiers()));
        sb.append("\n【クリア報酬】\n");
        if (d.getModuleRewards() != null) {
            for (ModuleReward r : d.getModuleRewards()) {
                String kind = r.getKind() == null ? "モジュール(種類はランダム)"
                        : ModuleKind.valueOf(r.getKind()).getTitle();
                sb.append(kind).append("  ").append(ModuleService.rarityName(Rarity.valueOf(r.getRarity())))
                        .append("  ").append(Math.round(r.getChance() * 100)).append("%");
                if ("FIRST".equals(r.getOnce()))
                    sb.append("  (初クリアのみ)");
                if ("DAILY".equals(r.getOnce())) {
                    boolean got = DataManager.getUserData().getProgress().getHighScores()
                            .containsKey("daily:" + java.time.LocalDate.now() + ":stage" + id);
                    sb.append(got ? "  (本日は受取済み)" : "  (1日1回)");
                }
                sb.append("\n");
            }
        }
        sb.append(chipLine(d)).append("\n");
        sb.append("最高スコア ").append(DataManager.getUserData().getProgress().getHighScores()
                .getOrDefault("stage" + id, 0));
        detailText.setText(sb.toString());
        detailText.setCaretPosition(0);
        UIBuilder.openPanel(detailPanel, d.getTitle(), true, null);
    }

    @Override
    public void setup() {
        DailyService.refresh(); // 今日のデイリーチャレンジを作る
        rebuild();
    }

    @Override
    public void update(double dt) {
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, px(18))));
        g.drawString("EXTRA MODE", MAIN.getWidth() / 2 - px(60), px(40));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g.setColor(new Color(190, 220, 255));
        g.drawString("特別なステージ(通常ステージの進行には影響しない)", px(20), px(66));
    }
}
