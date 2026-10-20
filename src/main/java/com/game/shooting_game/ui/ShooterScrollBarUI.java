package com.game.shooting_game.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.game.shooting_game.ShootingGameMain;

/**
 * シューティングゲーム風のスクロールバーの見た目
 * 矢印ボタンのない細いバーで、暗いレールの上をネオンの水色のつまみが動く。マウスを乗せる・つかむと明るくなる。
 */
public class ShooterScrollBarUI extends BasicScrollBarUI {
    private static final Color TRACK = new Color(255, 255, 255, 18);
    private static final Color THUMB = new Color(70, 215, 255, 150);
    private static final Color THUMB_HOVER = new Color(120, 230, 255, 215);
    private static final Color THUMB_DRAG = new Color(190, 245, 255, 255);

    /** UIManager が呼ぶ */
    public static ComponentUI createUI(JComponent c) {
        return new ShooterScrollBarUI();
    }

    /**
     * すべてのスクロールバーをこの見た目にする(画面を作る前に呼ぶ)
     */
    public static void install() {
        UIManager.put("ScrollBarUI", ShooterScrollBarUI.class.getName());
    }

    private static int thickness() {
        return Math.max(8, (int) (8 * ShootingGameMain.getScreenSizeAdj()));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(thickness(), thickness());
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private static JButton emptyButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(TRACK);
        int inset = Math.max(1, r.width / 4);
        g2.fillRoundRect(r.x + inset, r.y, r.width - 2 * inset, r.height, r.width, r.width);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled())
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isDragging ? THUMB_DRAG : isThumbRollover() ? THUMB_HOVER : THUMB);
        int inset = Math.max(1, r.width / 4);
        g2.fillRoundRect(r.x + inset, r.y + 1, r.width - 2 * inset, r.height - 2, r.width, r.width);
        g2.dispose();
    }
}
