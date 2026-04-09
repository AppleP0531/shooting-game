package src.upgradeScene.subPanels;

import src.upgradeScene.UpgradeManager;
import src.utils.UIBuilder;
import src.utils.Vector2;
import src.ShootingGameMain;
import src.data.DataManager;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JButton;

/**
 * スキル詳細パネル描画クラス
 */
public class SkillDetailPanel extends JPanel {
    private static final String component = "[SkillDetailPanel]";

    private final UpgradeManager UM;

    private Vector2 panelSize; // スキル詳細パネルの大きさ
    private int spacing; // 外周のスペース
    private String skillName; // 詳細参照中のボタンの名前

    private JButton upgradeButton;

    public SkillDetailPanel() {
        UM = UpgradeManager.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setBackground(new Color(0, 0, 0, 220));
        setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        setVisible(false);

        panelSize = new Vector2(250 * ShootingGameMain.getScreenSizeAdj(),
                300 * ShootingGameMain.getScreenSizeAdj());
        spacing = (int) (5 * ShootingGameMain.getScreenSizeAdj());

        // 強化ボタン(スキルのレベルアップ)
        Vector2 upgradeButtonSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String upgradeImagePath = "/static/img/UI/upgradeScene/upgradeButton.png";
        upgradeButton = UIBuilder.createImageButton(component, upgradeImagePath, upgradeButtonSize, "UPGRADE");
        upgradeButton.setBounds(
                (int) panelSize.getX() / 2 - (int) upgradeButton.getWidth() / 2,
                (int) panelSize.getY() - (int) upgradeButton.getHeight() - spacing,
                (int) upgradeButton.getWidth(), (int) upgradeButton.getHeight());
        upgradeButton.addActionListener(e -> {
            // 強化レベルを上げる
            String nowShip = DataManager.getUserData().getSelectedShip();
            int upgradeLevel = DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                    .getOrDefault(skillName, 0);
            DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                    .put(skillName, upgradeLevel + 1);
            UM.setup();

            setVisible(false);
        });
        this.add(upgradeButton);

        // 背後へのクリックを阻止
        MouseAdapter blocker = new MouseAdapter() {
        };
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
    }

    /**
     * スキル詳細パネルの表示
     */
    public void showSkillDetai(String skillName) {
        this.skillName = skillName;
        setBounds(
                UM.getSkillTreeButtonPanel().getWidth() / 2 - (int) panelSize.getX() / 2,
                UM.getSkillTreeButtonPanel().getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        UIBuilder.openPanel(this, skillName);
    }
}
