package com.game.shooting_game.upgradeScene.subPanels;

import com.game.shooting_game.ui.UIStyle;
import static com.game.shooting_game.ui.UIStyle.px;
import com.game.shooting_game.domain.cost.CostService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Family;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.convert.ConvertService;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.ResourceIcons;
import com.game.shooting_game.ui.UIBuilder;

/**
 * 素材変換パネル
 * 同じ素材の同じレア度5個を、1つ上のレア度1個に変換する。上から素材を選び、下で変換する。
 */
public class ConvertPanel extends JPanel {
    private static final Color SELECTED = new Color(120, 200, 255);
    private static final Rarity[] RARITIES = Rarity.values();

    private ResourceName selected;
    private final Map<ResourceName, JButton> matBtns = new LinkedHashMap<>();
    private JLabel selectedLabel;
    private Row[] rows;

    /**
     * 1段分(あるレア度 → 1つ上のレア度)
     */
    private class Row extends JPanel {
        private final Rarity from;
        private final JButton onceBtn = new JButton("1回");
        private final JButton maxBtn = new JButton("最大");

        Row(Rarity from) {
            this.from = from;
            setOpaque(false);
            setLayout(null);

            for (JButton b : new JButton[] { onceBtn, maxBtn }) {
                b.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
                b.setMargin(new Insets(0, 0, 0, 0));
                b.setFocusPainted(false);
                add(b);
            }
            onceBtn.addActionListener(e -> run(1));
            maxBtn.addActionListener(e -> run(Integer.MAX_VALUE));
        }

        void layoutButtons() {
            int w = getWidth();
            int h = getHeight();
            onceBtn.setBounds(w - px(112), px(6), px(50), h - px(12));
            maxBtn.setBounds(w - px(58), px(6), px(54), h - px(12));
        }

        private void run(int times) {
            if (selected == null)
                return;
            ConvertService.convert(selected, from, times);
            ConvertPanel.this.refresh();
        }

        void update() {
            int times = selected == null ? 0 : ConvertService.maxTimes(selected, from);
            onceBtn.setEnabled(times >= 1);
            maxBtn.setEnabled(times >= 1);
            maxBtn.setText(times >= 1 ? "最大 ×" + times : "最大");
            repaint();
        }

        private void cell(Graphics2D g2, int x, int y, Rarity r, int count) {
            Color ring = CostPanel.rarityColor(r);
            int size = px(24);
            g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 60));
            g2.fillRoundRect(x - 2, y - 2, size + 4, size + 4, 8, 8);
            BufferedImage img = selected == null ? null : ResourceIcons.get(new ResourceKinds(selected, r), size);
            if (img != null)
                g2.drawImage(img, x + (size - img.getWidth()) / 2, y + (size - img.getHeight()) / 2, null);
            g2.setColor(ring);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(x - 2, y - 2, size + 4, size + 4, 8, 8);

            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(12))));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(ring);
            g2.drawString(CostPanel.rarityName(r), x + size + 8, y + fm.getAscent() - 1);
            g2.setColor(Color.WHITE);
            g2.drawString("×" + count, x + size + 8, y + size + 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);

            Rarity to = ConvertService.next(from);
            int own = selected == null ? 0 : CostService.ownedReal(new ResourceKinds(selected, from));
            int ownTo = selected == null || to == null ? 0 : CostService.ownedReal(new ResourceKinds(selected, to));
            int y = (getHeight() - px(24)) / 2;
            cell(g2, px(8), y, from, own);

            g2.setColor(new Color(230, 230, 230));
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
            g2.drawString(ConvertService.RATE + "個 ▶ 1個", px(112), getHeight() / 2 + px(4));

            if (to != null)
                cell(g2, px(172) - px(0), y, to, ownTo);
            g2.dispose();
        }
    }

    /**
     * UIコンポーネントの初期化
     */
    public void initComponents() {
        setLayout(null);
        setVisible(false);

        int w = px(385);
        int h = px(510);
        setBounds(
                ShootingGameMain.getInstance().getWidth() / 2 - w / 2,
                ShootingGameMain.getInstance().getHeight() / 2 - h / 2, w, h);
        int pad = px(8);
        int innerW = w - 2 * pad;

        JLabel guide = new JLabel("<html>同じ素材の同じレア度 5個 → 1つ上のレア度 1個<br>"
                + "必要なレア度の素材が足りないときに使います(上から下には変換できません)</html>");
        guide.setForeground(Color.WHITE);
        guide.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        guide.setBounds(pad, px(4), innerW, px(34));
        add(guide);

        // 素材の選択(強化素材 / パーツ素材)
        int y = px(42);
        for (Family family : new Family[] { Family.UPGRADE, Family.PART, Family.MODULE }) {
            JLabel label = new JLabel(family == Family.UPGRADE ? "強化素材(スキル強化に使う)"
                    : family == Family.PART ? "パーツ素材(サブ機のパーツに使う)" : "モジュール基板(モジュールの制作に使う)");
            label.setForeground(new Color(200, 230, 255));
            label.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
            label.setBounds(pad, y, innerW, px(16));
            add(label);
            y += px(18);

            int count = 0;
            for (ResourceName n : ResourceName.values())
                if (n.getFamily() == family)
                    count++;
            int gap = px(4);
            int bw = (innerW - gap * (count - 1)) / Math.max(1, count);
            int x = pad;
            for (ResourceName n : ResourceName.values()) {
                if (n.getFamily() != family)
                    continue;
                JButton b = new JButton(n.getDisplayName());
                BufferedImage img = ResourceIcons.get(new ResourceKinds(n, Rarity.COMMON), px(26));
                if (img != null)
                    b.setIcon(new ImageIcon(img));
                b.setVerticalTextPosition(SwingConstants.BOTTOM);
                b.setHorizontalTextPosition(SwingConstants.CENTER);
                b.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, px(9))));
                b.setMargin(new Insets(0, 0, 0, 0));
                b.setFocusPainted(false);
                b.setToolTipText(n.getDisplayName());
                b.setBounds(x, y, bw, px(50));
                b.addActionListener(e -> {
                    selected = n;
                    refresh();
                });
                add(b);
                matBtns.put(n, b);
                x += bw + gap;
            }
            y += px(56);
        }

        selectedLabel = new JLabel("", SwingConstants.LEFT);
        selectedLabel.setForeground(Color.WHITE);
        selectedLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(13))));
        selectedLabel.setBounds(pad, y, innerW, px(20));
        add(selectedLabel);
        y += px(24);

        // 変換の段(コモン→アンコモン→レア→エピック→レジェンド の4段)
        rows = new Row[RARITIES.length - 1];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Row(RARITIES[i]);
            rows[i].setBounds(pad, y, innerW, px(44));
            rows[i].layoutButtons();
            add(rows[i]);
            y += px(48);
        }
    }

    private void refresh() {
        for (Map.Entry<ResourceName, JButton> e : matBtns.entrySet()) {
            e.getValue().setBackground(e.getKey() == selected ? SELECTED : null);
            e.getValue().setForeground(new Color(40, 40, 40));
        }
        selectedLabel.setText(selected == null ? "変換する素材を選んでください" : selected.getDisplayName() + " の変換");
        for (Row r : rows)
            r.update();
        repaint();
    }

    /**
     * パネルの表示
     */
    public void showPanel() {
        if (selected == null && !matBtns.isEmpty())
            selected = matBtns.keySet().iterator().next();
        refresh();

        UIBuilder.openPanel(this, "素材変換", true, null);
        setBackground(UIStyle.PANEL_BG);
    }
}
