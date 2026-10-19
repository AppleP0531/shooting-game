package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.skill.SkillService;
import com.game.shooting_game.skill.UnitService;
import com.game.shooting_game.utils.CostPanel;
import com.game.shooting_game.utils.UIBuilder;

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
    private JButton buyBtn;
    private JButton starBtn;

    private static int px(double base) {
        return (int) (base * ShootingGameMain.getScreenSizeAdj());
    }

    private static String shipId() {
        return DataManager.getUserData().getSelectedShip();
    }

    private JTextArea area(double size, boolean bold) {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);
        a.setForeground(Color.WHITE);
        a.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, Math.max(9, px(size))));
        return a;
    }

    private JButton button(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, h);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
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
        int h = px(440);
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

        // ★
        starArea = area(12, false);
        starArea.setBounds(pad, y, innerW, px(52));
        add(starArea);
        y += px(56);

        buyBtn = button("予備機を買う", pad, y, innerW / 2 - px(3), px(36));
        starBtn = button("★を上げる", pad + innerW / 2 + px(3), y, innerW / 2 - px(3), px(36));
        buyBtn.addActionListener(e -> {
            if (UnitService.buyCopy(shipId()))
                refresh();
        });
        starBtn.addActionListener(e -> {
            if (UnitService.starUp(shipId()))
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
        int stars = UnitService.getStars(id);
        int copies = UnitService.getCopies(id);

        headerArea.setText(id + "\nLv " + lv + " / " + UnitService.MAX_LEVEL + "\n" + UnitService.starText(id));

        double baseHp = UnitService.baseHp(id);
        statArea.setText("火力  " + pct(UnitService.attackRate(id)) + "   (Lvごとに+2%、★ごとに+5%)\n"
                + "耐久  " + pct(UnitService.hpRate(id)) + "   基礎HP " + Math.round(baseHp) + " → "
                + Math.round(baseHp * UnitService.hpRate(id)) + "\n(スキル強化とは別に、掛け算で効きます)");

        boolean maxLv = lv >= UnitService.MAX_LEVEL;
        costPanel.setCost(maxLv ? null : UnitService.levelCost(lv));
        levelBtn.setText(maxLv ? "レベル最大" : "レベルアップ  Lv" + lv + " → Lv" + (lv + 1));
        boolean canLv = !maxLv && SkillService.canAfford(UnitService.levelCost(lv));
        levelBtn.setEnabled(canLv);

        int price = UnitService.copyPrice(id);
        starArea.setText("★ " + stars + " / " + UnitService.MAX_STARS + "   予備機: " + copies + "機\n"
                + "同じ機体の予備機を1機使うと、★が1つ上がります。\n予備機の値段: " + price + " コイン");
        int coins = DataManager.getUserData().getResources().getCoins();
        boolean canBuy = stars + copies < UnitService.MAX_STARS && coins >= price;
        buyBtn.setText("予備機を買う (" + price + ")");
        buyBtn.setEnabled(canBuy);
        starBtn.setEnabled(stars < UnitService.MAX_STARS && copies >= 1);

        String msg;
        if (maxLv && stars >= UnitService.MAX_STARS)
            msg = "この機体は最大まで強化されています";
        else if (!maxLv && !canLv)
            msg = "レベルアップにはコインか強化素材が足りません";
        else if (stars >= UnitService.MAX_STARS)
            msg = "★は最大です";
        else if (copies < 1)
            msg = "★を上げるには予備機が必要です";
        else
            msg = "レベルアップや★上げができます";
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
        setBackground(new Color(0, 0, 0, 245));
    }
}
