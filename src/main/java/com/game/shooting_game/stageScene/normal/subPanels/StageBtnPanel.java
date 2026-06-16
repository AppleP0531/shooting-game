package com.game.shooting_game.stageScene.normal.subPanels;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.stage.StageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager;
import com.game.shooting_game.stageScene.normal.NormalStageManager.StageBtnData;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.Anchor.H;
import com.game.shooting_game.utils.Anchor.V;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class StageBtnPanel extends JPanel {
    private static final String component = "[StageBtnPanel]";
    @Getter
    private final StageDetailPanel stageDetailPanel;

    private final ShootingGameMain MAIN;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

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
        panelSize = new Vector2(MAIN.getWidth(), MAIN.getHeight() * 4 / 5);
        setLayout(null);
        setOpaque(false);
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
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
        maxScrollOffset = NSM.getStageBtnConfigMap().get(NSM.getStageBtnConfigMap().size())
                .getPos().getY() * ShootingGameMain.getScreenSizeAdj() - panelSize.getY()
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
                case "FILE_NOT_FOUND" -> LogUtils.systemLog(
                        "[WARN]",
                        component,
                        "File not found: " + ANSI.YELLOW + imagePath + ANSI.RESET + ".");

                // 画像読み込みエラーログ
                case "BROKEN_FILE" -> LogUtils.systemLog(
                        "[ERROR]",
                        component,
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
    }

    /**
     * ボタンの描画
     */
    private void drawBtn() {
        this.removeAll();

        buttonMap = new HashMap<>();

        for (Map.Entry<Integer, BufferedImage> entry : NSM.getStageBtnImageCache().entrySet()) {
            int stageId = entry.getKey();
            BufferedImage image = entry.getValue();

            // ステージボタン
            Vector2 stageBtnSize = new Vector2(
                    50 * ShootingGameMain.getScreenSizeAdj(),
                    50 * ShootingGameMain.getScreenSizeAdj());
            JButton button = UIBuilder.createImageBtn(
                    component, image, stageBtnSize, String.valueOf(stageId));
            button.addActionListener(e -> {
                STAGE_M.getStage().setStageId(stageId);
                stageDetailPanel.showStageDaetail(stageId);
            });
            button.setRolloverEnabled(false);

            this.add(button);
            button.setVisible(false); // 非表示

            // 現在のクリア状況の取得
            int clearedStage = DataManager.getUserData().getProgress().getClearedStage();
            // 解放されているかどうか
            boolean isLocked = stageId > (clearedStage + 1);
            // 解放されていないステージの場合ロックする
            if (isLocked) {
                button.setEnabled(false);
                // ロックアイコンが存在する場合
                if (lockImage != null) {
                    // ボタンアイコンが存在する場合はロックアイコンと合成する
                    if (button.getIcon() != null) {
                        BufferedImage combined = new BufferedImage(button.getWidth(), button.getHeight(),
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = combined.createGraphics();
                        UIBuilder.drawImage(g2, new Anchor(H.LEFT, V.TOP), new Vector2(0, 0), image);
                        UIBuilder.drawImage(g2, Anchor.CENTER, new Vector2(button.getWidth(), button.getHeight()),
                                lockImage);
                        g2.dispose();
                        button.setIcon(new ImageIcon(combined));
                        button.setDisabledIcon(new ImageIcon(combined));

                        button.setBorderPainted(false);
                        button.setContentAreaFilled(false);
                        button.setText("");
                    } else {
                        button.setText("");
                        button.setIcon(new ImageIcon(lockImage));
                        button.setDisabledIcon(new ImageIcon(lockImage));
                    }
                } else
                    button.setText("Locked");
            } else
                button.setEnabled(true);

            buttonMap.put(stageId, button);
        }

        this.add(stageDetailPanel);
    }
}