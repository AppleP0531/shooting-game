package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Function;

import javax.swing.JPanel;

/**
 * 絵(アイコン)から1つを選ぶ横一列の選択欄
 * 選んでいるものは明るい枠で光り、ほかは暗く出る。絵がないものは、色つきの文字で出す。
 *
 * @param <T> 選ぶものの型
 */
public class IconChoiceRow<T> extends JPanel {
    private final List<T> items;
    private final int chip;
    private final int gap;
    private final Function<T, BufferedImage> imageOf;
    private final Function<T, Color> colorOf;
    private final Function<T, String> labelOf;
    private int selected = 0;

    public IconChoiceRow(List<T> items, int chip, int gap, Function<T, BufferedImage> imageOf,
            Function<T, Color> colorOf, Function<T, String> labelOf, Function<T, String> tooltipOf,
            Runnable onChange) {
        this.items = items;
        this.chip = chip;
        this.gap = gap;
        this.imageOf = imageOf;
        this.colorOf = colorOf;
        this.labelOf = labelOf;
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0 && i != selected) {
                    selected = i;
                    repaint();
                    onChange.run();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                setToolTipText(i < 0 ? null : tooltipOf.apply(items.get(i)));
            }
        });
    }

    public T getSelected() {
        return items.get(selected);
    }

    private int left() {
        int total = items.size() * chip + (items.size() - 1) * gap;
        return (getWidth() - total) / 2;
    }

    private int indexAt(int x) {
        int rel = x - left();
        if (rel < 0)
            return -1;
        int i = rel / (chip + gap);
        return i < items.size() && rel % (chip + gap) < chip ? i : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int x0 = left();
        int y = (getHeight() - chip) / 2;
        int arc = Math.max(6, chip / 4);
        for (int i = 0; i < items.size(); i++) {
            T t = items.get(i);
            int x = x0 + i * (chip + gap);
            boolean sel = i == selected;
            Color c = colorOf.apply(t);
            g2.setColor(new Color(10, 20, 40, 220));
            g2.fillRoundRect(x, y, chip, chip, arc, arc);
            BufferedImage img = imageOf.apply(t);
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, sel ? 1f : 0.55f));
            if (img != null) {
                int s = chip - 6;
                double sc = Math.min((double) s / img.getWidth(), (double) s / img.getHeight());
                int w = (int) (img.getWidth() * sc), h = (int) (img.getHeight() * sc);
                g2.drawImage(img, x + (chip - w) / 2, y + (chip - h) / 2, w, h, null);
            } else {
                g2.setColor(c);
                g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, chip * 2 / 5)));
                FontMetrics fm = g2.getFontMetrics();
                String s = labelOf.apply(t);
                g2.drawString(s, x + (chip - fm.stringWidth(s)) / 2, y + (chip + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
            // 枠: 選んでいるものは太く光らせ、ほかは細く暗く
            if (sel) {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
                g2.setStroke(new BasicStroke(6f));
                g2.drawRoundRect(x, y, chip, chip, arc, arc);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f));
            } else {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.drawRoundRect(x, y, chip, chip, arc, arc);
        }
        g2.dispose();
    }
}
