package com.game.shooting_game.gameScene.ship;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ボスの種類ごとの見た目(機体の形・色・ゲージを壊すたびに増える部品)
 *
 * 座標はボスの中心を原点とし、機体の半分の大きさを1とする。ボスは下向きで、機首は +y。
 * 部品は、ゲージを壊して形態が進むたびに増える(minForm 以上の形態で表示される)。
 * 新しい形態に変わる間は、部品が根もと(ax, ay)からせり出して見える。
 */
public final class BossLook {
    /**
     * 部品1つ
     *
     * @param minForm この形態(0始まり)から表示する
     * @param front   機体の前に描くか(falseなら機体の後ろ)
     * @param pts     頂点
     * @param ax      せり出すときの根もとのx
     * @param ay      せり出すときの根もとのy
     */
    public record Part(int minForm, boolean front, double[][] pts, double ax, double ay) {
    }

    private final Color accent;
    private final double[][] hull;
    private final List<Part> parts;

    private BossLook(Color accent, double[][] hull, List<Part> parts) {
        this.accent = accent;
        this.hull = hull;
        this.parts = parts;
    }

    public Color accent() {
        return accent;
    }

    public double[][] hull() {
        return hull;
    }

    public List<Part> parts() {
        return parts;
    }

    // ----- 見た目の登録 -----
    private static final Map<String, BossLook> LOOKS = new HashMap<>();

    /**
     * ボスIDに合った見た目(未登録ならBoss1と同じ)
     */
    public static BossLook of(String bossId) {
        return LOOKS.getOrDefault(bossId, LOOKS.get("Boss1"));
    }

    // 部品を作る道具
    private static Part part(int form, boolean front, double ax, double ay, double[]... pts) {
        return new Part(form, front, pts, ax, ay);
    }

    /** 左右対称の部品を2つ作る(x が正の側を与える) */
    private static void pair(List<Part> out, int form, boolean front, double ax, double ay, double[]... right) {
        out.add(new Part(form, front, right, ax, ay));
        double[][] left = new double[right.length][];
        for (int i = 0; i < right.length; i++)
            left[i] = new double[] { -right[i][0], right[i][1] };
        out.add(new Part(form, front, left, -ax, ay));
    }

    private static double[][] ngon(double cx, double cy, double r, int n) {
        double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + Math.PI / n;
            p[i] = new double[] { cx + Math.cos(a) * r, cy + Math.sin(a) * r };
        }
        return p;
    }

    private static double[] pt(double x, double y) {
        return new double[] { x, y };
    }

    static {
        // Boss1「レイダー」: V字の機体。肩の双砲 → 翼と主砲 → 背のトゲ → 砲台ポッド
        List<Part> raider = new ArrayList<>();
        pair(raider, 1, true, 0.5, 0.0, pt(0.41, 0), pt(0.59, 0), pt(0.57, 0.95), pt(0.43, 0.95));
        pair(raider, 1, true, 0.5, 0.0, pt(0.38, 0.85), pt(0.62, 0.85), pt(0.62, 1.02), pt(0.38, 1.02));
        pair(raider, 2, false, 0.95, 0.0, pt(0.95, -0.15), pt(1.5, 0.4), pt(1.12, 0.5), pt(0.8, 0.1));
        raider.add(part(2, true, 0, 0.3, pt(-0.11, 0.3), pt(0.11, 0.3), pt(0.08, 1.3), pt(-0.08, 1.3)));
        raider.add(part(2, true, 0, 0.3, pt(-0.16, 1.15), pt(0.16, 1.15), pt(0.16, 1.34), pt(-0.16, 1.34)));
        for (double x : new double[] { -0.65, -0.22, 0.22, 0.65 })
            raider.add(part(3, false, x, -0.5, pt(x - 0.12, -0.5), pt(x, -0.5 - 0.5 * (1 - Math.abs(x) * 0.5)),
                    pt(x + 0.12, -0.5)));
        pair(raider, 4, false, 1.32, -0.3, pt(1.26, -0.3), pt(1.38, -0.3), pt(1.38, 0.15), pt(1.26, 0.15));
        raider.add(part(4, false, 1.32, -0.35, ngon(1.32, -0.35, 0.2, 8)));
        raider.add(part(4, false, -1.32, -0.35, ngon(-1.32, -0.35, 0.2, 8)));
        LOOKS.put("Boss1", new BossLook(new Color(255, 96, 96), new double[][] { pt(0, 0.95), pt(-1.0, -0.1),
                pt(-0.85, -0.6), pt(-0.35, -0.35), pt(0, -0.55), pt(0.35, -0.35), pt(0.85, -0.6), pt(1.0, -0.1) },
                raider));

        // Boss2「マンタ」: 平たく広い機体。長いヒレ → 前の刃 → 尾のトゲ → 翼端ポッド
        List<Part> manta = new ArrayList<>();
        pair(manta, 1, false, 1.0, -0.2, pt(1.0, -0.2), pt(1.55, -0.75), pt(1.25, -0.1));
        pair(manta, 1, false, 1.0, -0.1, pt(1.05, 0.1), pt(1.6, 0.3), pt(1.2, 0.35));
        manta.add(part(2, true, 0, 0.4, pt(-0.13, 0.4), pt(0.13, 0.4), pt(0, 1.4)));
        pair(manta, 2, true, 0.35, 0.3, pt(0.3, 0.3), pt(0.42, 0.3), pt(0.5, 1.0), pt(0.4, 1.0));
        pair(manta, 3, false, 0.3, -0.4, pt(0.15, -0.4), pt(0.45, -1.05), pt(0.5, -0.4));
        pair(manta, 4, false, 1.3, 0.1, pt(1.25, 0.1), pt(1.35, 0.1), pt(1.35, 0.7), pt(1.25, 0.7));
        manta.add(part(4, false, 1.3, 0.1, ngon(1.3, 0.1, 0.18, 8)));
        manta.add(part(4, false, -1.3, 0.1, ngon(-1.3, 0.1, 0.18, 8)));
        LOOKS.put("Boss2", new BossLook(new Color(70, 220, 255), new double[][] { pt(0, 0.75), pt(-1.25, -0.05),
                pt(-0.9, -0.5), pt(-0.3, -0.3), pt(0, -0.45), pt(0.3, -0.3), pt(0.9, -0.5), pt(1.25, -0.05) },
                manta));

        // Boss3「フォートレス」: 角ばった要塞。前の砲塔 → 肩の装甲 → 三連砲 → 側面ミサイル
        List<Part> fort = new ArrayList<>();
        pair(fort, 1, true, 0.65, 0.7, pt(0.5, 0.7), pt(0.8, 0.7), pt(0.8, 1.15), pt(0.5, 1.15));
        pair(fort, 2, false, 1.05, 0.0, pt(1.0, -0.55), pt(1.45, -0.45), pt(1.45, 0.5), pt(1.0, 0.4));
        for (double x : new double[] { -0.25, 0, 0.25 })
            fort.add(part(3, true, x, 0.6, pt(x - 0.07, 0.6), pt(x + 0.07, 0.6), pt(x + 0.06, 1.35),
                    pt(x - 0.06, 1.35)));
        for (double y : new double[] { -0.3, 0.0, 0.3 }) {
            pair(fort, 4, true, 1.1, y, pt(1.1, y - 0.07), pt(1.3, y - 0.07), pt(1.3, y + 0.07), pt(1.1, y + 0.07));
        }
        LOOKS.put("Boss3", new BossLook(new Color(255, 170, 60), new double[][] { pt(-0.7, 0.8), pt(0.7, 0.8),
                pt(1.05, 0.2), pt(0.9, -0.6), pt(-0.9, -0.6), pt(-1.05, 0.2) }, fort));

        // Boss4「ダガー」: 細長い刃。鼻先の双刃 → 後退翼 → 尾びれ → 翼端の針
        List<Part> dagger = new ArrayList<>();
        pair(dagger, 1, true, 0.2, 0.5, pt(0.2, 0.5), pt(0.32, 1.4), pt(0.08, 0.95));
        pair(dagger, 2, false, 0.5, 0.0, pt(0.5, 0.0), pt(1.55, -0.65), pt(0.7, -0.5));
        dagger.add(part(3, false, 0, -0.5, pt(-0.12, -0.5), pt(0, -1.3), pt(0.12, -0.5)));
        pair(dagger, 4, false, 1.5, -0.6, pt(1.45, -0.6), pt(1.75, 0.55), pt(1.3, -0.45));
        LOOKS.put("Boss4", new BossLook(new Color(120, 255, 150), new double[][] { pt(0, 1.2), pt(-0.5, 0.2),
                pt(-0.65, -0.5), pt(-0.2, -0.3), pt(0, -0.7), pt(0.2, -0.3), pt(0.65, -0.5), pt(0.5, 0.2) },
                dagger));

        // Boss5「ハイヴ」: 丸い巣。前のアンテナ → 弧の外殻 → 四隅のポッド → 頭の冠
        List<Part> hive = new ArrayList<>();
        pair(hive, 1, true, 0.4, 0.8, pt(0.3, 0.8), pt(0.5, 0.8), pt(0.42, 1.25));
        pair(hive, 2, false, 1.0, 0.0, pt(0.95, -0.7), pt(1.4, -0.3), pt(1.4, 0.4), pt(0.95, 0.8), pt(1.05, 0.0));
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2)
                hive.add(part(3, false, 1.1 * sx, 0.55 * sy, ngon(1.15 * sx, 0.55 * sy, 0.22, 8)));
        for (int i = -2; i <= 2; i++)
            hive.add(part(4, false, i * 0.3, -0.8, pt(i * 0.3 - 0.1, -0.8), pt(i * 0.3, -1.3 + Math.abs(i) * 0.12),
                    pt(i * 0.3 + 0.1, -0.8)));
        LOOKS.put("Boss5", new BossLook(new Color(196, 120, 255), ngon(0, 0.05, 0.9, 12), hive));

        // Boss6「ドレッド」: 王冠のような大型機。双砲 → 翼と主砲 → 二本の角 → 全砲塔
        List<Part> dread = new ArrayList<>();
        pair(dread, 1, true, 0.5, 0.2, pt(0.4, 0.2), pt(0.6, 0.2), pt(0.58, 1.15), pt(0.42, 1.15));
        pair(dread, 2, false, 1.05, 0.0, pt(1.05, -0.2), pt(1.65, 0.45), pt(1.2, 0.55), pt(0.85, 0.15));
        dread.add(part(2, true, 0, 0.4, pt(-0.13, 0.4), pt(0.13, 0.4), pt(0.1, 1.4), pt(-0.1, 1.4)));
        pair(dread, 3, false, 0.35, -0.8, pt(0.25, -0.8), pt(0.55, -1.5), pt(0.5, -0.7));
        pair(dread, 4, true, 1.3, 0.0, pt(1.22, 0.0), pt(1.38, 0.0), pt(1.36, 0.85), pt(1.24, 0.85));
        dread.add(part(4, false, 1.3, 0.0, ngon(1.3, 0.0, 0.2, 8)));
        dread.add(part(4, false, -1.3, 0.0, ngon(-1.3, 0.0, 0.2, 8)));
        LOOKS.put("Boss6", new BossLook(new Color(255, 226, 110), new double[][] { pt(0, 1.0), pt(-1.1, 0.3),
                pt(-1.0, -0.6), pt(-0.6, -0.4), pt(-0.4, -0.9), pt(0, -0.55), pt(0.4, -0.9), pt(0.6, -0.4),
                pt(1.0, -0.6), pt(1.1, 0.3) }, dread));

        // BossCoin「コインボス」: 金色の硬貨の形。小さな翼 → 前の砲 → 縁のトゲ
        List<Part> coin = new ArrayList<>();
        pair(coin, 1, false, 0.9, 0.0, pt(0.85, -0.2), pt(1.35, 0.15), pt(0.9, 0.4));
        pair(coin, 2, true, 0.35, 0.7, pt(0.28, 0.7), pt(0.42, 0.7), pt(0.4, 1.2), pt(0.3, 1.2));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 - Math.PI / 2;
            double x = Math.cos(a) * 0.9;
            double y = Math.sin(a) * 0.9;
            coin.add(part(3, false, x, y, pt(x - Math.sin(a) * 0.1, y + Math.cos(a) * 0.1),
                    pt(Math.cos(a) * 1.25, Math.sin(a) * 1.25), pt(x + Math.sin(a) * 0.1, y - Math.cos(a) * 0.1)));
        }
        LOOKS.put("BossCoin", new BossLook(new Color(255, 205, 60), ngon(0, 0, 0.92, 16), coin));
    }
}
