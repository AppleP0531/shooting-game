package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.utils.Vector2;

/**
 * 起動時の進捗バー
 * 起動の各段階の終わりで目標の進み具合(0〜1)と状況の文を設定すると、バーはそこへ滑らかに追いつく。
 * サーバー接続のように終わるまでの長さが読めない間は、上限まで時間でゆっくり増えていく。
 */
public final class LoadProgress {
    private static volatile double target = 0; // 目標の進み具合
    private static volatile double creepCap = -1; // 長さが読めない間に、時間で増えていく上限(負なら増えない)
    private static volatile String status = "";

    private static double shown = 0; // 画面に出している進み具合
    private static long lastNanos = 0;

    private LoadProgress() {
    }

    /**
     * 進み具合と状況の文を設定する
     *
     * @param text   状況の文(「...」は付けずに渡す)
     * @param amount 目標の進み具合(0〜1)
     */
    public static void set(String text, double amount) {
        status = text;
        target = Math.clamp(amount, 0, 1);
        creepCap = -1;
    }

    /**
     * 終わるまでの長さが読めない作業の間、上限に向かってゆっくり増やす
     *
     * @param text   状況の文
     * @param amount 今の進み具合
     * @param cap    増えていく上限(作業が終わったら set で次の目標を渡す)
     */
    public static void creep(String text, double amount, double cap) {
        status = text;
        target = Math.clamp(amount, 0, 1);
        creepCap = cap;
    }

    /**
     * 描画(画面の下の中央)
     */
    public static void draw(Graphics g, int width, int height) {
        // 経過時間で滑らかに追いつく
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        double goal = target;
        if (creepCap > goal)
            goal = Math.min(creepCap, Math.max(goal, shown) + 0.05 * dt); // 1秒に5%ずつ、上限まで
        shown += (goal - shown) * Math.min(1, dt * 6);
        if (goal > shown && goal - shown < 0.0005)
            shown = goal;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        int barW = (int) (width * 0.6);
        int barH = Math.max(4, (int) (6 * adj));
        int x = (width - barW) / 2;
        int y = height - (int) (45 * adj);

        // レール
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(x, y, barW, barH, barH, barH);
        // 進んだ分(うっすらした光と芯)
        int fillW = (int) (barW * Math.clamp(shown, 0, 1));
        if (fillW > 0) {
            g2.setColor(new Color(70, 215, 255, 50));
            g2.fillRoundRect(x - 2, y - 2, fillW + 4, barH + 4, barH + 4, barH + 4);
            g2.setColor(new Color(70, 215, 255, 230));
            g2.fillRoundRect(x, y, fillW, barH, barH, barH);
        }
        g2.setStroke(new BasicStroke(1f));
        g2.dispose();

        // 状況の文(バーの上)とパーセント(バーの右上)
        double size = 14 * adj;
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.BOTTOM), new Vector2(x, y - 6 * adj), size, Color.WHITE,
                Font.BOLD, status + "...");
        UIBuilder.drawString(g, new Anchor(H.RIGHT, V.BOTTOM), new Vector2(x + barW, y - 6 * adj), size,
                new Color(150, 230, 255), Font.BOLD, (int) Math.round(shown * 100) + "%");
    }
}
