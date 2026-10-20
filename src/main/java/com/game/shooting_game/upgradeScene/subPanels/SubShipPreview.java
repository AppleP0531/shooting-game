package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Path2D;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.domain.subship.SubShipService;

/**
 * サブ機のプレビュー
 * 左に「いまのサブ機」、右に「選んでいるパーツを制作・強化した後のサブ機」を、自機の両側に付いた形で並べて動かして見せる。
 */
public class SubShipPreview extends JPanel {
    private static final String[] KINDS = { "HULL", "CANNON", "ENGINE", "CORE", "EMITTER" };

    private String selectedKind;
    private double time = 0;
    private final Timer timer;

    public SubShipPreview() {
        setOpaque(false);
        setLayout(null);

        // 表示されている間だけ動かす
        timer = new Timer(33, e -> {
            time += 0.033;
            repaint();
        });
        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing())
                    timer.start();
                else
                    timer.stop();
            }
        });
    }

    /**
     * 強化候補のパーツの設定
     */
    public void setSelectedKind(String kind) {
        this.selectedKind = kind;
        repaint();
    }

    /**
     * 今のMkに対して、選択中のパーツを1段階上げたMkの配列を返す(上げられなければ今のまま)
     */
    private int[] tiers(boolean after) {
        int[] t = new int[KINDS.length];
        for (int i = 0; i < KINDS.length; i++) {
            t[i] = "HULL".equals(KINDS[i]) ? SubShipService.hullTier() : SubShipService.getTier(KINDS[i]);
            if (after && KINDS[i].equals(selectedKind) && SubShipService.getTier(KINDS[i]) < SubShipService.maxTier(KINDS[i]))
                t[i] = SubShipService.getTier(KINDS[i]) + 1; // 制作後は、新しく作ったMkを使う
        }
        return t;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        int w = getWidth();
        int h = getHeight();
        // 背景
        g2.setColor(new Color(255, 255, 255, 14));
        g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
        g2.setColor(new Color(120, 200, 255, 70));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

        boolean canUpgrade = selectedKind != null
                && SubShipService.getTier(selectedKind) < SubShipService.maxTier(selectedKind);

        drawHalf(g2, 0, w / 2, h, "いま", tiers(false), 1f);
        if (canUpgrade) {
            drawHalf(g2, w / 2, w, h, "制作後", tiers(true), 1f);
            // 矢印
            g2.setColor(new Color(255, 255, 255, 170));
            g2.setFont(new Font("SansSerif", Font.BOLD, (int) (18 * adj)));
            g2.drawString("▶", w / 2 - (int) (8 * adj), h / 2 + (int) (6 * adj));
        } else {
            g2.setFont(new Font("SansSerif", Font.PLAIN, (int) (12 * adj)));
            g2.setColor(new Color(220, 220, 220));
            String s = "最大まで強化済み";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, w * 3 / 4 - fm.stringWidth(s) / 2, h / 2 + fm.getAscent() / 2);
        }
        g2.dispose();
    }

    /**
     * 左右一組のサブ機と自機の影を描く
     */
    private void drawHalf(Graphics2D g2, int x0, int x1, int h, String label, int[] t, float alpha) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        double cx = (x0 + x1) / 2.0;
        double cy = h * 0.58;

        // ラベル
        g2.setFont(new Font("SansSerif", Font.BOLD, (int) (11 * adj)));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 230, 255));
        g2.drawString(label, (int) (cx - fm.stringWidth(label) / 2.0), (int) (14 * adj));

        // 自機の影(両側に付く位置の目安)
        g2.setColor(new Color(160, 160, 170, 90));
        Path2D.Double body = new Path2D.Double();
        double s = 20 * adj;
        body.moveTo(cx, cy - s);
        body.lineTo(cx + s * 0.8, cy + s * 0.7);
        body.lineTo(cx, cy + s * 0.4);
        body.lineTo(cx - s * 0.8, cy + s * 0.7);
        body.closePath();
        g2.fill(body);

        // サブ機(フレームが大きいほど外側に付く)
        double off = (24 + 6 * SubShipService.hullSize(Math.max(1, t[0]))) * adj * 0.8;
        double y = cy + 8 * adj;
        // 完成していない(必須パーツが足りない)ときも、制作済みの分は表示する
        for (boolean left : new boolean[] { true, false })
            com.game.shooting_game.domain.subship.SubShipRenderer.draw(g2,
                    left ? cx - off : cx + off, y, left,
                    t[0], t[1], t[2], t[3], t[4], ColorMode.BLUE, time, alpha, 0.45);
    }
}
