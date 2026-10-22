package com.game.shooting_game.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * ボタンのアニメーション(ホバー・押下の拡大縮小と、スプライトシートのコマ送り)
 * 付けたボタンだけが対象で、付けなければ従来どおり。画像ボタン(createImageBtn)と ShooterButtonUI の文字ボタンのどちらにも付けられる。
 * ボタンの枠からはみ出して描くと切れるので、ふだんを少し小さく(REST)、乗せると枠いっぱい(HOVER)、押すともっと小さく(PRESS)にする。
 * 当たり判定は枠のままで、拡大縮小では変わらない。
 */
public final class ButtonAnimator {
    static final double REST = 0.94;
    static final double HOVER = 1.0;
    static final double PRESS = 0.88;
    private static final String KEY = "buttonAnimator";
    private static final double SPEED = 18; // 目標へ近づく速さ(1/秒)

    private static final Map<ButtonAnimator, Boolean> ALL = new WeakHashMap<>();
    private static Timer timer;

    private final JButton button;
    private final boolean pop;
    private BufferedImage[] frames; // コマ送り(なければ null)
    private double fps;
    private double scale;
    private int lastFrame = -1;

    private ButtonAnimator(JButton button, boolean pop) {
        this.button = button;
        this.pop = pop;
        this.scale = pop ? REST : 1.0;
    }

    /**
     * ホバー・押下で拡大縮小させる
     */
    public static void attachPop(JButton button) {
        attach(new ButtonAnimator(button, true));
    }

    /**
     * ボタンの絵をコマ送りにする(ホバー・押下の拡大縮小も付く)
     *
     * @param frames 各コマ(1枚以上)
     * @param fps    1秒あたりのコマ数
     */
    public static void attachSprite(JButton button, BufferedImage[] frames, double fps) {
        ButtonAnimator a = new ButtonAnimator(button, true);
        a.frames = frames;
        a.fps = fps;
        attach(a);
    }

    private static synchronized void attach(ButtonAnimator a) {
        a.button.putClientProperty(KEY, a);
        a.button.setRolloverEnabled(true);
        ALL.put(a, Boolean.TRUE);
        if (timer == null) {
            timer = new Timer(16, ButtonAnimator::tickAll);
            timer.start();
        }
    }

    private static void tickAll(ActionEvent e) {
        List<ButtonAnimator> snapshot;
        synchronized (ButtonAnimator.class) {
            snapshot = new ArrayList<>(ALL.keySet());
        }
        for (ButtonAnimator a : snapshot)
            a.tick(0.016);
    }

    private void tick(double dt) {
        if (!button.isShowing())
            return;
        boolean dirty = false;
        if (pop) {
            double target = targetScale(button.isEnabled(), button.getModel().isRollover(),
                    button.getModel().isArmed() && button.getModel().isPressed());
            double next = approach(scale, target, dt);
            if (Math.abs(next - scale) > 1e-4)
                dirty = true;
            scale = next;
        }
        if (frames != null) {
            int f = frameAt(System.nanoTime() / 1e9, fps, frames.length);
            if (f != lastFrame) {
                lastFrame = f;
                dirty = true;
            }
        }
        if (dirty)
            button.repaint();
    }

    /** 状態に応じた目標の倍率 */
    static double targetScale(boolean enabled, boolean rollover, boolean pressed) {
        if (!enabled)
            return REST;
        if (pressed)
            return PRESS;
        return rollover ? HOVER : REST;
    }

    /** 現在値を目標へなめらかに近づける(時間に依存し、行き過ぎない) */
    static double approach(double current, double target, double dt) {
        return current + (target - current) * Math.min(1.0, dt * SPEED);
    }

    /** 時刻とコマ数から、いま出すコマの番号 */
    static int frameAt(double seconds, double fps, int count) {
        return count <= 0 ? 0 : (int) (seconds * fps) % count;
    }

    /**
     * 描画の前に呼ぶ。ボタンにアニメーションが付いていれば、中心を基準に拡大縮小した Graphics を返す。付いていなければ null。
     * 返ってきた Graphics で描き、描き終えたら dispose すること。
     */
    public static Graphics2D begin(Graphics g, JComponent c) {
        if (!(c.getClientProperty(KEY) instanceof ButtonAnimator a) || !a.pop)
            return null;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.translate(c.getWidth() / 2.0, c.getHeight() / 2.0);
        g2.scale(a.scale, a.scale);
        g2.translate(-c.getWidth() / 2.0, -c.getHeight() / 2.0);
        return g2;
    }

    /**
     * コマ送りの絵を描く(付いていなければ false で、何もしない)
     */
    public static boolean drawSprite(Graphics g, JComponent c) {
        if (!(c.getClientProperty(KEY) instanceof ButtonAnimator a) || a.frames == null || a.frames.length == 0)
            return false;
        int f = frameAt(System.nanoTime() / 1e9, a.fps, a.frames.length);
        g.drawImage(a.frames[f], 0, 0, c.getWidth(), c.getHeight(), null);
        return true;
    }
}
