package com.game.shooting_game.upgradeScene.subPanels;

import com.game.shooting_game.ui.UIStyle;
import static com.game.shooting_game.ui.UIStyle.px;
import java.awt.Color;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.cost.Cost;
import com.game.shooting_game.domain.subship.SubShipService;
import com.game.shooting_game.domain.subship.SubShipService.PartData;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * サブ機工房パネル
 * パーツ素材とコインでパーツを制作し、必須パーツが揃うとサブ機が自機の両端に付く
 * 画面(縦長 405x720 が基準)に収まる大きさで、上から「状態 → パーツ選択 → 説明 → コスト → ボタン」の順に並べる
 */
public class SubShipPanel extends JPanel {
    private static final String component = "[SubShipPanel]";
    private static final Color SELECTED = new Color(120, 200, 255);
    private static final Color NORMAL_TEXT = new Color(40, 40, 40);

    private final ShootingGameMain MAIN;

    private Vector2 panelSize;
    private String selectedKind;

    private JTextArea summaryArea;
    private JTextArea detailArea;
    private JTextArea statusArea;
    private CostPanel costPanel;
    private SubShipPreview preview;
    private JLabel hullLabel;
    private JButton hullPrevBtn;
    private JButton hullNextBtn;
    private JButton craftBtn;
    private JButton equipBtn;
    private final Map<String, JButton> partBtns = new LinkedHashMap<>();

    public SubShipPanel() {
        MAIN = ShootingGameMain.getInstance();
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        // 基準サイズは画面幅(405)より少し小さい幅にする
        panelSize = new Vector2(px(385), px(534));
        setBounds(
                MAIN.getWidth() / 2 - (int) panelSize.getX() / 2,
                MAIN.getHeight() / 2 - (int) panelSize.getY() / 2,
                (int) panelSize.getX(), (int) panelSize.getY());

        int pad = px(8);
        int innerW = (int) panelSize.getX() - 2 * pad;

        // 全体の状態
        summaryArea = createTextArea(12);
        summaryArea.setBounds(pad, px(6), innerW, px(34));
        this.add(summaryArea);

        // パーツ選択(横一列)
        int count = SubShipService.getConfig().size();
        int gap = px(4);
        int btnW = count > 0 ? (innerW - gap * (count - 1)) / count : innerW;
        int btnH = px(46);
        int x = pad;
        for (String kind : SubShipService.getConfig().keySet()) {
            JButton btn = new JButton();
            btn.setBounds(x, px(44), btnW, btnH);
            btn.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
            btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                selectedKind = kind;
                refresh();
            });
            this.add(btn);
            partBtns.put(kind, btn);
            x += btnW + gap;
        }

        // 使うフレームの切り替え(制作済みのフレームの中から選べる。見た目と性能がそのフレームのものになる)
        int selY = px(94);
        hullPrevBtn = new JButton("◀");
        hullNextBtn = new JButton("▶");
        hullLabel = new JLabel("", SwingConstants.CENTER);
        hullLabel.setForeground(Color.WHITE);
        hullLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(12))));
        hullPrevBtn.setBounds(pad, selY, px(40), px(24));
        hullNextBtn.setBounds(pad + innerW - px(40), selY, px(40), px(24));
        hullLabel.setBounds(pad + px(44), selY, innerW - px(88), px(24));
        for (JButton b : new JButton[] { hullPrevBtn, hullNextBtn }) {
            b.setMargin(new java.awt.Insets(0, 0, 0, 0));
            b.setFocusPainted(false);
            this.add(b);
        }
        hullPrevBtn.addActionListener(e -> {
            SubShipService.cycleHull(-1);
            refresh();
        });
        hullNextBtn.addActionListener(e -> {
            SubShipService.cycleHull(1);
            refresh();
        });
        this.add(hullLabel);

        // プレビュー(いまのサブ機と、制作後のサブ機)
        preview = new SubShipPreview();
        preview.setBounds(pad, px(122), innerW, px(100));
        this.add(preview);

        // 説明
        detailArea = createTextArea(12);
        detailArea.setBounds(pad, px(226), innerW, px(100));
        this.add(detailArea);

        // コスト(アイコン + 所有pt/必要pt + 持っている素材のレア度)。最大でコイン+素材5種=3行
        costPanel = new CostPanel(2);
        costPanel.setBounds(pad, px(330), innerW, costPanel.maxHeight());
        this.add(costPanel);

        // 状態メッセージ
        statusArea = createTextArea(12);
        statusArea.setBounds(pad, px(330) + costPanel.maxHeight() + px(2), innerW, px(34));
        this.add(statusArea);

        // 制作ボタン・装備ボタン
        Vector2 btnSize = new Vector2(px(150), px(40));
        int btnY = (int) panelSize.getY() - (int) btnSize.getY() - px(8);
        craftBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/craftBtn.png", btnSize, "CRAFT");
        craftBtn.setBounds((int) panelSize.getX() / 2 - (int) btnSize.getX() - px(6), btnY,
                (int) btnSize.getX(), (int) btnSize.getY());
        craftBtn.addActionListener(e -> {
            if (SubShipService.craft(selectedKind))
                refresh();
        });
        this.add(craftBtn);

        equipBtn = UIBuilder.createImageBtn(component, "/static/img/UI/upgradeScene/equipBtn.png", btnSize, "EQUIP");
        equipBtn.setBounds((int) panelSize.getX() / 2 + px(6), btnY,
                (int) btnSize.getX(), (int) btnSize.getY());
        equipBtn.addActionListener(e -> {
            SubShipService.setEquipped(!SubShipService.isEquipped());
            refresh();
        });
        this.add(equipBtn);
    }

    private JTextArea createTextArea(double fontSize) {
        return UIStyle.textArea(fontSize, false);
    }

    /**
     * 表示内容の更新
     */
    private void refresh() {
        // パーツボタン(1行目: 名前、2行目: 状態)
        for (Map.Entry<String, JButton> e : partBtns.entrySet()) {
            PartData p = SubShipService.getConfig().get(e.getKey());
            int tier = SubShipService.getTier(e.getKey());
            String state = tier < 1 ? "未制作" : "Mk" + tier + (tier >= SubShipService.maxTier(e.getKey()) ? " MAX" : "");
            e.getValue().setText("<html><center>" + p.getTitle() + "<br>" + state + "</center></html>");
            boolean selected = e.getKey().equals(selectedKind);
            e.getValue().setBackground(selected ? SELECTED : null);
            e.getValue().setForeground(NORMAL_TEXT);
        }

        // 使うフレーム(2種類以上作ったときだけ切り替えられる)
        boolean canSwitch = SubShipService.getTier("HULL") >= 2;
        hullLabel.setText("使用フレーム: " + SubShipService.hullLabel());
        hullPrevBtn.setEnabled(canSwitch);
        hullNextBtn.setEnabled(canSwitch);
        preview.setSelectedKind(selectedKind);
        summaryArea.setText(SubShipService.summary());
        detailArea.setText(SubShipService.describe(selectedKind));
        detailArea.setCaretPosition(0);

        Cost cost = SubShipService.getCost(selectedKind);
        costPanel.setCost(cost);
        statusArea.setText(SubShipService.status(selectedKind));

        craftBtn.setEnabled(SubShipService.canAfford(cost));
        equipBtn.setEnabled(SubShipService.isAssembled());
        // 画像がないときはボタンの文字を状態に合わせる
        if (equipBtn.getIcon() == null)
            equipBtn.setText(SubShipService.isEquipped() ? "取り外す" : "装備する");
        if (craftBtn.getIcon() == null)
            craftBtn.setText(SubShipService.getTier(selectedKind) < 1 ? "制作する" : "強化する");
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        if (selectedKind == null && !partBtns.isEmpty())
            selectedKind = partBtns.keySet().iterator().next();
        refresh();

        UIBuilder.openPanel(this, "サブ機工房", true, null);
        // 後ろのスキルツリーが透けて文字が読みにくくならないよう、背景を濃くする
        setBackground(UIStyle.PANEL_BG);
    }
}
