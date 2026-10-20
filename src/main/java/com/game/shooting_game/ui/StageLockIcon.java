package com.game.shooting_game.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * ステージボタンのアイコン作り
 * 「基のアイコン」と「ロックの鍵」を別々に作り、ロック中のボタンだけ重ねる。
 * 画像ファイルがないときは、コードで描いたアイコン(番号つきの四角と南京錠)を使う。
 */
public final class StageLockIcon {
    private static final Color NEON = new Color(70, 215, 255);

    private StageLockIcon() {
    }

    /**
     * 画像がないステージボタンの代わり(暗い角丸の四角に、番号を描く)
     * 
     * @param width  幅(px)
     * @param height 高さ(px)
     * @param label  描く文字(ステージ番号)
     */
    public static BufferedImage placeholder(int width, int height, String label) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int arc = Math.max(4, width / 5);
        g2.setColor(new Color(24, 34, 60));
        g2.fill(new RoundRectangle2D.Double(1, 1, width - 2, height - 2, arc, arc));
        g2.setStroke(new BasicStroke(Math.max(1.5f, width / 25f)));
        g2.setColor(NEON);
        g2.draw(new RoundRectangle2D.Double(1, 1, width - 2, height - 2, arc, arc));

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, height * 2 / 5)));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(label, (width - fm.stringWidth(label)) / 2f, (height - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
        return image;
    }

    /**
     * 南京錠(鍵アイコンの画像がないときに使う。背景は透明)
     * 
     * @param size 一辺(px)
     */
    public static BufferedImage padlock(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double bodyW = size * 0.62;
        double bodyH = size * 0.46;
        double bodyX = (size - bodyW) / 2;
        double bodyY = size * 0.42;

        // つる(上の輪)
        float stroke = Math.max(2f, size * 0.1f);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(190, 198, 214));
        double shackleW = bodyW * 0.62;
        double shackleX = (size - shackleW) / 2;
        g2.draw(new Arc2D.Double(shackleX, size * 0.12, shackleW, size * 0.62, 0, 180, Arc2D.OPEN));
        g2.drawLine((int) shackleX, (int) (size * 0.43), (int) shackleX, (int) bodyY);
        g2.drawLine((int) (shackleX + shackleW), (int) (size * 0.43), (int) (shackleX + shackleW), (int) bodyY);

        // 本体と鍵穴
        g2.setColor(new Color(150, 160, 182));
        g2.fill(new RoundRectangle2D.Double(bodyX, bodyY, bodyW, bodyH, size * 0.14, size * 0.14));
        g2.setColor(new Color(40, 46, 62));
        double hole = size * 0.12;
        g2.fillOval((int) (size / 2 - hole / 2), (int) (bodyY + bodyH * 0.3), (int) hole, (int) hole);
        g2.fillRect((int) (size / 2 - hole / 5), (int) (bodyY + bodyH * 0.3 + hole * 0.7), (int) Math.max(1, hole / 2.5),
                (int) (bodyH * 0.25));
        g2.dispose();
        return image;
    }

    /**
     * 基のアイコンを暗くして、その上に鍵を重ねたアイコンを作る
     * 
     * @param base   基のアイコン
     * @param lock   重ねる鍵(中央に置く)
     * @param width  できあがりの幅(px)
     * @param height できあがりの高さ(px)
     */
    public static BufferedImage overlay(BufferedImage base, BufferedImage lock, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 基のアイコンは暗くして、ロック中だと分かるようにする
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        g2.drawImage(base, 0, 0, width, height, null);

        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(lock, (width - lock.getWidth()) / 2, (height - lock.getHeight()) / 2, null);
        g2.dispose();
        return image;
    }
}
