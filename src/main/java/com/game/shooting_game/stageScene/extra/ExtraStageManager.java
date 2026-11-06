package com.game.shooting_game.stageScene.extra;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.farm.MaterialFarmService;
import com.game.shooting_game.domain.power.PowerService;
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
    private static final int EXTRA_BASE_ID = 1100;

    private final ShootingGameMain MAIN;
    private final SceneManager SM;
    private final StageManager STAGE_M;

    private final List<JButton> stageBtns = new ArrayList<>();
    private JButton goBackBtn;
    private JPanel detailPanel;
    private ExtraDetailView detailView;
    private JButton startBtn;
    private DifficultyPanel difficultyPanel; // ボス分類だけ、難易度を選べる
    private int selectedId = -1;

    // ボス分類は、ボスのプレビューを中央に出し、左右の矢印で挑戦するステージを選ぶ(一覧ではなく1件ずつ)
    private final boolean carousel;
    private int carouselIndex = 0;
    private List<Integer> carouselIds = new ArrayList<>();
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton challengeBtn;
    private final Map<String, BufferedImage> bossImages = new HashMap<>(); // ボスの絵(0コマ目)のキャッシュ。なければnull
    private final String category;
    private final String categoryTitle;

    /** 分類ごとに1つずつ作る(キー, 表示名) */
    public static final String[][] CATEGORIES = { { "MODULE", "モジュール" }, { "BOSS", "ボス" },
            { "MATERIAL", "素材" }, { "SPECIAL", "特別" } };

    public ExtraStageManager(String category, String categoryTitle) {
        this.category = category;
        this.categoryTitle = categoryTitle;
        this.carousel = true; // どの分類も、プレビューのカードで1件ずつ選ぶ
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

        goBackBtn = UIBuilder.createBackBtn(component, () -> SM.changeToPrevScene());
        this.add(goBackBtn);

        if (carousel) {
            int arrow = px(44);
            int cardTop = px(84), cardBottom = MAIN.getHeight() - px(112);
            int midY = (cardTop + cardBottom) / 2 - arrow / 2;
            prevBtn = UIBuilder.createImageBtn(component, "/static/img/UI/backArrow.png", new Vector2(arrow, arrow), "◀");
            prevBtn.setBounds(px(8), midY, arrow, arrow);
            prevBtn.addActionListener(e -> moveCarousel(-1));
            this.add(prevBtn);
            nextBtn = UIBuilder.createImageBtn(component, "/static/img/UI/forwardArrow.png", new Vector2(arrow, arrow), "▶");
            nextBtn.setBounds(MAIN.getWidth() - px(8) - arrow, midY, arrow, arrow);
            nextBtn.addActionListener(e -> moveCarousel(1));
            this.add(nextBtn);
            challengeBtn = UIBuilder.createImageBtn(component, "/static/img/UI/lobbyScene/startStage.png",
                    new Vector2(px(130), px(46)), "挑戦");
            challengeBtn.setBounds(MAIN.getWidth() / 2 - px(65), MAIN.getHeight() - px(104), px(130), px(46));
            challengeBtn.addActionListener(e -> {
                if (!carouselIds.isEmpty())
                    showDetail(carouselIds.get(carouselIndex));
            });
            this.add(challengeBtn);
        }

        // ステージの詳細(押したステージの説明と、報酬の内容)
        detailPanel = new JPanel();
        detailPanel.setLayout(null);
        detailPanel.setVisible(false);
        int dw = px(350);
        boolean pickDifficulty = "BOSS".equals(category);
        int diffH = pickDifficulty ? DifficultyPanel.height() + px(6) : 0;
        int dh = px(330) + diffH;
        detailPanel.setBounds(MAIN.getWidth() / 2 - dw / 2, MAIN.getHeight() / 2 - dh / 2, dw, dh);
        detailView = new ExtraDetailView(dw - px(16) - px(8));
        JScrollPane detailScroll = new JScrollPane(detailView);
        detailScroll.setBorder(null);
        detailScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(px(16));
        detailScroll.setOpaque(false);
        detailScroll.getViewport().setOpaque(false);
        detailScroll.setBounds(px(8), px(6), dw - px(16), dh - px(66) - diffH);
        detailPanel.add(detailScroll);
        if (pickDifficulty) {
            difficultyPanel = new DifficultyPanel(dw - px(16), d -> {
                // 難易度を選ぶまでは、STARTを押せない(未選択のときは、ノーマルの内容を見せるだけ)
                Difficulty shown = d == null ? Difficulty.NORMAL : d;
                STAGE_M.getStage().setDifficulty(shown);
                renderDetail(shown);
                startBtn.setEnabled(d != null);
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
            Difficulty diff = difficultyPanel == null || difficultyPanel.getSelected() == null ? Difficulty.NORMAL
                    : difficultyPanel.getSelected();
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
                    && category.equals(STAGE_M.getStageConfig().get(key).getCategory())
                    && (!MaterialFarmService.isFarmStage(id) || MaterialFarmService.obtained(MaterialFarmService.resourceOf(id))))
                ids.add(id); // 素材周回ステージは、手に入れたことのある素材だけ
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
        if (carousel) {
            for (JButton b : stageBtns)
                this.remove(b);
            stageBtns.clear();
            carouselIds = extraIds();
            carouselIndex = Math.max(0, Math.min(carouselIndex, carouselIds.size() - 1));
            refreshCarousel();
            return;
        }
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

    /** 矢印で挑戦対象を切り替える(端でつながる) */
    private void moveCarousel(int delta) {
        if (carouselIds.isEmpty())
            return;
        carouselIndex = Math.floorMod(carouselIndex + delta, carouselIds.size());
        refreshCarousel();
    }

    private void refreshCarousel() {
        boolean has = !carouselIds.isEmpty();
        prevBtn.setEnabled(carouselIds.size() > 1);
        nextBtn.setEnabled(carouselIds.size() > 1);
        challengeBtn.setEnabled(has && unlocked(STAGE_M.getStageConfig().get("stage" + carouselIds.get(carouselIndex))));
        repaint();
    }

    /** そのステージに出るボスのID(出てくる順。重複なし) */
    private List<String> bossIdsOf(StageData d) {
        List<String> ids = new ArrayList<>();
        if (d.getEnemyList() == null)
            return ids;
        for (var waves : d.getEnemyList().values())
            for (var e : waves)
                if (e.getEnemyId() != null && e.getEnemyId().startsWith("Boss") && !ids.contains(e.getEnemyId()))
                    ids.add(e.getEnemyId());
        return ids;
    }

    /** ボスの絵(0コマ目)。なければnull */
    private BufferedImage bossImage(String id) {
        if (bossImages.containsKey(id))
            return bossImages.get(id);
        BufferedImage img = null;
        try {
            BufferedImage sheet = UIBuilder.loadImage("/static/img/ship/boss/" + id + "_f0.png");
            img = UIBuilder.cutSpriteSheet(sheet, new Vector2(4, 2), 6)[0];
        } catch (java.io.IOException | RuntimeException e) {
            // 画像がなければ文字だけの表示になる
        }
        bossImages.put(id, img);
        return img;
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
        else {
            renderDetail(Difficulty.NORMAL);
            startBtn.setEnabled(true); // 難易度を選ばないステージは、いつでも押せる
        }
        UIBuilder.openPanel(detailPanel, d.getTitle(), true, null);
    }

    /**
     * 詳細の文章(難易度の倍率を反映した報酬を見せる)
     */
    private void renderDetail(Difficulty diff) {
        int id = selectedId;
        StageData d = STAGE_M.getStageConfig().get("stage" + id);
        int icon = detailView.iconSize();

        // 条件(デイリーは説明文に含まれている。敵のHP・被ダメージの倍率は難易度の欄に出す)
        List<ExtraDetailView.Condition> conds = new ArrayList<>();
        var spec = d.getModifiers();
        if (id != DailyService.STAGE_ID && spec != null) {
            if (spec.isNoHeal())
                conds.add(new ExtraDetailView.Condition("回復アイテムが効かない", "Heal", true));
            if (spec.isNoShield())
                conds.add(new ExtraDetailView.Condition("シールドが使えない", "Shield", true));
            if (spec.getPlayerHpRate() != 1.0)
                conds.add(new ExtraDetailView.Condition(String.format("自機の最大HP ×%.1f", spec.getPlayerHpRate()), null, false));
            if (spec.getCoinRate() != 1.0)
                conds.add(new ExtraDetailView.Condition(String.format("拾うコイン ×%.1f", spec.getCoinRate()), null, false));
        }

        // ボスのHP(難易度・ステージ条件・ステージ番号の倍率をかけたあと)
        int stageIndex = id < 1000 ? id : Math.max(1, d.getUnlock());
        double hpRate = StageModifiers.previewEnemyHpRate(spec, diff, "BOSS".equals(d.getCategory()), stageIndex);
        List<ExtraDetailView.BossInfo> bossInfos = new ArrayList<>();
        for (String bossId : bossIdsOf(d)) {
            var def = com.game.shooting_game.gameScene.ship.BossLibrary.forFight(bossId, id);
            double gaugeHp = def.getGaugeHp() * hpRate;
            int gauges = def.getGauges().size();
            bossInfos.add(new ExtraDetailView.BossInfo(bossImage(bossId), bossId, gauges,
                    String.format("%,.0f", gaugeHp), String.format("%,.0f", gaugeHp * gauges)));
        }

        // 報酬(モジュールのあとに素材。難易度の倍率を反映)
        List<ExtraDetailView.Reward> rewards = new ArrayList<>();
        if (d.getModuleRewards() != null) {
            for (ModuleReward r : d.getModuleRewards()) {
                ModuleKind kind = r.getKind() == null ? null : ModuleKind.valueOf(r.getKind());
                Rarity shown = Rarity.values()[Math.min(Rarity.values().length - 1,
                        Rarity.valueOf(r.getRarity()).ordinal() + diff.getRarityBump())];
                String badge = null;
                if ("FIRST".equals(r.getOnce()))
                    badge = "初回";
                else if ("DAILY".equals(r.getOnce()))
                    badge = DataManager.getUserData().getProgress().getHighScores()
                            .containsKey("daily:" + java.time.LocalDate.now() + ":stage" + id) ? "受取済" : "毎日";
                rewards.add(ExtraDetailView.Reward.module(kind, shown,
                        (int) Math.round(Math.min(1.0, r.getChance() * diff.getModuleChanceRate()) * 100), badge, icon));
            }
        }
        if (d.getRewardList() != null)
            for (Map.Entry<ResourceName, Map<Rarity, String>> e : d.getRewardList().entrySet())
                for (Map.Entry<Rarity, String> r : e.getValue().entrySet())
                    rewards.add(ExtraDetailView.Reward.resource(new ResourceKinds(e.getKey(), r.getKey()),
                            DifficultyService.scaleRange(r.getValue(), diff.getRewardRate()), icon));

        // 推定必要戦力値(解放に要るステージ番号を目安にする)と、いまの機体の戦力値
        String power = String.format("\n\n推定必要戦力値 %,d(あなた %,d)", Math.round(PowerService.required(stageIndex, diff)),
                Math.round(PowerService.currentPower()));
        detailView.setData((d.getDescription() == null ? "" : d.getDescription()) + power, conds, bossInfos, rewards,
                DifficultyService.highScore(id, diff));
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
        int cx0 = MAIN.getWidth() / 2; // 見出しは画面の中央に寄せる
        g.drawString(categoryTitle, cx0 - g.getFontMetrics().stringWidth(categoryTitle) / 2, px(40));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g.setColor(new Color(190, 220, 255));
        String sub = "特別なステージ(通常ステージの進行には影響しない)";
        g.drawString(sub, cx0 - g.getFontMetrics().stringWidth(sub) / 2, px(66));

        if (carousel)
            drawCarousel((java.awt.Graphics2D) g);
    }

    /**
     * ボスのプレビューのカード(ステージ名、ボスの絵、ゲージ数とHP、最高スコアまたは解放条件)
     */
    private void drawCarousel(java.awt.Graphics2D g) {
        if (carouselIds.isEmpty())
            return;
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int id = carouselIds.get(carouselIndex);
        StageData d = STAGE_M.getStageConfig().get("stage" + id);
        boolean open = unlocked(d);
        List<String> bosses = bossIdsOf(d);

        int cx = MAIN.getWidth() / 2;
        // 画面の高さいっぱいに使う(上は題字、下は挑戦ボタンと戻る矢印の分を空ける)
        int cardX = px(60), cardW = MAIN.getWidth() - px(120), cardY = px(84), cardH = MAIN.getHeight() - px(196);
        g.setColor(new Color(10, 20, 40, 200));
        g.fillRoundRect(cardX, cardY, cardW, cardH, px(16), px(16));
        g.setColor(new Color(70, 215, 255));
        g.setStroke(new java.awt.BasicStroke((float) px(2)));
        g.drawRoundRect(cardX, cardY, cardW, cardH, px(16), px(16));

        // ステージ名と何番目か
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(15))));
        drawCentered(g, d.getTitle(), cx, cardY + px(24));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g.setColor(new Color(150, 200, 230));
        drawCentered(g, (carouselIndex + 1) + " / " + carouselIds.size(), cx, cardY + px(42));

        // ボスの絵(複数のボスが出るステージは、小さく横に並べる)
        int imgAreaTop = cardY + px(52), imgAreaH = (int) (cardH * 0.46);
        int n = Math.max(1, bosses.size());
        int cell = Math.min(imgAreaH, (cardW - px(16)) / n);
        int startX = cx - cell * n / 2;
        if (bosses.isEmpty())
            drawRewardTiles(g, d, open, cx, imgAreaTop, imgAreaH, cardW);
        for (int i = 0; i < bosses.size(); i++) {
            BufferedImage img = bossImage(bosses.get(i));
            int x = startX + cell * i;
            int y = imgAreaTop + (imgAreaH - cell) / 2;
            if (img != null) {
                double s = Math.min((double) (cell - px(6)) / img.getWidth(), (double) (cell - px(6)) / img.getHeight());
                int w = (int) (img.getWidth() * s), h = (int) (img.getHeight() * s);
                if (!open)
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
                g.drawImage(img, x + (cell - w) / 2, y + (cell - h) / 2, w, h, null);
                g.setComposite(java.awt.AlphaComposite.SrcOver);
            } else {
                g.setColor(new Color(255, 255, 255, 40));
                g.fillRoundRect(x + px(3), y + px(3), cell - px(6), cell - px(6), px(10), px(10));
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(13))));
                drawCentered(g, bosses.get(i), x + cell / 2, y + cell / 2);
            }
        }

        // ボスの情報と最高スコア(絵の下)
        int y = imgAreaTop + imgAreaH + px(26);
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(13))));
        g.setColor(new Color(200, 225, 255));
        if (bosses.size() == 1) {
            var def = com.game.shooting_game.gameScene.ship.BossLibrary.get(bosses.get(0));
            drawCentered(g, "ゲージ数 " + def.getGauges().size() + "　ゲージHP " + (long) def.getGaugeHp(), cx, y);
        } else if (bosses.size() > 1) {
            drawCentered(g, "ボス " + bosses.size() + " 体", cx, y);
        } else {
            // ボスのいないステージは、説明の1行目
            String first = d.getDescription() == null ? "" : d.getDescription().split("\n")[0];
            drawCentered(g, first, cx, y);
        }
        y += px(28);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(15))));
        if (open) {
            g.setColor(new Color(255, 214, 80));
            drawCentered(g, "最高スコア " + DataManager.getUserData().getProgress().getHighScores()
                    .getOrDefault("stage" + id, 0), cx, y);
        } else {
            g.setColor(new Color(255, 120, 120));
            drawCentered(g, "ステージ" + d.getUnlock() + "クリアで解放", cx, y);
        }

        // 条件と報酬のまとめ(カードの残りの高さに収まる行だけ)
        y += px(30);
        g.setColor(new Color(70, 215, 255, 120));
        g.drawLine(cardX + px(16), y - px(14), cardX + cardW - px(16), y - px(14));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
        g.setColor(new Color(150, 200, 230));
        drawCentered(g, "クリア報酬", cx, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
        g.setColor(Color.WHITE);
        int bottom = cardY + cardH - px(10);
        for (String line : rewardLines(d)) {
            y += px(20);
            if (y > bottom)
                break;
            drawCentered(g, line, cx, y);
        }
    }

    /** カードに出す報酬の短い行(モジュールと素材) */
    private List<String> rewardLines(StageData d) {
        List<String> lines = new ArrayList<>();
        if (d.getModuleRewards() != null)
            for (ModuleReward r : d.getModuleRewards()) {
                String kind = r.getKind() == null ? "モジュール" : ModuleKind.valueOf(r.getKind()).getTitle();
                lines.add(kind + "  " + ModuleService.rarityName(Rarity.valueOf(r.getRarity())) + "  "
                        + Math.round(Math.min(1.0, r.getChance()) * 100) + "%");
            }
        for (String s : chipLine(d, Difficulty.NORMAL).split("\n"))
            if (!s.isBlank())
                lines.add(s);
        return lines;
    }

    /** ボスのいないステージの絵: 報酬のモジュール・素材のアイコンを並べる(未解放は暗く) */
    private void drawRewardTiles(java.awt.Graphics2D g, StageData d, boolean open, int cx, int top, int areaH, int cardW) {
        List<Object[]> tiles = new ArrayList<>(); // {絵(BufferedImage か null), 枠の色, 文字}
        if (d.getModuleRewards() != null)
            for (ModuleReward r : d.getModuleRewards()) {
                BufferedImage img = r.getKind() == null ? null
                        : com.game.shooting_game.ui.ModuleIcons.get(ModuleKind.valueOf(r.getKind()), px(64));
                tiles.add(new Object[] { img, com.game.shooting_game.ui.CostPanel.rarityColor(Rarity.valueOf(r.getRarity())),
                        r.getKind() == null ? "?" : ModuleKind.valueOf(r.getKind()).getShortName() });
            }
        if (d.getRewardList() != null) {
            var images = com.game.shooting_game.ResourceManager.getInstance().getResourceImage();
            for (Map.Entry<ResourceName, Map<Rarity, String>> e : d.getRewardList().entrySet())
                for (Rarity rar : e.getValue().keySet()) {
                    BufferedImage[] frames = images == null || images.get(e.getKey()) == null ? null
                            : images.get(e.getKey()).get(rar);
                    tiles.add(new Object[] { frames != null && frames.length > 0 ? frames[0] : null,
                            com.game.shooting_game.ui.CostPanel.rarityColor(rar), e.getKey().getDisplayName() });
                }
        }
        if (tiles.isEmpty()) { // 報酬の絵がないステージは、ステージ名を大きく出す
            g.setColor(open ? Color.WHITE : new Color(150, 150, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, px(22))));
            drawCentered(g, STAGE_M.getStageConfig().get("stage" + carouselIds.get(carouselIndex)).getTitle(), cx,
                    top + areaH / 2);
            return;
        }
        int n = Math.min(tiles.size(), 4);
        int cell = Math.min(areaH, (cardW - px(16)) / n);
        int startX = cx - cell * n / 2;
        for (int i = 0; i < n; i++) {
            Object[] t = tiles.get(i);
            int x = startX + cell * i + px(4), y = top + (areaH - cell) / 2 + px(4), s = cell - px(8);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRoundRect(x, y, s, s, px(12), px(12));
            if (!open)
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
            if (t[0] != null) {
                BufferedImage img = (BufferedImage) t[0];
                double sc = Math.min((double) (s - px(10)) / img.getWidth(), (double) (s - px(10)) / img.getHeight());
                int w = (int) (img.getWidth() * sc), h = (int) (img.getHeight() * sc);
                g.drawImage(img, x + (s - w) / 2, y + (s - h) / 2, w, h, null);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, px(20))));
                drawCentered(g, (String) t[2], x + s / 2, y + s / 2 + px(7));
            }
            g.setColor((Color) t[1]);
            g.setStroke(new java.awt.BasicStroke((float) px(2)));
            g.drawRoundRect(x, y, s, s, px(12), px(12));
            g.setComposite(java.awt.AlphaComposite.SrcOver);
        }
    }

    private void drawCentered(java.awt.Graphics2D g, String text, int cx, int baselineY) {
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baselineY);
    }
}
