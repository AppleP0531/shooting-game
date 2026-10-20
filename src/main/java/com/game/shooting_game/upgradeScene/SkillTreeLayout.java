package com.game.shooting_game.upgradeScene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * スキルツリーのボタンの置き方(機体ごとに模様が変わる)
 * <p>
 * 木の形(誰が誰の子か)は設定のまま、各ボタンの位置だけを模様に合わせて決める。
 * 位置は基準ボタン(Origin)からのずれ(画面倍率をかける前の値)で返す。画面や設定には依存しない純ロジック。
 * <p>
 * 土台は放射状の配置: 根の周りに5方向(などの枝の数)を等間隔に取り、子は親の向きのまわりに左右対称に広げる。
 * 子が1つならまっすぐ外へ伸びる。同じ深さのボタンは同じ半径の円の上に並び、
 * 間隔が足りなければその深さの円を外へ広げる。
 * <ul>
 * <li>FLOWER 放射状の花(土台そのまま)</li>
 * <li>SPIRAL 渦巻き(外側の円ほど回り込む)</li>
 * <li>HONEYCOMB 六角の蜂の巣(ボタンが六角格子の目に並ぶ)</li>
 * <li>STAR 星形(偶数の深さが外へ突き出る)</li>
 * <li>RING 同心円(同じ深さのボタンが等間隔の輪になる)</li>
 * </ul>
 */
public final class SkillTreeLayout {
    /** 模様 */
    public enum Pattern {
        FLOWER, SPIRAL, HONEYCOMB, STAR, RING
    }

    /** 木の1ボタン(pos は兄弟の中の並び。小さい順に時計回り) */
    public record Node(String name, String parent, int pos) {
    }

    /** ボタンの中心どうしの最低の間隔(基準の大きさ。ボタンは44) */
    public static final double MIN_GAP = 52;

    private static final double STEP = 82; // 1段ごとの距離(最低)

    private SkillTreeLayout() {
    }

    /** 機体ごとの模様 */
    public static Pattern patternFor(String shipId) {
        if (shipId == null)
            return Pattern.FLOWER;
        return switch (shipId) {
            case "Ship2" -> Pattern.SPIRAL;
            case "Ship3" -> Pattern.HONEYCOMB;
            case "Ship4" -> Pattern.STAR;
            case "Ship5" -> Pattern.RING;
            default -> Pattern.FLOWER;
        };
    }

    /**
     * 位置を決める
     *
     * @param pattern 模様
     * @param nodes   ボタン(根は parent が null)
     * @return ボタンの名前 → Origin からのずれ(x, y)
     */
    public static Map<String, double[]> compute(Pattern pattern, List<Node> nodes) {
        Map<String, List<Node>> children = new LinkedHashMap<>();
        Node root = null;
        for (Node n : nodes) {
            if (n.parent() == null) {
                root = n;
                continue;
            }
            children.computeIfAbsent(n.parent(), k -> new ArrayList<>()).add(n);
        }
        Map<String, double[]> out = new LinkedHashMap<>();
        if (root == null)
            return out;
        for (List<Node> l : children.values())
            l.sort((a, b) -> Integer.compare(a.pos(), b.pos()));
        out.put(root.name(), new double[] { 0, 0 });

        // 角度と深さ: 根の周りに、最初の枝が真上に来るように等間隔で並べ、子は親の向きのまわりに左右対称に広げる
        Map<String, double[]> polar = new LinkedHashMap<>(); // name → {角度(ラジアン), 深さ}
        List<Node> top = children.getOrDefault(root.name(), List.of());
        for (int i = 0; i < top.size(); i++)
            polar.put(top.get(i).name(), new double[] { -Math.PI / 2 + 2 * Math.PI * i / top.size(), 1 });
        for (int i = 0; i < top.size(); i++)
            spread(top.get(i), children, polar);

        List<Node> order = new ArrayList<>(); // 深さ順
        for (Node n : nodes)
            if (polar.containsKey(n.name()))
                order.add(n);
        order.sort((a, b) -> Double.compare(polar.get(a.name())[1], polar.get(b.name())[1]));

        // 同心円は、同じ枝・同じ深さのボタンを、枝の向きを中心に等間隔の輪に並べ直す
        if (pattern == Pattern.RING)
            for (Node t : top)
                respaceByDepth(t, children, polar);

        // 渦巻きは、深さが増えるごとに角度を回す
        if (pattern == Pattern.SPIRAL)
            for (double[] p : polar.values())
                p[0] += Math.toRadians(20) * (p[1] - 1);

        // 深さごとの半径(間隔が足りなければ広げる)
        Map<Integer, Double> radii = radii(pattern, order, polar);

        switch (pattern) {
            case HONEYCOMB -> placeHoneycomb(order, polar, out);
            default -> {
                for (Node n : order) {
                    double[] p = polar.get(n.name());
                    double r = radii.get((int) p[1]);
                    out.put(n.name(), new double[] { Math.cos(p[0]) * r, Math.sin(p[0]) * r });
                }
            }
        }
        return out;
    }

    /** 子を、親の向きのまわりに左右対称に広げる(子が1つなら、親の向きのまままっすぐ) */
    private static void spread(Node parent, Map<String, List<Node>> children, Map<String, double[]> polar) {
        List<Node> kids = children.getOrDefault(parent.name(), List.of());
        double[] p = polar.get(parent.name());
        // 子が多いほど、1つあたりの角度は狭める(隣の枝にはみ出さない範囲)
        double step = Math.toRadians(Math.min(30, 70.0 / Math.max(2, kids.size())));
        for (int i = 0; i < kids.size(); i++) {
            polar.put(kids.get(i).name(), new double[] { p[0] + (i - (kids.size() - 1) / 2.0) * step, p[1] + 1 });
            spread(kids.get(i), children, polar);
        }
    }

    /** 深さごとの半径。前の円より STEP 以上外で、同じ円の上のボタンどうしが MIN_GAP 以上離れるように決める */
    private static Map<Integer, Double> radii(Pattern pattern, List<Node> order, Map<String, double[]> polar) {
        Map<Integer, List<Double>> byDepth = new HashMap<>();
        int maxDepth = 0;
        for (Node n : order) {
            int d = (int) polar.get(n.name())[1];
            byDepth.computeIfAbsent(d, k -> new ArrayList<>()).add(norm(polar.get(n.name())[0]));
            maxDepth = Math.max(maxDepth, d);
        }
        Map<Integer, Double> r = new HashMap<>();
        double prev = 0;
        for (int d = 1; d <= maxDepth; d++) {
            double need = prev + STEP;
            List<Double> angles = byDepth.getOrDefault(d, List.of());
            if (angles.size() > 1) {
                List<Double> s = new ArrayList<>(angles);
                s.sort(Double::compare);
                double minSep = 2 * Math.PI;
                for (int i = 0; i < s.size(); i++) {
                    double next = i + 1 < s.size() ? s.get(i + 1) : s.get(0) + 2 * Math.PI;
                    minSep = Math.min(minSep, next - s.get(i));
                }
                if (minSep > 1e-6)
                    need = Math.max(need, MIN_GAP * 1.08 / (2 * Math.sin(Math.min(Math.PI / 2, minSep / 2))));
            }
            // 星形は偶数の深さが外へ突き出る(外へ動かすだけなので間隔は保たれる)
            if (pattern == Pattern.STAR && d % 2 == 0)
                need += STEP * 0.4;
            r.put(d, need);
            prev = need;
        }
        return r;
    }

    /** 同心円: 1つの枝の中で、同じ深さのボタンを、枝の向きを中心に等間隔の輪へ並べ直す(深さの中の順は木の順のまま) */
    private static void respaceByDepth(Node branch, Map<String, List<Node>> children, Map<String, double[]> polar) {
        double axis = polar.get(branch.name())[0];
        Map<Integer, List<Node>> byDepth = new LinkedHashMap<>();
        collect(branch, children, polar, byDepth);
        for (Map.Entry<Integer, List<Node>> e : byDepth.entrySet()) {
            List<Node> list = e.getValue();
            double step = Math.toRadians(Math.min(28, 66.0 / list.size()));
            for (int j = 0; j < list.size(); j++)
                polar.get(list.get(j).name())[0] = axis + (j - (list.size() - 1) / 2.0) * step;
        }
    }

    /** 枝の下のボタンを、木の順(親→子の順に深さ優先)で深さごとに集める */
    private static void collect(Node n, Map<String, List<Node>> children, Map<String, double[]> polar,
            Map<Integer, List<Node>> byDepth) {
        byDepth.computeIfAbsent((int) polar.get(n.name())[1], k -> new ArrayList<>()).add(n);
        for (Node k : children.getOrDefault(n.name(), List.of()))
            collect(k, children, polar, byDepth);
    }

    /** 蜂の巣: 理想の位置にいちばん近い、空いている六角格子の目に置く */
    private static void placeHoneycomb(List<Node> order, Map<String, double[]> polar, Map<String, double[]> out) {
        double s = 62; // 目の間隔
        Set<Long> used = new HashSet<>();
        used.add(key(0, 0));
        for (Node n : order) {
            double[] p = polar.get(n.name());
            double r = s * 1.15 * p[1];
            double ix = Math.cos(p[0]) * r;
            double iy = Math.sin(p[0]) * r;
            double bestD = Double.MAX_VALUE;
            int bq = 0, br = 0;
            for (int rr = -12; rr <= 12; rr++)
                for (int q = -12; q <= 12; q++) {
                    if (used.contains(key(q, rr)))
                        continue;
                    double x = s * (q + rr / 2.0);
                    double y = s * Math.sqrt(3) / 2 * rr;
                    double d = Math.hypot(x - ix, y - iy);
                    if (d < bestD) {
                        bestD = d;
                        bq = q;
                        br = rr;
                    }
                }
            used.add(key(bq, br));
            out.put(n.name(), new double[] { s * (bq + br / 2.0), s * Math.sqrt(3) / 2 * br });
        }
    }

    private static long key(int q, int r) {
        return ((long) (q + 100) << 16) | (r + 100);
    }

    private static double norm(double a) {
        double t = a % (2 * Math.PI);
        return t < 0 ? t + 2 * Math.PI : t;
    }

    /**
     * 配置の広がり(全体が収まる、中心からの最大の距離)
     *
     * @return {横方向の最大値, 縦方向の最大値}(いずれも絶対値)
     */
    public static double[] extent(Map<String, double[]> layout) {
        double mx = 0, my = 0;
        for (double[] p : layout.values()) {
            mx = Math.max(mx, Math.abs(p[0]));
            my = Math.max(my, Math.abs(p[1]));
        }
        return new double[] { mx, my };
    }
}
