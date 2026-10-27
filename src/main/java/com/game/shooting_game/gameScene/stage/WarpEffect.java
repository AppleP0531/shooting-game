package com.game.shooting_game.gameScene.stage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

/**
 * 無限モードの自動スキップの演出(爆速で飛んでいく見た目)
 * 画面の奥から手前へ流れる光の筋(スピードライン)、ウェーブの数字、進み具合の帯を出す。
 * スキップが終わった瞬間には、白い閃光を一瞬出して、戦いの始まりを知らせる。
 */
public final class WarpEffect {
    private static final int STREAKS = 90;
    private static final double FLASH_SECONDS = 0.45;

    /** 光の筋(位置は画面に対する割合) */
    private static final class Streak {
        double x, y, speed, length;
        int shade;
    }

    private final Streak[] streaks = new Streak[STREAKS];
    private final Random rnd = new Random(11);
    private long lastNanos;
    private boolean wasSkipping;
    private double flash; // 閃光の残り時間(秒)

    public WarpEffect() {
        for (int i = 0; i < STREAKS; i++) {
            streaks[i] = new Streak();
            respawn(streaks[i], true);
        }
    }

    private void respawn(Streak s, boolean anywhere) {
        s.x = rnd.nextDouble();
        s.y = anywhere ? rnd.nextDouble() : -0.2 * rnd.nextDouble();
        s.speed = 1.6 + rnd.nextDouble() * 2.6; // 1秒に画面の高さの1.6〜4.2倍(奥にあるほど遅い)
        s.length = 0.08 + s.speed * 0.05;
        s.shade = rnd.nextInt(3);
    }

    /**
     * 描画する(スキップ中でも、直後の閃光でも、毎フレーム呼ぶ)
     *
     * @param skipping いまスキップ中か
     * @param wave     いま飛んでいるウェーブ
     * @param target   飛ぶ先のウェーブ(これを超えたら、戦い始める)
     */
    public void draw(Graphics g, int width, int height, boolean skipping, int wave, int target) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        if (wasSkipping && !skipping)
            flash = FLASH_SECONDS; // 終わった瞬間に閃光
        wasSkipping = skipping;
        flash = Math.max(0, flash - dt);
        if (!skipping && flash <= 0)
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (skipping) {
            // 中心へ向かって暗くなる、青みがかった背景(速さの中にいる感じ)
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 10, 40, 140), 0, height, new Color(0, 30, 70, 90)));
            g2.fillRect(0, 0, width, height);

            // スピードライン(下へ流れ、画面の下を出たら上から出し直す。速いほど長く明るい)
            for (Streak s : streaks) {
                s.y += s.speed * dt;
                if (s.y - s.length > 1)
                    respawn(s, false);
                double x = s.x * width, y0 = (s.y - s.length) * height, y1 = s.y * height;
                int a = (int) Math.min(255, 80 + s.speed * 45);
                Color c = s.shade == 0 ? new Color(255, 255, 255, a)
                        : s.shade == 1 ? new Color(150, 220, 255, a) : new Color(190, 170, 255, a);
                g2.setStroke(new BasicStroke((float) (1 + s.speed * 0.45), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(c);
                g2.drawLine((int) x, (int) y0, (int) x, (int) y1);
            }

            // 数字(ウェーブが速く増えていく)と進み具合の帯
            double ratio = target <= 1 ? 1 : Math.clamp((double) wave / (target - 1), 0.0, 1.0);
            int unit = Math.max(1, width / 20);
            g2.setFont(new Font("SansSerif", Font.BOLD, unit * 3));
            FontMetrics fm = g2.getFontMetrics();
            String number = "WAVE " + wave;
            int tx = (width - fm.stringWidth(number)) / 2, ty = (int) (height * 0.42);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(number, tx + 2, ty + 2);
            g2.setColor(Color.WHITE);
            g2.drawString(number, tx, ty);

            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, unit)));
            fm = g2.getFontMetrics();
            String sub = "WARP  >>>  " + Math.max(1, target - 1);
            g2.setColor(new Color(150, 220, 255));
            g2.drawString(sub, (width - fm.stringWidth(sub)) / 2, ty + fm.getHeight() + unit / 2);

            int barW = (int) (width * 0.7), barH = Math.max(4, unit / 2);
            int bx = (width - barW) / 2, by = (int) (height * 0.55);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(bx, by, barW, barH, barH, barH);
            g2.setPaint(new GradientPaint(bx, 0, new Color(80, 200, 255), bx + barW, 0, new Color(255, 255, 255)));
            g2.fillRoundRect(bx, by, (int) (barW * ratio), barH, barH, barH);
        }

        // 終わった瞬間の閃光(白から透明へ)
        if (flash > 0) {
            int a = (int) (255 * Math.pow(flash / FLASH_SECONDS, 1.5));
            g2.setColor(new Color(255, 255, 255, Math.clamp(a, 0, 255)));
            g2.fillRect(0, 0, width, height);
        }
        g2.dispose();
    }

    /** スキップ全体にかける時間の目安(秒)。飛ぶウェーブが多くても、この時間に収める */
    static final double MAX_SKIP_SECONDS = 3.5;

    /**
     * 1ウェーブ飛ばすのにかける時間(秒)。飛ぶ数が少なければ、ゆっくり目(上限 maxStep)、多ければ、全体が MAX_SKIP_SECONDS に収まるよう速くする。
     *
     * @param waves   飛ぶウェーブの数
     * @param maxStep 1ウェーブあたりの上限(秒)
     */
    static double stepSeconds(int waves, double maxStep) {
        return Math.min(maxStep, MAX_SKIP_SECONDS / Math.max(1, waves));
    }
}
