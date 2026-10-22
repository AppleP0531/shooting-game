package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.game.shooting_game.ShootingGameMain;

/**
 * 画面の上下に敷く帯(暗い紺の板と、ネオンの水色の縁)
 * ステータス(上)やボタン列(下)の背景。後ろの絵を少し透かす。ボタンや文字は、この上に描く。
 */
public final class SceneBars {
    /** 上の帯の高さ(基準サイズ405x720での長さ) */
    public static final double TOP = 44;

    private SceneBars() {
    }

    /**
     * 描画
     *
     * @param topH    上の帯の高さ(基準サイズの長さ。0なら描かない)
     * @param bottomH 下の帯の高さ(基準サイズの長さ。0なら描かない)
     */
    public static void draw(Graphics g, double topH, double bottomH) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int w = ShootingGameMain.getInstance().getWidth();
        int h = ShootingGameMain.getInstance().getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(Math.max(1f, (float) (1.5 * adj))));
        if (topH > 0) {
            int th = (int) (topH * adj);
            g2.setPaint(new GradientPaint(0, 0, new Color(18, 34, 72, 235), 0, th, new Color(8, 16, 38, 200)));
            g2.fillRect(0, 0, w, th);
            g2.setColor(new Color(70, 215, 255, 200));
            g2.drawLine(0, th, w, th);
        }
        if (bottomH > 0) {
            int bh = (int) (bottomH * adj);
            g2.setPaint(new GradientPaint(0, h - bh, new Color(8, 16, 38, 200), 0, h, new Color(18, 34, 72, 235)));
            g2.fillRect(0, h - bh, w, bh);
            g2.setColor(new Color(70, 215, 255, 200));
            g2.drawLine(0, h - bh, w, h - bh);
        }
        g2.dispose();
    }
}
