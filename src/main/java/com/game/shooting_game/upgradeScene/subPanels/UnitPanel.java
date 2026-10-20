package com.game.shooting_game.upgradeScene.subPanels;

import com.game.shooting_game.ui.UIStyle;
import static com.game.shooting_game.ui.UIStyle.px;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.domain.gacha.PieceService;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.UIBuilder;

/**
 * 機体強化パネル
 * 使っている機体のレベル(1〜50)を、コインと強化素材で上げる。
 * ★(0〜5)は、同じ機体の予備機を1機消費して上げる。予備機はコインで手に入る。
 * レベルと★が上がるほど、火力と耐久力の基礎値が伸びる。
 */
public class UnitPanel extends JPanel {
    private static final Color GOLD = new Color(255, 214, 80);

    private JTextArea headerArea;
    private JTextArea statArea;
    private CostPanel costPanel;
    private JTextArea starArea;
    private JTextArea statusArea;
    private JButton levelBtn;
    private CostPanel rankCostPanel; // ランクアップに必要な素材
    private JButton starBtn;

    private static String shipId() {
        return DataManager.getUserData().getSelectedShip();
    }

    private JTextArea area(double size, boolean bold) {
        return UIStyle.textArea(size, bold);
    }

    private JButton button(String text, int x, int y, int w, int h) {
        JButton b = UIStyle.button(text, 12);
        b.setBounds(x, y, w, h);
        add(b);
        return b;
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        int w = px(385);
        int h = px(520);
        setBounds(ShootingGameMain.getInstance().getWidth() / 2 - w / 2,
                ShootingGameMain.getInstance().getHeight() / 2 - h / 2, w, h);
        int pad = px(8);
        int innerW = w - 2 * pad;

        // 見出し(機体の絵 + 名前・Lv・★)
        headerArea = area(14, true);
        headerArea.setBounds(pad + px(84), px(8), innerW - px(84), px(76));
        add(headerArea);

        // 能力
        statArea = area(12, false);
        statArea.setBounds(pad, px(92), innerW, px(60));
        add(statArea);

        // レベルアップのコスト
        costPanel = new CostPanel(1);
        costPanel.setBounds(pad, px(156), innerW, costPanel.maxHeight());
        add(costPanel);

        int y = px(156) + costPanel.maxHeight() + px(4);
        levelBtn = button("レベルアップ", pad, y, innerW, px(36));
        levelBtn.addActionListener(e -> {
            if (UnitService.levelUp(shipId()))
                refresh();
        });
        y += px(44);

        // ランク(レベルが最大になったら、ランクコアで上げる。上げるとレベルは1に戻る)
        starArea = area(12, false);
        starArea.setBounds(pad, y, innerW, px(52));
        add(starArea);
        y += px(54);

        rankCostPanel = new CostPanel(1);
        rankCostPanel.setBounds(pad, y, innerW, rankCostPanel.maxHeight());
        add(rankCostPanel);
        y += rankCostPanel.maxHeight() + px(4);

        starBtn = button("ランクアップ", pad, y, innerW, px(36));
        starBtn.addActionListener(e -> {
            if (UnitService.rankUp(shipId()))
                refresh();
        });
        y += px(42);

        statusArea = area(11, false);
        statusArea.setBounds(pad, y, innerW, px(34));
        add(statusArea);
    }

    private static String pct(double rate) {
        return String.format("×%.2f", rate);
    }

    /**
     * 表示内容の更新
     */
    private void refresh() {
        String id = shipId();
        int lv = UnitService.getLevel(id);
        int rank = UnitService.getRank(id);

        headerArea.setText(id + "\nLv " + lv + " / " + UnitService.MAX_LEVEL + "\n" + UnitService.rankText(id));

        double baseHp = UnitService.baseHp(id);
        statArea.setText("火力  " + pct(UnitService.attackRate(id)) + "   (Lvごとに×1.06、ランクごとに×18)\n"
                + "耐久  " + pct(UnitService.hpRate(id)) + "   基礎HP " + Math.round(baseHp) + " → "
                + Math.round(baseHp * UnitService.hpRate(id)) + "\n(スキル強化とは別に、掛け算で効きます)");

        boolean maxLv = lv >= UnitService.MAX_LEVEL;
        costPanel.setCost(maxLv ? null : UnitService.levelCost(lv));
        levelBtn.setText(maxLv ? "レベル最大" : "レベルアップ  Lv" + lv + " → Lv" + (lv + 1));
        boolean canLv = !maxLv && CostService.canAfford(UnitService.levelCost(lv));
        levelBtn.setEnabled(canLv);

        boolean maxRank = rank >= UnitService.MAX_RANK;
        String pieceLine = maxRank ? ""
                : "\nこの機体のピース: " + PieceService.owned(id) + " / " + PieceService.rankUpPieces(rank);
        starArea.setText("Rank " + rank + " / " + UnitService.MAX_RANK + "\nレベルが最大(" + UnitService.MAX_LEVEL
                + ")になると、ランクコアとピースでランクを上げられます。\n上げるとレベルは1に戻りますが、火力と耐久が大きく上がります。"
                + pieceLine);
        rankCostPanel.setCost(maxRank || !maxLv ? null : UnitService.rankUpCost(rank));
        starBtn.setText(maxRank ? "ランク最大" : "ランクアップ  Rank " + rank + " → Rank " + (rank + 1));
        starBtn.setEnabled(UnitService.canRankUp(id));

        String msg;
        if (maxLv && maxRank)
            msg = "この機体は最大まで強化されています";
        else if (!maxLv && !canLv)
            msg = "レベルアップにはコインか強化素材が足りません";
        else if (!maxLv)
            msg = "レベルアップができます(最大になるとランクアップできます)";
        else if (maxRank)
            msg = "ランクは最大です";
        else if (!UnitService.canRankUp(id))
            msg = "ランクアップにはランクコアが足りません(ボスなどが落とします)";
        else
            msg = "ランクアップができます";
        statusArea.setText(msg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (DataManager.getUserData() == null)
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage[] frames = ShipManager.getInstance().getShipImage().get(shipId());
        int s = px(76);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(px(8), px(8), s, s, 10, 10);
        if (frames != null && frames.length > 0 && frames[0] != null) {
            BufferedImage img = frames[0];
            double k = Math.min((double) s / img.getWidth(), (double) s / img.getHeight()) * 0.9;
            int dw = (int) (img.getWidth() * k);
            int dh = (int) (img.getHeight() * k);
            g2.drawImage(img, px(8) + (s - dw) / 2, px(8) + (s - dh) / 2, dw, dh, null);
        }
        g2.setColor(GOLD);
        g2.drawRoundRect(px(8), px(8), s, s, 10, 10);
        g2.dispose();
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        refresh();
        UIBuilder.openPanel(this, "機体強化", true, null);
        setBackground(UIStyle.PANEL_BG);
    }
}
