package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * スキルツリーのボタン画像を作る(模様の画像に、枠とレベルの札を重ねる)
 * <ul>
 * <li>強化していない: 枠は灰青、模様は少し暗い</li>
 * <li>強化中: 枠は水色、札は「Lv3」</li>
 * <li>最大レベル: 枠は金色で光り、札は「MAX」</li>
 * </ul>
 * 画面の都合に依存しない描画だけを行う。
 */
public final class SkillIconComposer {
    private static final int SIZE = 132;
    private static final Color CYAN = new Color(70, 215, 255);
    private static final Color GOLD = new Color(255, 205, 70);
    private static final Color IDLE = new Color(105, 135, 165);

    private SkillIconComposer() {
    }

    /**
     * @param glyph    模様の画像(透明な背景)
     * @param level    いまのレベル
     * @param maxLevel 最大レベル(根のボタンなど強化できないものは 1 以下)
     * @param root     根のボタン(札を付けない)
     */
    public static BufferedImage compose(BufferedImage glyph, int level, int maxLevel, boolean root) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        boolean max = !root && maxLevel > 1 && level >= maxLevel;
        boolean idle = !root && level <= 0;
        Color frame = max ? GOLD : idle ? IDLE : CYAN;

        double pad = 8;
        RoundRectangle2D body = new RoundRectangle2D.Double(pad, pad, SIZE - 2 * pad, SIZE - 2 * pad, 34, 34);

        // 最大レベルは、枠の外に金色の光を出す
        if (max) {
            for (int i = 4; i >= 1; i--) {
                g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 28));
                g.setStroke(new BasicStroke(i * 3f));
                g.draw(body);
            }
        }

        // 背景
        g.setPaint(new GradientPaint(0, 0, max ? new Color(66, 52, 24) : new Color(30, 48, 86),
                0, SIZE, max ? new Color(30, 22, 10) : new Color(12, 20, 44)));
        g.fill(body);

        // 模様
        int gs = (int) (SIZE * 0.66);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, idle ? 0.5f : 1f));
        if (glyph != null)
            g.drawImage(glyph, (SIZE - gs) / 2, (int) (pad + 6), gs, gs, null);
        g.setComposite(AlphaComposite.SrcOver);

        // 枠
        g.setColor(frame);
        g.setStroke(new BasicStroke(max ? 8f : 5f));
        g.draw(body);
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(pad + 6, pad + 6, SIZE - 2 * pad - 12, SIZE - 2 * pad - 12, 26, 26));

        // 札(強化できるボタンだけ)
        if (!root && maxLevel > 1) {
            String text = max ? "MAX" : "Lv" + level;
            g.setFont(new Font("SansSerif", Font.BOLD, max ? 24 : 26));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text) + 20, h = 32;
            int x = (SIZE - w) / 2, y = SIZE - h - 2;
            g.setColor(max ? new Color(60, 44, 8, 235) : new Color(8, 16, 36, 230));
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(frame);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(x, y, w, h, h, h);
            g.setColor(max ? new Color(255, 230, 140) : idle ? new Color(170, 190, 210) : Color.WHITE);
            g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.dispose();
        return img;
    }
}
