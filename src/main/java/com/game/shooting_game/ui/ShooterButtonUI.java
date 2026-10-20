package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicHTML;

import com.game.shooting_game.ShootingGameMain;

/**
 * シューティングゲーム風のボタンの見た目
 * 角を斜めに落とした暗い青の板に、ネオンの水色の縁取り。押せるものはマウスを乗せると光り、押すと沈む。
 * 画像のボタン(背景を塗らないボタン)はこれまで通りに描く。
 */
public class ShooterButtonUI extends BasicButtonUI {
    private static final Color NEON = new Color(70, 215, 255);
    private static final Color NEON_HOVER = new Color(160, 240, 255);
    private static final Color TOP = new Color(34, 56, 100);
    private static final Color BOTTOM = new Color(10, 20, 46);
    private static final Color TEXT = new Color(228, 244, 255);
    private static final Color DISABLED_EDGE = new Color(96, 110, 128);
    private static final Color DISABLED_TOP = new Color(52, 58, 70);
    private static final Color DISABLED_BOTTOM = new Color(30, 33, 42);
    private static final Color DISABLED_TEXT = new Color(165, 176, 190);

    private static final String HTML_COLOR_KEY = "shooterButton.htmlColor";

    /** UIManager が呼ぶ */
    public static ComponentUI createUI(JComponent c) {
        return new ShooterButtonUI();
    }

    /**
     * すべてのボタンをこの見た目にする(画面を作る前に呼ぶ)
     */
    public static void install() {
        UIManager.put("ButtonUI", ShooterButtonUI.class.getName());
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setRolloverEnabled(true);
        b.setForeground(TEXT);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        return d == null ? null : new Dimension(d.width + 12, d.height + 6);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        // 画像のボタン(背景を塗らない)は、アイコンと文字だけをいつも通りに描く
        if (!b.isContentAreaFilled()) {
            super.paint(g, c);
            return;
        }

        syncTextColor(b);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ButtonModel m = b.getModel();
        boolean enabled = b.isEnabled();
        boolean pressed = enabled && m.isArmed() && m.isPressed();
        boolean hover = enabled && m.isRollover();
        boolean highlighted = enabled && isHighlighted(b);

        int w = b.getWidth();
        int h = b.getHeight();
        double adj = ShootingGameMain.getScreenSizeAdj();
        double cut = Math.min(Math.min(w, h) / 3.0, 7 * adj);
        Shape shape = plate(1, 1, w - 2, h - 2, cut);

        // 板(押すと少し沈む)
        Color top = enabled ? TOP : DISABLED_TOP;
        Color bottom = enabled ? BOTTOM : DISABLED_BOTTOM;
        if (highlighted) {
            top = new Color(40, 120, 170);
            bottom = new Color(14, 60, 100);
        }
        if (hover) {
            top = brighten(top, 1.35);
            bottom = brighten(bottom, 1.45);
        }
        if (pressed) {
            Color t = top;
            top = bottom;
            bottom = t;
        }
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fill(shape);

        // 上側のうっすらしたツヤ
        g2.setColor(new Color(255, 255, 255, enabled ? 28 : 12));
        g2.fill(plate(3, 3, w - 6, (h - 6) / 2.0, Math.max(1, cut - 2)));

        // ネオンの縁取り(マウスを乗せると明るく、光がにじむ)
        Color edge = !enabled ? DISABLED_EDGE : (hover || highlighted ? NEON_HOVER : NEON);
        if (enabled && (hover || highlighted)) {
            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 22 * (4 - i)));
                g2.setStroke(new BasicStroke(1.5f + i * 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(shape);
            }
        }
        g2.setColor(edge);
        g2.setStroke(new BasicStroke(pressed ? 2.4f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        g2.dispose();

        // 文字・アイコン(押すと1ピクセル下がる)
        Graphics g3 = g.create();
        if (pressed)
            g3.translate(0, 1);
        super.paint(g3, c);
        g3.dispose();
    }

    /**
     * 文字の色を、使えるときは明るく、使えないときは灰色にそろえる。
     * HTMLのラベルは、文字の色を「ラベルを設定した時点の前景色」で決めてしまうので、
     * 前景色を後から変える画面(暗い色を設定する画面)でも読めるよう、色が変わったらHTMLを作り直す。
     */
    private static void syncTextColor(AbstractButton b) {
        Color want = b.isEnabled() ? TEXT : DISABLED_TEXT;
        boolean changed = !want.equals(b.getForeground());
        if (changed)
            b.setForeground(want);
        String text = b.getText();
        if (text != null && BasicHTML.isHTMLString(text)
                && (changed || !want.equals(b.getClientProperty(HTML_COLOR_KEY)))) {
            b.putClientProperty(HTML_COLOR_KEY, want);
            BasicHTML.updateRenderer(b, text);
        }
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintText(g, b, textRect, text);
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect,
            Rectangle iconRect) {
        // 縁取りで状態が分かるので、フォーカスの点線は出さない
    }

    /**
     * 選ばれている状態として、明るい背景色が指定されているか(工房の選択中ボタンなど)
     */
    private static boolean isHighlighted(AbstractButton b) {
        Color bg = b.getBackground();
        if (bg == null || bg.equals(UIManager.getColor("Button.background")))
            return false;
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return bg.getAlpha() > 200 && lum > 110;
    }

    private static Color brighten(Color c, double k) {
        return new Color(Math.min(255, (int) (c.getRed() * k)), Math.min(255, (int) (c.getGreen() * k)),
                Math.min(255, (int) (c.getBlue() * k)), c.getAlpha());
    }

    /**
     * 角を斜めに落とした板
     */
    private static Shape plate(double x, double y, double w, double h, double cut) {
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
