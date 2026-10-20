package com.game.shooting_game.upgradeScene.subPanels;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.domain.cost.CostService;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.upgradeScene.UpgradeManager.SkillTreeData;
import com.game.shooting_game.upgradeScene.UpgradeManager;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class SkillDetailPanel extends JPanel {
    private static final String component = "[SkillDetailPanel]";

    private final UpgradeManager UM;

    private Vector2 panelSize; // スキル詳細パネルの大きさ
    private int spacing; // 外周のスペース
    private String skillName; // 詳細参照中のボタンの名前

    private JTextArea detailTextArea;
    private JTextArea statusTextArea;
    private CostPanel costPanel;
    private JButton upgradeBtn;
    private JButton skipBtn; // 研究スキップ券を使う

    public SkillDetailPanel() {
        UM = UpgradeManager.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        panelSize = new Vector2(300 * ShootingGameMain.getScreenSizeAdj(),
                520 * ShootingGameMain.getScreenSizeAdj());
        setBounds(
                UM.getSkillTreeBtnPanel().getWidth() / 2 - (int) panelSize.getX() / 2,
                UM.getSkillTreeBtnPanel().getHeight() / 2 - (int) panelSize.getY() / 2,
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
                (int) (190 * ShootingGameMain.getScreenSizeAdj()));
        this.add(scrollPane);

        // コスト(アイコン + 所有数/必要数)
        double adj = ShootingGameMain.getScreenSizeAdj();
        costPanel = new CostPanel();
        costPanel.setBounds(spacing, (int) (200 * adj), (int) panelSize.getX() - 2 * spacing,
                costPanel.maxHeight());
        this.add(costPanel);

        // 強化できるかどうかの一言
        statusTextArea = new JTextArea();
        statusTextArea.setEditable(false);
        statusTextArea.setFocusable(false);
        statusTextArea.setLineWrap(true);
        statusTextArea.setWrapStyleWord(true);
        statusTextArea.setOpaque(false);
        statusTextArea.setForeground(Color.WHITE);
        statusTextArea.setFont(new Font("SansSerif", Font.PLAIN, (int) (12 * adj)));
        statusTextArea.setBounds(spacing, (int) (200 * adj) + costPanel.maxHeight() + spacing,
                (int) panelSize.getX() - 2 * spacing, (int) (34 * adj));
        this.add(statusTextArea);

        // 強化ボタン(スキルのレベルアップ)
        Vector2 upgradeBtnSize = new Vector2(
                100 * ShootingGameMain.getScreenSizeAdj(),
                50 * ShootingGameMain.getScreenSizeAdj());
        String upgradeImagePath = "/static/img/UI/upgradeScene/upgradeBtn.png";
        upgradeBtn = UIBuilder.createImageBtn(component, upgradeImagePath, upgradeBtnSize, "UPGRADE");
        upgradeBtn.setBounds(
                (int) panelSize.getX() / 2 - (int) upgradeBtn.getWidth() / 2,
                (int) panelSize.getY() - (int) upgradeBtn.getHeight() - spacing,
                (int) upgradeBtn.getWidth(), (int) upgradeBtn.getHeight());
        upgradeBtn.addActionListener(e -> {
            // コインと強化素材を消費して強化(足りなければ何もしない)
            if (!SkillService.purchase(skillName))
                return;

            UM.reloadScreen();
            UIBuilder.closePanel(this);
        });
        this.add(upgradeBtn);

        // 研究中のスキルでは、強化ボタンの代わりに「研究スキップ券を使う」ボタンを出す
        skipBtn = UIBuilder.createImageBtn(component, (java.awt.image.BufferedImage) null,
                new Vector2(150 * ShootingGameMain.getScreenSizeAdj(), 40 * ShootingGameMain.getScreenSizeAdj()),
                "スキップ");
        skipBtn.setBounds(
                (int) panelSize.getX() / 2 - (int) skipBtn.getWidth() / 2,
                (int) panelSize.getY() - (int) skipBtn.getHeight() - spacing,
                (int) skipBtn.getWidth(), (int) skipBtn.getHeight());
        skipBtn.addActionListener(e -> {
            if (!SkillService.skipResearchWithTicket())
                return;
            UM.reloadScreen();
            UIBuilder.closePanel(this);
        });
        skipBtn.setVisible(false);
        this.add(skipBtn);
    }

    /**
     * スキル詳細パネルの表示
     */
    public void showSkillDetail(String skillName) {
        this.skillName = skillName;

        SkillTreeData data = SkillService.find(skillName);
        String name = data != null && data.getTitle() != null ? data.getTitle() : skillName;

        // 詳細情報の設定(効果・コスト・状態を含む)
        String detailText = data != null ? SkillService.describe(data) : "No details available.";
        detailTextArea.setText(detailText);
        detailTextArea.setCaretPosition(0); // テキストエリアのスクロール位置を先頭にする

        // 強化できないときはボタンを押せなくする
        boolean upgradable = data != null
                && !skillName.equals("Origin")
                && SkillService.isUnlocked(skillName)
                && !SkillService.isMaxLevel(data)
                && !SkillService.isResearching()
                && CostService.canAfford(SkillService.getCost(data, SkillService.getLevel(skillName)));
        upgradeBtn.setEnabled(upgradable);

        // 研究中のスキルなら、強化ボタンの代わりにスキップ券のボタン
        boolean researchingThis = SkillService.isResearching(skillName);
        boolean admin = DataManager.isAdminMode(); // 管理者モードは券が無制限
        int tickets = ShopService.owned(ItemKinds.SKIP_TICKET);
        upgradeBtn.setVisible(!researchingThis);
        skipBtn.setVisible(researchingThis);
        skipBtn.setText("スキップ(券×" + (admin ? "∞" : String.valueOf(tickets)) + ")");
        skipBtn.setEnabled(admin || tickets > 0);

        // コストと状態(強化できるときだけ表示)
        boolean showCost = data != null && !skillName.equals("Origin")
                && SkillService.isUnlocked(skillName) && !SkillService.isMaxLevel(data);
        costPanel.setCost(showCost ? SkillService.getCost(data, SkillService.getLevel(skillName)) : null);
        statusTextArea.setText(data != null && showCost ? SkillService.status(data) : "");

        UIBuilder.openPanel(this, name, true, null);
    }
}
