package com.game.shooting_game.stageScene.extra;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.gameScene.stage.EndlessWaves;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.gameScene.stage.StageManager.ModuleReward;
import com.game.shooting_game.gameScene.stage.StageManager.StageData;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.domain.module.ModuleKind;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.stageScene.DifficultyPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * エクストラ(分類ごとに1シーン: モジュール/ボス/素材/特別)
 * 通常ステージとは別の、特別なステージの一覧。番号は100以上(StageConfig.jsonの "stage101" など)。
 * クリアで素材やモジュールが手に入る。通常ステージの進行度(クリア済みステージ)には影響しない。
 * 解放条件は、通常ステージのクリア状況(StageData.unlock)。
 */
public final class ExtraStageManager extends JPanel implements Manager {
    private static final String component = "[ExtraStageManager]";
    private static final int EXTRA_BASE_ID = 100;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final StageManager STAGE_M;

    private final List<JButton> stageBtns = new ArrayList<>();
    private JButton goBackBtn;
    private JPanel detailPanel;
    private JTextArea detailText;
    private JButton startBtn;
    private DifficultyPanel difficultyPanel; // ボス分類だけ、難易度を選べる
    private int selectedId = -1;
    private final String category;
    private final String categoryTitle;

    /** 分類ごとに1つずつ作る(キー, 表示名) */
    public static final String[][] CATEGORIES = { { "MODULE", "モジュール" }, { "BOSS", "ボス" },
            { "MATERIAL", "素材" }, { "SPECIAL", "特別" } };

    public ExtraStageManager(String category, String categoryTitle) {
        this.category = category;
        this.categoryTitle = categoryTitle;
        MAIN = ShootingGameMain.getInstance();
        SM = SceneManager.getInstance();
        STAGE_M = StageManager.getInstance();
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

        // ステージの詳細(押したステージの説明と、報酬の内容)
        detailPanel = new JPanel();
        detailPanel.setLayout(null);
        detailPanel.setVisible(false);
        int dw = px(350);
        boolean pickDifficulty = "BOSS".equals(category);
        int diffH = pickDifficulty ? DifficultyPanel.height() + px(6) : 0;
        int dh = px(330) + diffH;
        detailPanel.setBounds(MAIN.getWidth() / 2 - dw / 2, MAIN.getHeight() / 2 - dh / 2, dw, dh);
        detailText = new JTextArea();
        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setOpaque(false);
        detailText.setForeground(Color.WHITE);
        detailText.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
        detailText.setBounds(px(8), px(6), dw - px(16), dh - px(66) - diffH);
        detailPanel.add(detailText);
        if (pickDifficulty) {
            difficultyPanel = new DifficultyPanel(dw - px(16), d -> {
                STAGE_M.getStage().setDifficulty(d);
                renderDetail(d);
            });
            difficultyPanel.setLocation(px(8), dh - px(62) - DifficultyPanel.height());
            detailPanel.add(difficultyPanel);
        }
        startBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/startStage.png",
                new Vector2(px(100), px(46)), "START");
        startBtn.setBounds(dw / 2 - startBtn.getWidth() / 2, dh - startBtn.getHeight() - px(8),
                startBtn.getWidth(), startBtn.getHeight());
        startBtn.addActionListener(e -> {
            UIBuilder.closePanel(detailPanel);
            Difficulty diff = difficultyPanel == null ? Difficulty.NORMAL : difficultyPanel.getSelected();
            STAGE_M.getStage().setStageId(selectedId);
            STAGE_M.getStage().setDifficulty(diff);
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

        int y = 90;
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

    private static String chipLine(StageData d, Difficulty diff) {
        StringBuilder sb = new StringBuilder();
        if (d.getRewardList() == null)
            return "";
        for (Map.Entry<ResourceName, Map<Rarity, String>> e : d.getRewardList().entrySet())
            for (Map.Entry<Rarity, String> r : e.getValue().entrySet())
                sb.append(e.getKey().getDisplayName()).append(" ").append(ModuleService.rarityName(r.getKey()))
                        .append(" ").append(DifficultyService.scaleRange(r.getValue(), diff.getRewardRate()))
                        .append("\n");
        return sb.toString();
    }

    private void showDetail(int id) {
        selectedId = id;
        StageData d = STAGE_M.getStageConfig().get("stage" + id);
        if (difficultyPanel != null)
            difficultyPanel.show(id); // 選ばれた難易度で renderDetail が呼ばれる
        else
            renderDetail(Difficulty.NORMAL);
        UIBuilder.openPanel(detailPanel, d.getTitle(), true, null);
    }

    /**
     * 詳細の文章(難易度の倍率を反映した報酬を見せる)
     */
    private void renderDetail(Difficulty diff) {
        int id = selectedId;
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
                Rarity shown = Rarity.values()[Math.min(Rarity.values().length - 1,
                        Rarity.valueOf(r.getRarity()).ordinal() + diff.getRarityBump())];
                sb.append(kind).append("  ").append(ModuleService.rarityName(shown))
                        .append("  ").append(Math.round(Math.min(1.0, r.getChance() * diff.getModuleChanceRate()) * 100))
                        .append("%");
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
        sb.append(chipLine(d, diff)).append("\n");
        sb.append("最高スコア ").append(DifficultyService.highScore(id, diff));
        detailText.setText(sb.toString());
        detailText.setCaretPosition(0);
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
        g.drawString(categoryTitle, MAIN.getWidth() / 2 - px(80), px(40));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g.setColor(new Color(190, 220, 255));
        g.drawString("特別なステージ(通常ステージの進行には影響しない)", px(20), px(66));
    }
}
