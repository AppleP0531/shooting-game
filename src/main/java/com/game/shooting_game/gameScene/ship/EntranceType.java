package com.game.shooting_game.gameScene.ship;

import com.game.shooting_game.utils.Vector2;

/**
 * 入場軌道の種類
 * 召喚位置(from)から待機位置(to)までの経路を、進行度t(0~1)から計算する
 */
public enum EntranceType {
    LINEAR, // 直線(従来動作)
    ARC_LEFT, // 画面左側に膨らむ弧
    ARC_RIGHT, // 画面右側に膨らむ弧
    SPIRAL, // 振れ幅が小さくなっていく蛇行(らせん)
    ZIGZAG, // ジグザグ
    SWOOP, // 大きなS字を描いて滑り込む
    LOOP_IN, // 宙返り(ループ)をしながら入ってくる
    BOUNCE; // 待機位置を行き過ぎてから戻る

    /**
     * 直線距離に対する経路長の概算倍率(入場にかける時間の計算用)
     * 
     * @return 倍率
     */
    public double lengthFactor() {
        return switch (this) {
            case LINEAR -> 1.0;
            case ARC_LEFT, ARC_RIGHT -> 1.25;
            case SPIRAL -> 1.5;
            case ZIGZAG -> 1.4;
            case SWOOP -> 1.6;
            case LOOP_IN -> 1.9;
            case BOUNCE -> 1.2;
        };
    }

    /**
     * 進行度に対する位置の計算
     * 始点と終点では必ずfrom/toに一致する
     * 
     * @param from 召喚位置
     * @param to   待機位置
     * @param t    進行度(0~1)
     * @return 位置
     */
    public Vector2 evaluate(Vector2 from, Vector2 to, double t) {
        t = Math.max(0, Math.min(1, t));

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6)
            return new Vector2(to);

        // 進行方向に対する垂直単位ベクトル(下に進むとき画面左向き)
        double px = -dy / dist;
        double py = dx / dist;

        // 直線以外は待機位置に近づくほど減速させる
        double u = (this == LINEAR) ? t : 1 - (1 - t) * (1 - t);

        // 進行方向に対する前後のずれ量(ループ・行き過ぎ用。u=0,1で必ず0)
        double along = 0;
        if (this == LOOP_IN) {
            // 周回しながら進む曲線(トロコイド)で1回転する
            along = -0.2 * dist * Math.sin(Math.PI * 2 * u);
        } else if (this == BOUNCE) {
            // 待機位置を少し行き過ぎて戻る(easeOutBack)
            double c1 = 1.70158;
            double s = t - 1;
            double back = 1 + (c1 + 1) * s * s * s + c1 * s * s;
            u = back;
        }

        // 進行方向に対する横ずれ量(u=0,1で必ず0になる)
        double offset = switch (this) {
            case LINEAR -> 0;
            case ARC_LEFT -> Math.sin(Math.PI * u) * dist * 0.3;
            case ARC_RIGHT -> -Math.sin(Math.PI * u) * dist * 0.3;
            case SPIRAL -> Math.sin(Math.PI * 3 * u) * (1 - u) * dist * 0.28;
            case ZIGZAG -> triangle(u * 3) * (1 - u) * dist * 0.22;
            case SWOOP -> Math.sin(Math.PI * 2 * u) * dist * 0.3;
            case LOOP_IN -> 0.2 * dist * (1 - Math.cos(Math.PI * 2 * u));
            case BOUNCE -> 0;
        };

        return new Vector2(
                from.getX() + dx * u + px * offset + (dx / dist) * along,
                from.getY() + dy * u + py * offset + (dy / dist) * along);
    }

    /**
     * 三角波(周期1、-1~1、x=0で0)
     */
    private static double triangle(double x) {
        double frac = (x + 0.25) - Math.floor(x + 0.25);
        return 4 * Math.abs(frac - 0.5) - 1;
    }
}
