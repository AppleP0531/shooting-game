package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * 大きなモード選択ボタンの絵(角を斜めに落とした、色つきの板に、縁取りの太い文字)
 * ロビーの下の「無限モード」「ステージ」のように、目立たせたいボタンに使う。
 * 画像ファイルを用意せず、大きさと色から描く。UIBuilder.createImageBtn に渡して使う。
 */
public final class ModeButtonImage {
    private ModeButtonImage() {
    }

    /**
     * 絵を描く
     *
     * @param w      幅(画素)
     * @param h      高さ(画素)
     * @param label  文字
     * @param top    板の上側の色
     * @param bottom 板の下側の色
     * @param edge   縁の色
     * @param fontPx 文字の大きさ(画素)
     */
    public static BufferedImage create(int w, int h, String label, Color top, Color bottom, Color edge, int fontPx) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        UIBuilder.applyHighQualityRenderSettings(g);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double cut = h * 0.28;
        Shape plate = plate(1.5, 1.5, w - 3, h - 3, cut);
        g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g.fill(plate);

        // 上半分のツヤ
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 110), 0, h * 0.5f, new Color(255, 255, 255, 0)));
        g.fill(plate(4, 4, w - 8, h / 2.0 - 4, Math.max(1, cut - 3)));

        // 縁(外側を暗く、内側を明るく)
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(edge.darker().darker());
        g.draw(plate);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(edge);
        g.draw(plate(3, 3, w - 6, h - 6, Math.max(1, cut - 2)));

        // 文字(暗い縁取りの上に白)
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, fontPx)));
        GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), label);
        Rectangle2D b = gv.getVisualBounds();
        Shape text = AffineTransform.getTranslateInstance((w - b.getWidth()) / 2 - b.getX(),
                (h - b.getHeight()) / 2 - b.getY()).createTransformedShape(gv.getOutline());
        g.setColor(bottom.darker().darker());
        g.setStroke(new BasicStroke(Math.max(2f, fontPx / 6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(text);
        g.setColor(Color.WHITE);
        g.fill(text);

        g.dispose();
        return img;
    }

    /** 角を斜めに落とした板 */
    static Shape plate(double x, double y, double w, double h, double cut) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + cut, y);
        p.lineTo(x + w - cut, y);
        p.lineTo(x + w, y + cut);
        p.lineTo(x + w, y + h - cut);
        p.lineTo(x + w - cut, y + h);
        p.lineTo(x + cut, y + h);
        p.lineTo(x, y + h - cut);
        p.lineTo(x, y + cut);
        p.closePath();
        return p;
    }
}
