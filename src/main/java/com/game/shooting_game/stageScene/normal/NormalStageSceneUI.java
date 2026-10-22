package com.game.shooting_game.stageScene.normal;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.ui.SceneBars;

public class NormalStageSceneUI {
    /** 背景の星(位置は 0〜1 の割合。層が奥ほど、ゆっくり流れる) */
    private record Star(double x, double y, int layer, double size, double phase) {
    }

    private static final int STAR_COUNT = 110;
    private static final double[] LAYER_SPEED = { 0.08, 0.2, 0.45 }; // スクロール量に対する流れる割合
    private final Star[] stars = new Star[STAR_COUNT];

    public NormalStageSceneUI() {
        Random rnd = new Random(7); // 毎回同じ並びにする
        for (int i = 0; i < STAR_COUNT; i++)
            stars[i] = new Star(rnd.nextDouble(), rnd.nextDouble(), rnd.nextInt(LAYER_SPEED.length),
                    0.8 + rnd.nextDouble() * 1.6, rnd.nextDouble() * Math.PI * 2);
    }

    /**
     * 描画(暗い紺の宇宙に、スクロールに合わせて流れる星と、上下の帯)
     *
     * @param scroll ステージの台のスクロール量(基準サイズ)
     */
    public void draw(Graphics g, double scroll) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int w = ShootingGameMain.getInstance().getWidth();
        int h = ShootingGameMain.getInstance().getHeight();
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setPaint(new GradientPaint(0, 0, new Color(6, 10, 30), 0, h, new Color(26, 14, 52)));
        g2.fillRect(0, 0, w, h);

        double time = System.nanoTime() / 1e9;
        for (Star s : stars) {
            // 台と同じ向きに、奥の層ほどゆっくり動かす(画面の外へ出たら反対側から)
            double y = ((s.y * h + scroll * LAYER_SPEED[s.layer]) % h + h) % h;
            double twinkle = 0.55 + 0.45 * Math.sin(time * 1.5 + s.phase);
            int a = (int) (90 + 120 * twinkle * (s.layer + 1) / LAYER_SPEED.length);
            g2.setColor(new Color(200, 225, 255, Math.min(255, a)));
            double r = s.size * (0.7 + 0.3 * s.layer) * adj;
            g2.fill(new java.awt.geom.Ellipse2D.Double(s.x * w - r / 2, y - r / 2, r, r));
        }

        SceneBars.draw(g2, SceneBars.TOP, 56);
        g2.dispose();
    }
}
