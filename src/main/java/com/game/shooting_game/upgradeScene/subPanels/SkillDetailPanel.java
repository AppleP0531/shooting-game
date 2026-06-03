package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class SkillDetailPanel extends JPanel {
    private static final String component = "[SkillDetailPanel]";

    private final UpgradeManager UM;

    private Vector2 panelSize; // スキル詳細パネルの大きさ
    private int spacing; // 外周のスペース
    private String skillName; // 詳細参照中のボタンの名前

    private JTextArea detailTextArea;
    private JButton upgradeButton;

    public SkillDetailPanel() {
        UM = UpgradeManager.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        panelSize = new Vector2(250 * ShootingGameMain.getScreenSizeAdj(),
                300 * ShootingGameMain.getScreenSizeAdj());
        setBounds(
                UM.getSkillTreeButtonPanel().getWidth() / 2 - (int) panelSize.getX() / 2,
                UM.getSkillTreeButtonPanel().getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());
        spacing = (int) (5 * ShootingGameMain.getScreenSizeAdj());

        // スクロール可能な詳細テキストエリアの追加
        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true); // 自動改行
        detailTextArea.setWrapStyleWord(true); // 単語単位で改行
        detailTextArea.setOpaque(false);
        detailTextArea.setForeground(Color.WHITE);
        detailTextArea.setCaretColor(new Color(0, 0, 0, 0)); // カーソルの色を透明にする
        // 文字サイズの指定
        double fontSize = 14 * ShootingGameMain.getScreenSizeAdj();
        detailTextArea.setFont(new Font("SansSerif", Font.PLAIN, (int) fontSize));
        JScrollPane scrollPane = new JScrollPane(detailTextArea);
        scrollPane.setBorder(null); // 境界線を消す
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBounds(
                spacing, spacing,
                (int) panelSize.getX() - 2 * spacing,
                (int) (panelSize.getY() - 3 * spacing - 50 * ShootingGameMain.getScreenSizeAdj()));
        this.add(scrollPane);

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
            String nowShip = DataManager.getUserData().getSelectedShip();
            int upgradeLevel = DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                    .getOrDefault(skillName, 0);
            DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                    .put(skillName, upgradeLevel + 1);
            UM.reloadScreen();

            UIBuilder.closePanel(this);
        });
        this.add(upgradeButton);
    }

    /**
     * スキル詳細パネルの表示
     */
    public void showSkillDetail(String skillName) {
        this.skillName = skillName;

        // タイトルの設定
        String nowShip = DataManager.getUserData().getSelectedShip();
        int upgradeLevel = DataManager.getUserData().getShips().get(nowShip).getUpgrades()
                .getOrDefault(skillName, 0);
        String title = skillName + " Level:" + upgradeLevel;

        // 詳細情報の設定
        String detailText = UM.getSkillTreeConfigMap().stream()
                .filter(data -> data.getName().equals(skillName))
                .findFirst()
                .map(data -> data.getDetail())
                .orElse("No details available.");
        detailTextArea.setText(detailText);
        detailTextArea.setCaretPosition(0); // テキストエリアのスクロール位置を先頭にする

        UIBuilder.openPanel(this, title, true, null);
    }
}
