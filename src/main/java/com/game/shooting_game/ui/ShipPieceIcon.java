package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import com.game.shooting_game.gameScene.ship.ShipManager;

/**
 * 機体のピースのアイコン(機体の絵の右下に、ピース(パズルの1片)の絵を重ねる)
 * ピースの絵は画像ではなく図形で描く。
 */
public final class ShipPieceIcon {
    private static final Color GOLD = new Color(255, 214, 80);
    private static final Color PIECE = new Color(70, 215, 255);

    private ShipPieceIcon() {
    }

    /**
     * 描画
     *
     * @param x,y,size 左上と、1辺の長さ(ピースの絵は、この右下にはみ出さずに収まる)
     */
    public static void draw(Graphics2D g0, String shipId, int x, int y, int size) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int arc = Math.max(4, size / 5);

        g.setColor(new Color(255, 255, 255, 22));
        g.fillRoundRect(x, y, size, size, arc, arc);

        // 機体の絵(左上寄りに置いて、右下をピースに空ける)
        var all = ShipManager.getInstance().getShipImage();
        BufferedImage[] frames = all == null ? null : all.get(shipId);
        if (frames != null && frames.length > 0 && frames[0] != null) {
            BufferedImage img = frames[0];
            double k = Math.min((double) size / img.getWidth(), (double) size / img.getHeight()) * 0.8;
            int dw = (int) (img.getWidth() * k);
            int dh = (int) (img.getHeight() * k);
            g.drawImage(img, x + (int) (size * 0.06), y + (int) (size * 0.06) + (size * 8 / 10 - dh) / 2, dw, dh,
                    null);
        }

        g.setColor(GOLD);
        g.setStroke(new BasicStroke(Math.max(1f, size / 28f)));
        g.drawRoundRect(x, y, size, size, arc, arc);

        // 右下のピース
        int p = (int) (size * 0.5);
        drawPiece(g, x + size - p, y + size - p, p);
        g.dispose();
    }

    /**
     * ピース1片(四角い本体に、上と右の出っ張りがついた形)を、s×s の枠に描く
     */
    private static void drawPiece(Graphics2D g, int x, int y, int s) {
        double knob = s * 0.32; // 出っ張りの直径
        double body = s - knob / 2; // 本体の1辺
        double bx = x, by = y + knob / 2;
        Area shape = new Area(new RoundRectangle2D.Double(bx, by, body, body, s * 0.2, s * 0.2));
        shape.add(new Area(new Ellipse2D.Double(bx + body / 2 - knob / 2, by - knob / 2, knob, knob))); // 上
        shape.add(new Area(new Ellipse2D.Double(bx + body - knob / 2, by + body / 2 - knob / 2, knob, knob))); // 右

        g.setColor(new Color(0, 20, 40, 220));
        g.setStroke(new BasicStroke(Math.max(2f, s / 7f)));
        g.draw(shape);
        g.setColor(PIECE);
        g.fill(shape);
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(Math.max(1f, s / 14f)));
        g.draw(shape);
    }
}
