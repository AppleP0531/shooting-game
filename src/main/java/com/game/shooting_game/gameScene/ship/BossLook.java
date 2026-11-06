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

    /** 先がとがる曲がったヒレ(二次ベジェの中心線に沿って幅が細くなる) */
    private static double[][] blade(double x0, double y0, double cx, double cy, double x1, double y1, double w) {
        int n = 8;
        double[][] out = new double[n * 2][];
        for (int i = 0; i < n; i++) {
            double t = i / (n - 1.0);
            double u = 1 - t;
            double x = u * u * x0 + 2 * u * t * cx + t * t * x1;
            double y = u * u * y0 + 2 * u * t * cy + t * t * y1;
            double dx = 2 * u * (cx - x0) + 2 * t * (x1 - cx);
            double dy = 2 * u * (cy - y0) + 2 * t * (y1 - cy);
            double len = Math.max(1e-6, Math.hypot(dx, dy));
            double hw = w * Math.sin(Math.PI * Math.min(1, 0.35 + 0.65 * (1 - t)) / 1.0) * (1 - t * 0.9);
            out[i] = pt(x - dy / len * hw, y + dx / len * hw);
            out[n * 2 - 1 - i] = pt(x + dy / len * hw, y - dx / len * hw);
        }
        return out;
    }

    /** 追加ボス(Boss10〜19)の見た目。画像がなくても、ボスごとに違う形と色で描ける */
    private static void registerExtra() {
        // Boss10「ウォール」: 八角の壁。前の盾 → 肩の円盤 → 背の尖塔 → 外輪の砲塔
        List<Part> wall = new ArrayList<>();
        pair(wall, 0, true, 0.5, 0.6, pt(0.35, 0.6), pt(0.65, 0.6), pt(0.6, 1.1), pt(0.4, 1.1));
        pair(wall, 1, false, 1.0, 0.0, ngon(1.15, 0.0, 0.3, 8));
        pair(wall, 2, false, 0.5, -0.7, pt(0.3, -0.7), pt(0.7, -0.7), pt(0.5, -1.4));
        wall.add(part(3, true, 0, 0.7, pt(-0.15, 0.7), pt(0.15, 0.7), pt(0.1, 1.4), pt(-0.1, 1.4)));
        pair(wall, 4, false, 1.5, 0.0, pt(1.4, -0.5), pt(1.75, -0.5), pt(1.75, 0.5), pt(1.4, 0.5));
        LOOKS.put("Boss10", new BossLook(new Color(255, 140, 80), ngon(0, 0, 0.95, 8), wall));

        // Boss11「レイン」: 雲のような丸い機体。垂れ下がる雫 → 横の雲 → 雨筋
        List<Part> rain = new ArrayList<>();
        for (int i = -2; i <= 2; i++)
            rain.add(part(0, true, i * 0.3, 0.6, pt(i * 0.3 - 0.07, 0.6), pt(i * 0.3, 1.0 + (2 - Math.abs(i)) * 0.12),
                    pt(i * 0.3 + 0.07, 0.6)));
        pair(rain, 1, false, 0.9, 0.0, ngon(1.0, 0.05, 0.4, 10));
        pair(rain, 2, false, 1.3, -0.2, ngon(1.45, -0.2, 0.28, 10));
        pair(rain, 3, false, 0.6, -0.8, blade(0.3, -0.7, 0.8, -1.0, 0.6, -1.5, 0.15));
        pair(rain, 4, true, 0.9, 0.5, pt(0.8, 0.5), pt(1.0, 0.5), pt(0.9, 1.4));
        LOOKS.put("Boss11", new BossLook(new Color(100, 170, 255), new double[][] { pt(0, 0.7), pt(-0.6, 0.55),
                pt(-1.0, 0.1), pt(-0.8, -0.45), pt(-0.3, -0.75), pt(0.3, -0.75), pt(0.8, -0.45), pt(1.0, 0.1),
                pt(0.6, 0.55) }, rain));

        // Boss12「クロス」: 十字の機体。縦の砲身 → 横腕 → 斜めの腕 → 先端の円
        List<Part> cross = new ArrayList<>();
        cross.add(part(0, true, 0, 0.6, pt(-0.1, 0.6), pt(0.1, 0.6), pt(0.1, 1.3), pt(-0.1, 1.3)));
        pair(cross, 1, false, 0.7, 0.0, pt(0.6, -0.15), pt(1.5, -0.15), pt(1.5, 0.15), pt(0.6, 0.15));
        pair(cross, 2, false, 0.6, -0.6, pt(0.5, -0.5), pt(1.3, -1.3), pt(1.5, -1.1), pt(0.7, -0.3));
        pair(cross, 3, false, 0.6, 0.6, pt(0.5, 0.5), pt(1.3, 1.3), pt(1.5, 1.1), pt(0.7, 0.3));
        pair(cross, 4, false, 1.5, 0.0, ngon(1.6, 0.0, 0.22, 8));
        cross.add(part(4, false, 0, -1.0, ngon(0, -1.2, 0.22, 8)));
        LOOKS.put("Boss12", new BossLook(new Color(255, 80, 120), new double[][] { pt(0, 1.0), pt(-0.25, 0.25),
                pt(-1.0, 0.25), pt(-1.0, -0.25), pt(-0.25, -0.25), pt(-0.25, -0.9), pt(0.25, -0.9), pt(0.25, -0.25),
                pt(1.0, -0.25), pt(1.0, 0.25), pt(0.25, 0.25) }, cross));

        // Boss13「ツイン」: 二連の円。もう一つの核 → 連結アーム → 外側の輪 → 双砲
        List<Part> twin = new ArrayList<>();
        pair(twin, 0, true, 0.45, 0.5, pt(0.35, 0.5), pt(0.55, 0.5), pt(0.5, 1.2), pt(0.4, 1.2));
        pair(twin, 1, false, 1.0, 0.0, ngon(1.0, 0.0, 0.45, 12));
        pair(twin, 2, false, 0.7, 0.0, pt(0.6, -0.1), pt(1.0, -0.1), pt(1.0, 0.1), pt(0.6, 0.1));
        pair(twin, 3, false, 1.0, 0.0, ngon(1.0, 0.0, 0.62, 16));
        pair(twin, 4, true, 1.0, 0.4, pt(0.92, 0.4), pt(1.08, 0.4), pt(1.06, 1.2), pt(0.94, 1.2));
        LOOKS.put("Boss13", new BossLook(new Color(150, 255, 90), ngon(0, 0, 0.6, 12), twin));

        // Boss14「ストップ」: 六角の箱。前の止め具 → 左右のバンパー → 後ろの脚 → 警告灯
        List<Part> stop = new ArrayList<>();
        pair(stop, 0, true, 0.5, 0.8, pt(0.3, 0.8), pt(0.7, 0.8), pt(0.7, 1.1), pt(0.3, 1.1));
        pair(stop, 1, false, 1.0, 0.0, pt(1.0, -0.4), pt(1.4, -0.4), pt(1.4, 0.4), pt(1.0, 0.4));
        pair(stop, 2, false, 0.6, -0.8, pt(0.4, -0.8), pt(0.8, -0.8), pt(0.8, -1.4), pt(0.5, -1.4));
        pair(stop, 3, true, 0.3, 0.2, ngon(0.4, 0.3, 0.16, 6));
        stop.add(part(4, true, 0, 0.3, ngon(0, 0.2, 0.3, 6)));
        LOOKS.put("Boss14", new BossLook(new Color(255, 70, 70), ngon(0, 0, 1.0, 6), stop));

        // Boss15「スナイプ」: 細い矢尻。照準の輪 → 長砲身 → 後退翼 → 尾の針
        List<Part> snipe = new ArrayList<>();
        snipe.add(part(0, true, 0, 0.5, ngon(0, 0.6, 0.3, 14)));
        snipe.add(part(1, true, 0, 0.7, pt(-0.06, 0.7), pt(0.06, 0.7), pt(0.04, 1.6), pt(-0.04, 1.6)));
        pair(snipe, 2, false, 0.4, -0.2, pt(0.4, -0.2), pt(1.4, -0.9), pt(0.6, -0.6));
        pair(snipe, 3, true, 0.5, 0.3, pt(0.45, 0.3), pt(0.55, 0.3), pt(0.5, 1.4));
        snipe.add(part(4, false, 0, -0.7, pt(-0.1, -0.7), pt(0, -1.6), pt(0.1, -0.7)));
        LOOKS.put("Boss15", new BossLook(new Color(255, 255, 120), new double[][] { pt(0, 1.2), pt(-0.45, 0.1),
                pt(-0.3, -0.6), pt(0, -0.35), pt(0.3, -0.6), pt(0.45, 0.1) }, snipe));

        // Boss16「ブルーム」: 花の機体。内側の花弁 → 外側の花弁 → がく → 雌しべ
        List<Part> bloom = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            double x = Math.cos(a) * 0.9, y = Math.sin(a) * 0.9;
            bloom.add(part(0, false, x * 0.6, y * 0.6, ngon(x, y, 0.38, 8)));
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + Math.PI / 6;
            double x = Math.cos(a) * 1.35, y = Math.sin(a) * 1.35;
            bloom.add(part(2, false, x * 0.5, y * 0.5, ngon(x, y, 0.3, 6)));
        }
        pair(bloom, 3, true, 0.3, 0.7, blade(0.3, 0.6, 0.7, 1.0, 0.4, 1.5, 0.12));
        bloom.add(part(4, true, 0, 0.0, ngon(0, 0, 0.3, 10)));
        LOOKS.put("Boss16", new BossLook(new Color(255, 130, 200), ngon(0, 0, 0.7, 12), bloom));

        // Boss17「ヴォルテックス」: 渦巻きの機体。内の羽根 → 外の羽根 → 大きな羽根 → 中心の目
        List<Part> vortex = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3;
            vortex.add(part(0, false, 0, 0, rot(blade(0.3, 0.0, 1.0, 0.2, 1.0, 0.9, 0.18), a)));
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + Math.PI / 3;
            vortex.add(part(2, false, 0, 0, rot(blade(0.3, 0.0, 1.3, 0.3, 1.3, 1.2, 0.22), a)));
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + Math.PI / 6;
            vortex.add(part(3, false, 0, 0, rot(blade(0.3, 0.0, 1.6, 0.3, 1.6, 1.4, 0.26), a)));
        }
        vortex.add(part(4, true, 0, 0, ngon(0, 0, 0.28, 8)));
        LOOKS.put("Boss17", new BossLook(new Color(120, 90, 255), ngon(0, 0, 0.75, 10), vortex));

        // Boss18「ミラージュ」: 鏡のような菱形。分身の菱形 → 破片 → 光の筋 → 前の結晶
        List<Part> mirage = new ArrayList<>();
        pair(mirage, 0, false, 0.9, 0.0, pt(0.9, 0.0), pt(1.3, 0.5), pt(1.7, 0.0), pt(1.3, -0.5));
        pair(mirage, 1, false, 0.6, -0.9, pt(0.5, -1.2), pt(0.8, -0.8), pt(0.5, -0.5), pt(0.2, -0.8));
        pair(mirage, 2, true, 0.3, 0.8, pt(0.3, 0.7), pt(0.55, 0.95), pt(0.3, 1.3), pt(0.1, 0.95));
        pair(mirage, 3, false, 1.6, 0.8, pt(1.6, 0.6), pt(1.8, 0.9), pt(1.6, 1.2), pt(1.4, 0.9));
        mirage.add(part(4, true, 0, 0.9, pt(-0.15, 0.9), pt(0, 1.5), pt(0.15, 0.9), pt(0, 0.7)));
        LOOKS.put("Boss18", new BossLook(new Color(190, 120, 255), new double[][] { pt(0, 1.1), pt(-0.75, 0.0),
                pt(0, -1.0), pt(0.75, 0.0) }, mirage));

        // Boss19「オメガ」: 大型の王機。肩の双砲 → 大きな翼 → 背の冠 → 外輪 → 全砲塔
        List<Part> omega = new ArrayList<>();
        pair(omega, 0, true, 0.55, 0.3, pt(0.45, 0.3), pt(0.65, 0.3), pt(0.62, 1.2), pt(0.48, 1.2));
        pair(omega, 1, false, 1.1, 0.0, pt(1.1, -0.3), pt(1.8, 0.5), pt(1.3, 0.6), pt(0.9, 0.2));
        for (int i = -2; i <= 2; i++)
            omega.add(part(2, false, i * 0.3, -0.8, pt(i * 0.3 - 0.1, -0.8), pt(i * 0.3, -1.4 + Math.abs(i) * 0.15),
                    pt(i * 0.3 + 0.1, -0.8)));
        pair(omega, 3, false, 1.6, 0.0, ngon(1.7, 0.1, 0.3, 8));
        omega.add(part(4, true, 0, 0.5, pt(-0.14, 0.5), pt(0.14, 0.5), pt(0.1, 1.55), pt(-0.1, 1.55)));
        pair(omega, 4, true, 1.1, 0.4, pt(1.0, 0.4), pt(1.2, 0.4), pt(1.18, 1.1), pt(1.02, 1.1));
        LOOKS.put("Boss19", new BossLook(new Color(245, 245, 255), new double[][] { pt(0, 1.1), pt(-0.5, 0.8),
                pt(-1.1, 0.2), pt(-1.0, -0.7), pt(-0.4, -0.5), pt(0, -0.95), pt(0.4, -0.5), pt(1.0, -0.7),
                pt(1.1, 0.2), pt(0.5, 0.8) }, omega));
    }

    /** 頂点を原点まわりに回す(渦巻きの羽根用) */
    private static double[][] rot(double[][] p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        double[][] o = new double[p.length][];
        for (int i = 0; i < p.length; i++)
            o[i] = pt(p[i][0] * c - p[i][1] * s, p[i][0] * s + p[i][1] * c);
        return o;
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

        // Boss7「スナイパー」: 細い筒の機体。照準レンズ → 長い双砲身 → 安定翼 → 中央の超長砲身と外側の砲 → 翼端の観測ポッド
        List<Part> sniper = new ArrayList<>();
        sniper.add(part(0, true, 0, 0.55, ngon(0, 0.6, 0.27, 12)));
        sniper.add(part(0, true, 0, 0.55, ngon(0, 0.6, 0.15, 12)));
        pair(sniper, 1, true, 0.38, 0.3, pt(0.33, 0.3), pt(0.45, 0.3), pt(0.43, 1.4), pt(0.35, 1.4));
        pair(sniper, 2, false, 0.3, -0.4, pt(0.3, -0.35), pt(1.15, -0.95), pt(1.25, -0.7), pt(0.5, 0.0));
        sniper.add(part(3, true, 0, 0.7, pt(-0.07, 0.7), pt(0.07, 0.7), pt(0.05, 1.5), pt(-0.05, 1.5)));
        pair(sniper, 3, true, 0.85, 0.0, pt(0.78, 0.0), pt(0.92, 0.0), pt(0.9, 1.1), pt(0.8, 1.1));
        pair(sniper, 4, false, 1.45, -0.2, pt(1.35, -0.55), pt(1.75, -0.4), pt(1.75, 0.3), pt(1.35, 0.4));
        sniper.add(part(4, false, 1.55, -0.1, ngon(1.55, -0.1, 0.16, 8)));
        sniper.add(part(4, false, -1.55, -0.1, ngon(-1.55, -0.1, 0.16, 8)));
        LOOKS.put("Boss7", new BossLook(new Color(255, 110, 210), new double[][] { pt(0, 1.2), pt(-0.3, 0.9),
                pt(-0.32, -0.55), pt(-0.18, -0.95), pt(0.18, -0.95), pt(0.32, -0.55), pt(0.3, 0.9) }, sniper));

        // Boss8「カスケード」: 流線形のしずく型の機体。尾びれ → 脇のヒレ → 前の誘導ヒレ → 長い流れるヒレ → 外側の波ヒレ
        List<Part> cascade = new ArrayList<>();
        pair(cascade, 0, false, 0.3, -0.6, blade(0.3, -0.6, 0.9, -1.0, 0.7, -1.45, 0.16));
        pair(cascade, 1, false, 0.7, 0.0, blade(0.7, 0.2, 1.4, 0.1, 1.5, -0.5, 0.2));
        pair(cascade, 2, true, 0.35, 0.7, blade(0.4, 0.5, 0.95, 0.9, 0.8, 1.4, 0.13));
        pair(cascade, 3, false, 0.6, -0.3, blade(0.6, -0.3, 1.6, -0.5, 1.7, -1.3, 0.24));
        pair(cascade, 4, false, 1.2, 0.3, blade(1.2, 0.5, 1.75, 0.8, 1.7, 0.1, 0.16));
        cascade.add(part(4, true, 0, 0.9, pt(-0.1, 0.8), pt(0, 1.35), pt(0.1, 0.8)));
        double[][] drop = new double[20][];
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20 + Math.PI / 2;
            double r = 0.82;
            double y = Math.sin(a);
            // 前(+y)はとがり、後ろ(-y)は丸い
            drop[i] = pt(Math.cos(a) * r * (y > 0 ? 1 - 0.45 * y : 1), y > 0 ? y * 1.15 : y * 0.9);
        }
        LOOKS.put("Boss8", new BossLook(new Color(60, 225, 190), drop, cascade));

        // Boss9「プリズム」: 結晶の多面体の機体。前の結晶 → 側面の欠片 → 後ろの欠片 → 浮かぶ菱形 → 大翼と中央の大結晶
        List<Part> prism = new ArrayList<>();
        pair(prism, 0, true, 0.35, 0.6, pt(0.15, 0.55), pt(0.55, 0.65), pt(0.35, 1.3));
        pair(prism, 1, false, 0.9, 0.0, pt(0.85, 0.35), pt(1.5, 0.0), pt(0.95, -0.4));
        pair(prism, 2, false, 0.45, -0.7, pt(0.2, -0.6), pt(0.7, -1.0), pt(0.4, -1.5));
        pair(prism, 3, false, 1.2, 0.8, pt(1.2, 0.55), pt(1.45, 0.85), pt(1.2, 1.15), pt(0.95, 0.85));
        pair(prism, 3, false, 1.2, -0.9, pt(1.2, -1.15), pt(1.45, -0.85), pt(1.2, -0.55), pt(0.95, -0.85));
        pair(prism, 4, false, 1.4, 0.0, pt(1.3, -0.35), pt(1.75, -0.9), pt(1.7, 0.55));
        prism.add(part(4, true, 0, 0.9, pt(-0.2, 0.75), pt(0, 1.4), pt(0.2, 0.75), pt(0, 0.5)));
        LOOKS.put("Boss9", new BossLook(new Color(215, 225, 245), new double[][] { pt(0, 1.15), pt(-0.55, 0.6),
                pt(-1.0, 0.05), pt(-0.7, -0.6), pt(-0.3, -0.95), pt(0.3, -0.95), pt(0.7, -0.6), pt(1.0, 0.05),
                pt(0.55, 0.6) }, prism));

        registerExtra();
    }
}
