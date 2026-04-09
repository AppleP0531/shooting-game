package src.upgradeScene.subPanels;

import src.ShootingGameMain;
import src.data.DataManager;
import src.upgradeScene.UpgradeManager;
import src.upgradeScene.UpgradeManager.SkillTreeButtonData;
import src.utils.UIBuilder;
import src.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import lombok.Getter;

/**
 * スキルツリーボタン描画クラス
 */
public class SkillTreeButtonPanel extends JPanel {
    private static final String component = "[SkillTreeButtonPanel]";
    private final ShootingGameMain MAIN;
    private final UpgradeManager UM;
    @Getter
    private final SkillDetailPanel skillDetailPanel;

    private MouseAdapter mouseHandler;
    private Vector2 dragStartPos; // ドラッグ開始マウス位置
    private Vector2 dragStartOriginPos; // ドラッグ開始時の基準点の位置
    private Vector2 dragMousePos; // ドラッグ中のマウス位置
    private double scale = 1; // ズーム量
    private Vector2 panelSize; // パネルサイズ
    private Vector2 skillTreeButtonOrigin; // 基準点の位置

    private Map<String, JButton> buttonMap;

    public SkillTreeButtonPanel() {
        MAIN = ShootingGameMain.getInstance();
        UM = UpgradeManager.getInstance();
        skillDetailPanel = new SkillDetailPanel();

        mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPos = new Vector2(e.getX(), e.getY());
                dragStartOriginPos = new Vector2(skillTreeButtonOrigin.getX(), skillTreeButtonOrigin.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPos = null;
                dragMousePos = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragMousePos = new Vector2(e.getX(), e.getY());
                skillTreeButtonOrigin.setX(dragStartOriginPos.getX() + dragMousePos.getX() - dragStartPos.getX());
                skillTreeButtonOrigin.setY(dragStartOriginPos.getY() + dragMousePos.getY() - dragStartPos.getY());
                drawPanel();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomAmount = 0.1; // ズーム変化量
                if (e.getPreciseWheelRotation() < 0) {
                    scale = Math.min(scale + zoomAmount, 2);
                } else {
                    scale = Math.max(scale - zoomAmount, 0.5);
                }
                setup();
            }
        };
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        panelSize = new Vector2(MAIN.getWidth(), MAIN.getHeight() * 5 / 6);
        skillDetailPanel.initComponents();
    }

    /**
     * セットアップ
     */
    public void setup() {
        skillTreeButtonOrigin = new Vector2(panelSize.getX() / 2, panelSize.getY() / 2);

        drawButton();
        this.add(skillDetailPanel);
        skillDetailPanel.setVisible(false);
        drawPanel();

        // 最前面に持ってくる
        if (getParent() != null)
            getParent().setComponentZOrder(this, 0);
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Vector2 panelPos = new Vector2(this.getX(), this.getY());

        g2.setClip((int) panelPos.getX(), (int) panelPos.getY(), getWidth(), getHeight());
        g2.setColor(Color.CYAN);
        g2.setStroke(new BasicStroke(2));

        for (SkillTreeButtonData data : UM.getSkillTreeButtonConfigMap()) {
            if (data.getName().equals("Origin"))
                continue;
            JButton parent = buttonMap.get(data.getRootNode());
            JButton child = buttonMap.get(data.getName());

            // 親ボタンの中心座標
            Vector2 parentPos = new Vector2(
                    panelPos.getX() + parent.getX() + parent.getWidth() / 2,
                    panelPos.getY() + parent.getY() + parent.getHeight() / 2);
            // 子ボタンの中心座標
            Vector2 childPos = new Vector2(
                    panelPos.getX() + child.getX() + child.getWidth() / 2,
                    panelPos.getY() + child.getY() + child.getHeight() / 2);

            String nowShip = DataManager.getUserData().getSelectedShip();
            int upgradeLevel = DataManager.getUserData().getShips().get(nowShip)
                    .getUpgrades().getOrDefault(data.getRootNode(), 0);

            if (data.getName().equals("Origin") || upgradeLevel > 0) {
                // パネルのY座標の範囲
                int panelTop = (int) panelPos.getY();
                int panelBottom = panelTop + getHeight();

                // 親と子がパネルの表示領域に入っているかの判定
                boolean parentInView = (parentPos.getY() >= panelTop && parentPos.getY() <= panelBottom);
                boolean childInView = (childPos.getY() >= panelTop && childPos.getY() <= panelBottom);

                // どちらかが入っている場合に線で結ぶ
                if (parentInView || childInView)
                    g2.drawLine((int) parentPos.getX(), (int) parentPos.getY(),
                            (int) childPos.getX(), (int) childPos.getY());
            }
        }

        g2.setClip(null);
    }

    /**
     * リソースの再読み込み
     */
    public void reload() {
        setup();
    }

    /**
     * パネルの描画
     */
    private void drawPanel() {
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

        // スキルツリーボタンの基準点の設定(画面中央)
        if (skillTreeButtonOrigin == null)
            skillTreeButtonOrigin = new Vector2(this.getWidth() / 2, this.getHeight() / 2);

        // スキルツリーボタンの追加
        for (SkillTreeButtonData data : UM.getSkillTreeButtonConfigMap()) {
            JButton button = buttonMap.get(data.getName());

            button.setVisible(false);

            // ボタン同士の距離
            double radius = data.getDist() * scale * ShootingGameMain.getScreenSizeAdj();
            Vector2 buttonPos;
            // 各ボタンの位置設定
            if (data.getName().equals("Origin")) {
                buttonPos = new Vector2(
                        skillTreeButtonOrigin.getX() - button.getWidth() / 2,
                        skillTreeButtonOrigin.getY() - button.getHeight() / 2);
            } else {
                // 子要素の円形配置用(5個)
                double ang = Math.PI * 2 / 5;
                JButton parentButton = buttonMap.get(data.getRootNode());
                buttonPos = new Vector2(
                        parentButton.getX() + Math.cos(ang * data.getPos() - Math.PI / 2) * radius,
                        parentButton.getY() + Math.sin(ang * data.getPos() - Math.PI / 2) * radius);
            }
            // 位置
            int x = (int) buttonPos.getX();
            int y = (int) buttonPos.getY();
            button.setBounds(
                    x, y,
                    (int) button.getWidth(), (int) button.getHeight());

            boolean isSelfInPanel = (0 < y + button.getHeight() && y < this.getHeight());
            boolean isParentInPanel = false;
            JButton parentButton = buttonMap.get(data.getRootNode());
            if (parentButton != null) // 親が存在する場合
                isParentInPanel = (0 < y + parentButton.getHeight() && y < this.getHeight());
            // どちらかがパネル内にある場合
            if (isSelfInPanel || isParentInPanel) {
                // 基準ボタンではない時
                if (!data.getName().equals("Origin")) {
                    String nowShip = DataManager.getUserData().getSelectedShip();
                    int upgradeLevel = DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                            .getOrDefault(data.getRootNode(), 0);

                    // ルートノードが未強化なら非表示のままにする
                    if (upgradeLevel == 0)
                        continue;
                }

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

        for (Map.Entry<String, BufferedImage> entry : UM.getSkillTreeButtonImageCache().entrySet()) {
            String skillName = entry.getKey();
            BufferedImage image = entry.getValue();

            Vector2 skillTreeButtonSize = new Vector2(
                    30 * scale * ShootingGameMain.getScreenSizeAdj(),
                    30 * scale * ShootingGameMain.getScreenSizeAdj());
            JButton button = UIBuilder.createImageButton(
                    component, image, skillTreeButtonSize, skillName);

            button.addActionListener(e -> {
                skillDetailPanel.showSkillDetai(skillName);
            });
            button.setRolloverEnabled(false);

            this.add(button);
            button.setVisible(false);

            // ボタン画像のセット、画像がなければテキストをセット
            if (button.getIcon() != null) {
                button.setIcon(new ImageIcon(image));
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setText("");
            } else
                button.setText(skillName);

            buttonMap.put(skillName, button);
        }
    }

    /**
     * ハンドラの追加
     */
    public void addHandler() {
        MAIN.addMouseListener(mouseHandler);
        MAIN.addMouseMotionListener(mouseHandler);
        MAIN.addMouseWheelListener(mouseHandler);
    }

    /**
     * ハンドラの削除
     */
    public void removeHandler() {
        MAIN.removeMouseListener(mouseHandler);
        MAIN.removeMouseMotionListener(mouseHandler);
        MAIN.removeMouseWheelListener(mouseHandler);
    }
}
