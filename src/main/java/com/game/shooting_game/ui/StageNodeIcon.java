package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * ノーマルステージ画面のステージボタンの絵(光る丸い台に、浮かぶ番号と、下に★)
 * 通常=青、★3つ=金、ボス=赤い台にドクロ。未解放は灰色。画像ファイルがなくても、コードで描く。
 */
public final class StageNodeIcon {
    /** 台の見た目 */
    public enum Look {
        NORMAL(new Color(70, 170, 255)),
        GOLD(new Color(255, 190, 60)),
        BOSS(new Color(255, 70, 80)),
        LOCKED(new Color(110, 120, 140));

        private final Color color;

        Look(Color color) {
            this.color = color;
        }
    }

    /** 台の中心が、絵の上から何割の高さか(ステージ同士をつなぐ線は、ここを通す) */
    public static final double PLATFORM_CENTER = 0.66;

    private StageNodeIcon() {
    }

    /**
     * 台の見た目を決める(未解放が最優先、次にボス、★3つなら金)
     */
    public static Look lookOf(boolean locked, boolean boss, int stars) {
        if (locked)
            return Look.LOCKED;
        if (boss)
            return Look.BOSS;
        return stars >= 3 ? Look.GOLD : Look.NORMAL;
    }

    /**
     * 絵を描く
     *
     * @param w     幅(px)
     * @param h     高さ(px。幅の1.15倍ほど)
     * @param label ステージ番号
     * @param look  台の見た目
     * @param stars 獲得した★(0〜3)。未解放では描かない
     */
    public static BufferedImage create(int w, int h, String label, Look look, int stars) {
        return create(w, h, label, look, stars, false);
    }

    /**
     * 絵を描く(★の色を選べる)
     *
     * @param silverStars ★をシルバーで描くか(ナイトメアをクリアしたステージ。falseなら金)
     */
    public static BufferedImage create(int w, int h, String label, Look look, int stars, boolean silverStars) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        UIBuilder.applyHighQualityRenderSettings(g);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color c = look.color;

        // 台(楕円。外側へにじむ光 → 暗い板 → 縁)
        double cx = w / 2.0, cy = h * PLATFORM_CENTER;
        double rx = w * 0.42, ry = w * 0.2;
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (rx * 1.35), new float[] { 0.5f, 1f },
                new Color[] { alpha(c, 110), alpha(c, 0) }));
        g.fill(new Ellipse2D.Double(cx - rx * 1.35, cy - ry * 1.9, rx * 2.7, ry * 3.8));
        g.setColor(new Color(10, 18, 40, 230));
        g.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        g.setStroke(new BasicStroke(Math.max(1.5f, w / 20f)));
        g.setColor(c);
        g.draw(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        g.setStroke(new BasicStroke(1f));
        g.setColor(alpha(c, 120));
        g.draw(new Ellipse2D.Double(cx - rx * 0.66, cy - ry * 0.66, rx * 1.32, ry * 1.32));

        if (look == Look.BOSS) {
            drawSkull(g, cx, cy - ry * 0.1, w * 0.32, c);
        } else {
            // 台の上に浮かぶ番号(縁取りつき)
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (w * 0.48))));
            FontMetrics fm = g.getFontMetrics();
            float tx = (float) (cx - fm.stringWidth(label) / 2.0);
            float ty = (float) (cy - ry * 0.7);
            g.setColor(new Color(0, 0, 0, 160));
            g.drawString(label, tx + 1.5f, ty + 1.5f);
            g.setColor(look == Look.LOCKED ? new Color(170, 175, 190) : Color.WHITE);
            g.drawString(label, tx, ty);
        }

        // ★(未解放は出さない。取れていないものは暗い枠)
        if (look != Look.LOCKED) {
            double sr = w * 0.12, sy = h - sr - 1;
            for (int i = 0; i < 3; i++) {
                double sx = cx + (i - 1) * sr * 2.5;
                Path2D star = star(sx, sy, sr);
                if (i < stars) {
                    g.setColor(silverStars ? new Color(214, 224, 240) : new Color(255, 205, 50));
                    g.fill(star);
                } else {
                    g.setColor(new Color(110, 130, 190, 170));
                    g.setStroke(new BasicStroke(1f));
                    g.draw(star);
                }
            }
        }
        g.dispose();
        return img;
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** 5つ角の星 */
    static Path2D star(double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double rad = (i % 2 == 0 ? r : r * 0.45);
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(ang) * rad, y = cy + Math.sin(ang) * rad;
            if (i == 0)
                p.moveTo(x, y);
            else
                p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    /** ドクロ(頭と顎、目と鼻) */
    private static void drawSkull(Graphics2D g, double cx, double cy, double r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - r, cy - r * 1.05, r * 2, r * 1.7));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(cx - r * 0.55, cy + r * 0.3, r * 1.1, r * 0.8, 4, 4));
        g.setColor(new Color(10, 18, 40));
        g.fill(new Ellipse2D.Double(cx - r * 0.7, cy - r * 0.3, r * 0.55, r * 0.6));
        g.fill(new Ellipse2D.Double(cx + r * 0.15, cy - r * 0.3, r * 0.55, r * 0.6));
        Path2D nose = new Path2D.Double();
        nose.moveTo(cx, cy + r * 0.2);
        nose.lineTo(cx - r * 0.12, cy + r * 0.45);
        nose.lineTo(cx + r * 0.12, cy + r * 0.45);
        nose.closePath();
        g.fill(nose);
    }
}
