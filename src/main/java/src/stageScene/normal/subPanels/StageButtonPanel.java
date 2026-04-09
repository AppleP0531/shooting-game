package src.stageScene.normal.subPanels;

import src.ShootingGameMain;
import src.data.DataManager;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.UIBuilder;
import src.gameScene.stage.StageManager;
import src.stageScene.normal.NormalStageManager;
import src.stageScene.normal.NormalStageManager.StageButtonData;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import lombok.Getter;

/**
 * ステージボタン描画クラス
 */
public class StageButtonPanel extends JPanel {
    private static final String component = "[StageButtonPanel]";
    @Getter
    private final StageDetailPanel stageDetailPanel;

    private final ShootingGameMain MAIN;
    private final NormalStageManager NSM;
    private final StageManager STAGE_M;

    private BufferedImage lockImage; // ロックアイコン
    private Vector2 panelSize; // パネルサイズ
    private Vector2 stageButtonOrigin; // 基準点の位置
    private double scrollOffset = 0; // スクロール量

    private Map<Integer, JButton> buttonMap;

    public StageButtonPanel() {
        MAIN = ShootingGameMain.getInstance();
        NSM = NormalStageManager.getInstance();
        STAGE_M = StageManager.getInstance();
        stageDetailPanel = new StageDetailPanel();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        addMouseWheelListener(e -> {
            // スクロール制限
            double maxScrollOffset = NSM.getStageButtonConfigMap().get(NSM.getStageButtonConfigMap().size())
                    .getPos().getY() * ShootingGameMain.getScreenSizeAdj() - panelSize.getY()
                    + buttonMap.get(1).getHeight() * 2;
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
        drawButton();
        this.add(stageDetailPanel);
        drawPanel();

        if (getParent() != null) // 最前面に持ってくる
            getParent().setComponentZOrder(this, 0);
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        setup();
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
        // パネル設定
        panelSize = new Vector2(MAIN.getWidth(), MAIN.getHeight() * 4 / 5);
        // パネルサイズが正しく取得できていなかった場合処理をスキップ
        if (panelSize.getX() == 0)
            return;

        setLayout(null);
        setOpaque(false);
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());
        setVisible(true);

        // ステージボタンの基準点の設定(画面中央を基準、下から50px)
        double bottom = this.getHeight() - 50 * ShootingGameMain.getScreenSizeAdj();
        stageButtonOrigin = new Vector2(this.getWidth() / 2, Math.max(bottom + scrollOffset, bottom));
        // ステージボタンの追加
        for (StageButtonData data : NSM.getStageButtonConfigMap().values()) {
            int stageId = data.getId();
            Vector2 buttonPos = data.getPos();

            JButton button = buttonMap.get(stageId);

            button.setVisible(false);

            // 位置
            int x = (int) stageButtonOrigin.getX() + (int) (buttonPos.getX() * ShootingGameMain.getScreenSizeAdj())
                    - (int) button.getWidth() / 2;
            int y = (int) stageButtonOrigin.getY() - (int) (buttonPos.getY() * ShootingGameMain.getScreenSizeAdj())
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
    private void drawButton() {
        this.removeAll();

        buttonMap = new HashMap<>();

        for (Map.Entry<Integer, BufferedImage> entry : NSM.getStageButtonImageCache().entrySet()) {
            int stageId = entry.getKey();
            BufferedImage image = entry.getValue();

            // ステージボタン
            Vector2 stageButtonSize = new Vector2(
                    50 * ShootingGameMain.getScreenSizeAdj(),
                    50 * ShootingGameMain.getScreenSizeAdj());
            JButton button = UIBuilder.createImageButton(
                    component, image, stageButtonSize, String.valueOf(stageId));
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
                        g2.drawImage(image, 0, 0, null);
                        g2.drawImage(lockImage,
                                (button.getWidth() - lockImage.getWidth()) / 2,
                                (button.getHeight() - lockImage.getHeight()) / 2,
                                null);
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
    }
}