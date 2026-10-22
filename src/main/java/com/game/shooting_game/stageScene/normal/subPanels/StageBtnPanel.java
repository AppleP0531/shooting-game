package com.game.shooting_game.stageScene.normal.subPanels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.stageScene.normal.Chapters;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager.StageBtnData;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.ui.StageLockIcon;
import com.game.shooting_game.ui.StageNodeIcon;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class StageBtnPanel extends JPanel {
    private static final String component = "[StageBtnPanel]";
    @Getter
    private final StageDetailPanel stageDetailPanel;

    private final ShootingGameMain MAIN;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

    private static final double NODE_H = 64; // ステージの台の絵の高さ(基準サイズ)
    private static final double NODE_W = 56;

    private BufferedImage lockImage; // ロックアイコン
    private Vector2 panelSize; // パネルサイズ
    private Vector2 originPos; // 基準点の位置
    private double scrollOffset = 0; // スクロール量
    private double maxScrollOffset; // 最大スクロール量

    private Map<Integer, JButton> buttonMap;

    public StageBtnPanel() {
        MAIN = ShootingGameMain.getInstance();
        NSM = NormalStageManager.getInstance();
        STAGE_M = StageManager.getInstance();
        stageDetailPanel = new StageDetailPanel();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        // 上下の帯(上44・下56)の間いっぱいに使う
        double adj0 = ShootingGameMain.getScreenSizeAdj();
        panelSize = new Vector2(MAIN.getWidth(), MAIN.getHeight() - (44 + 56) * adj0);
        setLayout(null);
        setOpaque(false);
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                (int) (44 * adj0),
                (int) panelSize.getX(), (int) panelSize.getY());

        addMouseWheelListener(e -> {
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * 20, 0, maxScrollOffset);
            drawPanel();
        });

        loadImages();
        stageDetailPanel.initComponents();
    }

    /**
     * セットアップ
     */
    public void setup() {
        drawBtn();
        drawPanel();

        if (getParent() != null) // 最前面に持ってくる
            getParent().setComponentZOrder(this, 0);

        // スクロール制限
        // 一番高い所にある台まで(下へ戻る配置もあるので、最後の台ではなく、最大の高さで決める)
        double topY = NSM.getStageBtnConfigMap().values().stream().mapToDouble(d -> d.getPos().getY()).max().orElse(0);
        maxScrollOffset = topY * ShootingGameMain.getScreenSizeAdj() - panelSize.getY()
                + buttonMap.get(1).getHeight() * 2;
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        drawBtn();
        drawPanel();
    }

    /**
     * 画像読み込み
     */
    private void loadImages() {
        String imagePath = "/static/img/UI/lobbyScene/stageLock.png";
        try {
            lockImage = UIBuilder.loadImage(imagePath);
            double imageSize = 20 * ShootingGameMain.getScreenSizeAdj();
            lockImage = UIBuilder.resizeImage(lockImage, imageSize);
        } catch (IOException e) {
            switch (e.getMessage()) {
                // 画像ファイル未発見ログ
                case "FILE_NOT_FOUND" -> LogUtils.warn(component,
                        "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                // 画像読み込みエラーログ
                case "BROKEN_FILE" -> LogUtils.error(component,
                        "Failed to load image: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                // その他エラーログ
                default -> System.out.println(e);
            }
        }
    }

    /**
     * パネルの描画
     */
    private void drawPanel() {
        // パネルサイズが正しく取得できていなかった場合処理をスキップ
        if (panelSize.getX() == 0)
            return;

        setVisible(true);

        // ステージボタンの基準点の設定(画面中央を基準、下から50px)
        double bottom = this.getHeight() - 50 * ShootingGameMain.getScreenSizeAdj();
        originPos = new Vector2(this.getWidth() / 2, Math.max(bottom + scrollOffset, bottom));
        // ステージボタンの追加
        for (StageBtnData data : NSM.getStageBtnConfigMap().values()) {
            int stageId = data.getId();
            Vector2 buttonPos = data.getPos();

            JButton button = buttonMap.get(stageId);

            button.setVisible(false);

            // 位置
            int x = (int) originPos.getX() + (int) (buttonPos.getX() * ShootingGameMain.getScreenSizeAdj())
                    - (int) button.getWidth() / 2;
            int y = (int) originPos.getY() - (int) (buttonPos.getY() * ShootingGameMain.getScreenSizeAdj())
                    - (int) button.getHeight() / 2;

            // パネル内にあるかどうか
            boolean isInPanel = 0 < Math.abs(y + button.getHeight()) && Math.abs(y) < this.getHeight();
            // パネル内にある場合は表示
            if (isInPanel) {
                button.setBounds(
                        x, y,
                        (int) button.getWidth(), (int) button.getHeight());

                button.setVisible(true);
            }
        }
        repaint();
    }

    /**
     * ステージの台の中心(ステージ選択パネルの中の座標。ボタンの中心より、台の分だけ下)
     */
    private Vector2 buttonCenter(StageBtnData data) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        return new Vector2(
                originPos.getX() + data.getPos().getX() * adj,
                originPos.getY() - data.getPos().getY() * adj + (StageNodeIcon.PLATFORM_CENTER - 0.5) * NODE_H * adj);
    }

    /** 今のスクロール量(背景の星を、同じ向きにゆっくり流すのに使う) */
    public double getScrollOffset() {
        return scrollOffset;
    }

    /**
     * ステージの台を番号順に点線の経路でつなぐ(ボタンより奥に描く)。章の最後のステージには、章のラベルを出す。
     * 次のステージに行ける区間(そのステージまでクリア済み)は水色の光る点、まだ行けない区間は暗い小さな点にする。
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (originPos == null || NSM.getStageBtnConfigMap() == null || DataManager.getUserData() == null)
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();
        int cleared = DataManager.getUserData().getProgress().getClearedStage();
        Map<Integer, StageBtnData> configs = NSM.getStageBtnConfigMap();

        for (int id = 1; configs.containsKey(id) && configs.containsKey(id + 1); id++) {
            Vector2 a = buttonCenter(configs.get(id));
            Vector2 b = buttonCenter(configs.get(id + 1));
            // 画面の外の区間は描かない
            if (Math.max(a.getY(), b.getY()) < 0 || Math.min(a.getY(), b.getY()) > getHeight())
                continue;

            Line2D line = new Line2D.Double(a.getX(), a.getY(), b.getX(), b.getY());
            if (id <= cleared) {
                // 光のにじみ + 水色の点
                g2.setStroke(new BasicStroke((float) (8 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[] { 0.1f, (float) (9 * adj) }, 0f));
                g2.setColor(new Color(70, 215, 255, 60));
                g2.draw(line);
                g2.setStroke(new BasicStroke((float) (4 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[] { 0.1f, (float) (9 * adj) }, 0f));
                g2.setColor(new Color(130, 235, 255, 230));
                g2.draw(line);
            } else {
                g2.setStroke(new BasicStroke((float) (2.5 * adj), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[] { 0.1f, (float) (9 * adj) }, 0f));
                g2.setColor(new Color(110, 125, 160, 120));
                g2.draw(line);
            }
        }

        // 章のラベル(章の最後のステージの台の、画面の端の側。上に「第N章」、下に星座の名前の2行)
        java.awt.Font big = new java.awt.Font("SansSerif", java.awt.Font.BOLD, Math.max(10, (int) (14 * adj)));
        java.awt.Font small = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, Math.max(9, (int) (11 * adj)));
        for (StageBtnData data : configs.values()) {
            if (!Chapters.isChapterEnd(data.getId()))
                continue;
            Vector2 c = buttonCenter(data);
            if (c.getY() < -NODE_H * adj || c.getY() > getHeight() + NODE_H * adj)
                continue;
            int chapter = Chapters.chapterOf(data.getId());
            String top = "第" + chapter + "章", bottom = Chapters.name(chapter);
            g2.setFont(big);
            int tw = g2.getFontMetrics().stringWidth(top);
            int th = g2.getFontMetrics().getHeight();
            g2.setFont(small);
            int bw = g2.getFontMetrics().stringWidth(bottom);
            int w = Math.max(tw, bw);
            int gap = (int) ((NODE_W / 2 + 8) * adj);
            // 台が左側なら左に、右側なら右に置く(中央側の経路と重ならない)
            int x = c.getX() < getWidth() / 2.0 ? (int) c.getX() - gap - w : (int) c.getX() + gap;
            x = Math.clamp(x, (int) (4 * adj), getWidth() - w - (int) (4 * adj));
            int y = (int) c.getY() - th / 2;
            g2.setFont(big);
            g2.setColor(Color.WHITE);
            g2.drawString(top, x + (w - tw) / 2, y + g2.getFontMetrics().getAscent());
            g2.setFont(small);
            g2.setColor(new Color(130, 235, 255));
            g2.drawString(bottom, x + (w - bw) / 2, y + th + g2.getFontMetrics().getAscent() - (int) (2 * adj));
        }
        g2.dispose();
    }

    /**
     * ボタンの描画
     */
    private void drawBtn() {
        this.removeAll();

        buttonMap = new HashMap<>();

        // 現在のクリア状況の取得
        int clearedStage = DataManager.getUserData().getProgress().getClearedStage();

        for (Map.Entry<Integer, BufferedImage> entry : NSM.getStageBtnImageCache().entrySet()) {
            int stageId = entry.getKey();
            // 解放されていないステージはロックする
            boolean isLocked = stageId > (clearedStage + 1);

            JButton button = createStageButton(stageId, entry.getValue(), isLocked);
            this.add(button);
            button.setVisible(false); // 非表示
            buttonMap.put(stageId, button);
        }

        this.add(stageDetailPanel);
    }

    /**
     * ステージボタンの作成(アイコンと、押したときの動き。ロック中は押せないボタンにする)
     * 
     * @param stageId ステージID
     * @param image   ステージのアイコン画像(なければnull)
     * @param locked  ロック中かどうか
     */
    private JButton createStageButton(int stageId, BufferedImage image, boolean locked) {
        int w = (int) (NODE_W * ShootingGameMain.getScreenSizeAdj());
        int h = (int) (NODE_H * ShootingGameMain.getScreenSizeAdj());

        // アイコン(画像がなければコードで描いた番号つきの四角)。ロック中は鍵を重ねる
        int stars = 0;
        for (Difficulty d : locked ? new Difficulty[0] : Difficulty.values())
            stars = Math.max(stars, DifficultyService.stars(stageId, d));
        BufferedImage icon = image != null ? image
                : StageNodeIcon.create(w, h, String.valueOf(stageId),
                        StageNodeIcon.lookOf(locked, Chapters.isBoss(stageId), stars), stars);
        if (locked)
            icon = StageLockIcon.overlay(icon, lockIconImage(w), w, h);

        JButton button = UIBuilder.createImageBtn(component, icon, new Vector2(w, h), String.valueOf(stageId));
        button.setRolloverEnabled(false);
        button.setEnabled(!locked);
        if (locked) {
            // 押せないボタンを、灰色に塗り替えず、同じアイコンのまま出す
            button.setDisabledIcon(button.getIcon());
        } else {
            button.addActionListener(e -> {
                STAGE_M.getStage().setStageId(stageId);
                stageDetailPanel.showStageDaetail(stageId);
            });
        }
        return button;
    }

    /**
     * ロックの鍵の画像(鍵の画像ファイルがなければ、コードで描いた南京錠)
     * 
     * @param buttonSize ボタンの一辺(px)
     */
    private BufferedImage lockIconImage(int buttonSize) {
        return lockImage != null ? lockImage : StageLockIcon.padlock((int) (buttonSize * 0.55));
    }
}