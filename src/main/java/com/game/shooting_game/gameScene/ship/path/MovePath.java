package com.game.shooting_game.gameScene.ship.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import com.game.shooting_game.utils.Vector2;

/**
 * 通過点を滑らかな曲線(Catmull-Rom)でつないだ移動経路
 * 経路上の距離から位置と進行方向を取得できる(一定速度で動かすため)
 */
public class MovePath {
    private static final int STEPS = 16; // 通過点間の分割数

    private final List<Vector2> samples = new ArrayList<>(); // 曲線上のサンプル点
    private final double[] cumulative; // サンプル点までの累積距離
    @Getter
    private final double length; // 経路の全長

    /**
     * @param points 通過点(画面座標)
     */
    public MovePath(List<Vector2> points) {
        int n = points.size();
        if (n < 2)
            throw new IllegalArgumentException("経路には2点以上の通過点が必要です");

        for (int i = 0; i < n - 1; i++) {
            Vector2 p0 = points.get(Math.max(0, i - 1));
            Vector2 p1 = points.get(i);
            Vector2 p2 = points.get(i + 1);
            Vector2 p3 = points.get(Math.min(n - 1, i + 2));

            for (int s = 0; s < STEPS; s++)
                samples.add(catmullRom(p0, p1, p2, p3, s / (double) STEPS));
        }
        samples.add(new Vector2(points.get(n - 1)));

        cumulative = new double[samples.size()];
        for (int i = 1; i < samples.size(); i++) {
            cumulative[i] = cumulative[i - 1] + Math.hypot(
                    samples.get(i).getX() - samples.get(i - 1).getX(),
                    samples.get(i).getY() - samples.get(i - 1).getY());
        }
        length = cumulative[cumulative.length - 1];
    }

    /**
     * 経路上の距離に対する位置
     * 
     * @param dist 始点からの距離
     * @return 位置
     */
    public Vector2 positionAt(double dist) {
        int i = segmentIndex(dist);
        double segLen = cumulative[i + 1] - cumulative[i];
        double r = segLen < 1e-9 ? 0 : (clamp(dist) - cumulative[i]) / segLen;

        Vector2 a = samples.get(i);
        Vector2 b = samples.get(i + 1);
        return new Vector2(
                a.getX() + (b.getX() - a.getX()) * r,
                a.getY() + (b.getY() - a.getY()) * r);
    }

    /**
     * 経路上の距離に対する進行方向
     * 
     * @param dist 始点からの距離
     * @return 進行方向(ラジアン、右が0で下がPI/2)
     */
    public double headingAt(double dist) {
        int i = segmentIndex(dist);
        Vector2 a = samples.get(i);
        Vector2 b = samples.get(i + 1);
        return Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
    }

    /**
     * 始点
     * 
     * @return 始点
     */
    public Vector2 start() {
        return new Vector2(samples.get(0));
    }

    private double clamp(double dist) {
        return Math.max(0, Math.min(length, dist));
    }

    /**
     * 距離が含まれる区間の番号(samples[i]~samples[i+1])
     */
    private int segmentIndex(double dist) {
        double d = clamp(dist);
        int idx = Arrays.binarySearch(cumulative, d);
        if (idx < 0)
            idx = -idx - 2; // 挿入位置の1つ手前が区間の始点
        return Math.max(0, Math.min(samples.size() - 2, idx));
    }

    private static Vector2 catmullRom(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * ((2 * p1.getX())
                + (-p0.getX() + p2.getX()) * t
                + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2
                + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);
        double y = 0.5 * ((2 * p1.getY())
                + (-p0.getY() + p2.getY()) * t
                + (2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t2
                + (-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t3);
        return new Vector2(x, y);
    }
}
