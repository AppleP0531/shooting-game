package com.game.shooting_game.upgradeScene.subPanels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JPanel;

import com.game.shooting_game.domain.subship.SubShipTypeService;
import com.game.shooting_game.domain.subship.SubShipTypeService.TypeDef;

/**
 * サブ機のタイプの並び(◇が並び、見ているタイプが塗りつぶされる)
 * 使用中のタイプは金色の縁、未解放のタイプは暗い灰色で、何番目のサブ機かがひと目で分かる。
 */
public class TypeDots extends JPanel {
    private TypeDef viewing;

    public TypeDots() {
        setOpaque(false);
    }

    public void setViewing(TypeDef viewing) {
        this.viewing = viewing;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        var types = SubShipTypeService.types();
        int n = types.size();
        double d = Math.min(getHeight() - 2, 14); // ◇の大きさ
        double gap = d * 1.9;
        double x0 = getWidth() / 2.0 - (n - 1) * gap / 2;
        double cy = getHeight() / 2.0;
        TypeDef current = SubShipTypeService.current();
        for (int i = 0; i < n; i++) {
            TypeDef t = types.get(i);
            double cx = x0 + i * gap;
            Path2D.Double dia = new Path2D.Double();
            dia.moveTo(cx, cy - d / 2);
            dia.lineTo(cx + d / 2, cy);
            dia.lineTo(cx, cy + d / 2);
            dia.lineTo(cx - d / 2, cy);
            dia.closePath();
            boolean own = SubShipTypeService.isUnlocked(t);
            Color line = t.equals(current) ? new Color(255, 205, 70) : own ? new Color(70, 215, 255) : new Color(90, 100, 115);
            if (t.equals(viewing)) {
                g2.setColor(line);
                g2.fill(dia);
            }
            g2.setColor(line);
            g2.setStroke(new java.awt.BasicStroke(t.equals(current) ? 2.4f : 1.6f));
            g2.draw(dia);
        }
        g2.dispose();
    }
}
