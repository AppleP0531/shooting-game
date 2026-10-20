package com.game.shooting_game.domain.subship;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.ui.UIBuilder;

/**
 * サブ機の描画(ゲーム中と工房のプレビューで共通)
 * 機体の画像(フレームのMk1~5)に、付けているパーツ(主砲・推進機・コア・エミッター)を重ねて描く。
 * 画像は SubShip{Mk}_L.png / _R.png(左右対称)。なければ簡易の三角形で描く。
 */
public final class SubShipRenderer {
    private static final String IMAGE_PATH = "/static/img/ship/player/SubShip%d_%s.png";
    private static final double BASE_PX = 40; // フレームMk1の画像の大きさ(画面倍率なし)
    private static final Color BLUE = new Color(80, 150, 255);
    private static final Color RED = new Color(255, 80, 90);

    private static final Map<String, BufferedImage> originals = new HashMap<>();
    private static final Map<String, BufferedImage> scaled = new HashMap<>();

    private SubShipRenderer() {
    }

    /**
     * 機体画像の取得(読めなければnull)
     */
    // タイプごとの色(色相。標準は元の絵のまま)。0=標準 1=ガンナー 2=ランサー 3=シーカー
    private static final float[] TYPE_HUE = { -1f, 0.02f, 0.52f, 0.76f };
    private static final Color[] TYPE_ACCENT = { new Color(255, 214, 80), new Color(255, 120, 70),
            new Color(110, 220, 255), new Color(190, 140, 255) };

    /** 絵の色相を、タイプの色にずらす(明るさと濃さは保つ) */
    private static BufferedImage tint(BufferedImage src, float hue) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) {
                    out.setRGB(x, y, 0);
                    continue;
                }
                Color.RGBtoHSB((p >> 16) & 255, (p >> 8) & 255, p & 255, hsb);
                // 色のついた部分だけ染める(白や灰色の部分はそのまま)
                float s = hsb[1] > 0.12f ? Math.min(1f, hsb[1] * 1.1f) : hsb[1];
                int rgb = hsb[1] > 0.12f ? Color.HSBtoRGB(hue, s, hsb[2]) : (p & 0xFFFFFF);
                out.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
            }
        return out;
    }

    private static BufferedImage image(int hull, boolean left, int px, int type) {
        BufferedImage base = image(hull, left, px);
        if (base == null || type <= 0 || type >= TYPE_HUE.length)
            return base;
        String key = hull + (left ? "_L" : "_R") + "@" + px + "#" + type;
        return scaled.computeIfAbsent(key, k -> tint(base, TYPE_HUE[type]));
    }

    private static BufferedImage image(int hull, boolean left, int px) {
        String base = hull + (left ? "_L" : "_R");
        if (!originals.containsKey(base)) {
            BufferedImage img = null;
            try {
                img = UIBuilder.loadImage(String.format(IMAGE_PATH, hull, left ? "L" : "R"));
            } catch (Exception ignored) {
                // 画像がない場合は簡易描画になる
            }
            originals.put(base, img);
        }
        BufferedImage origin = originals.get(base);
        if (origin == null)
            return null;
        String key = base + "@" + px;
        return scaled.computeIfAbsent(key, k -> UIBuilder.resizeImage(origin, px));
    }

    /**
     * サブ機を描く
     * 
     * @param g2      描画先
     * @param cx      中心X
     * @param cy      中心Y
     * @param left    左側の機体ならtrue
     * @param hull    フレームのMk(0なら未制作の薄い影)
     * @param cannon  主砲のMk(0なら無し)
     * @param engine  推進機のMk
     * @param core    コアのMk
     * @param emitter エミッターのMk
     * @param mode    色
     * @param time    経過時間(アニメーション用)
     * @param alpha   不透明度
     * @param scale   大きさの倍率
     */
    public static void draw(Graphics2D g2, double cx, double cy, boolean left,
            int hull, int cannon, int engine, int core, int emitter,
            ColorMode mode, double time, float alpha, double scale) {
        draw(g2, cx, cy, left, hull, cannon, engine, core, emitter, 0, mode, time, alpha, scale);
    }

    /**
     * タイプ(何番目のサブ機か)を指定して描く。タイプごとに機体の色が変わり、下にタイプの番号の数だけ光点が並ぶ
     * 
     * @param type タイプの番号(0=標準、1=2番目、…)
     */
    public static void draw(Graphics2D g2, double cx, double cy, boolean left,
            int hull, int cannon, int engine, int core, int emitter, int type,
            ColorMode mode, double time, float alpha, double scale) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        boolean ghost = hull < 1;
        int hullTier = Math.max(1, Math.min(5, hull));
        double px = BASE_PX * adj * SubShipService.hullSize(hullTier) * scale;
        double phase = left ? 0 : Math.PI;
        cy += Math.sin(time * 3 + phase) * 1.5 * adj; // ふわふわ浮く

        Composite oldComposite = g2.getComposite();
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float a = Math.max(0f, Math.min(1f, ghost ? alpha * 0.25f : alpha));
        Color col = mode == ColorMode.RED ? RED : BLUE;

        // 色が分かる薄い光(自機と同じ色)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.5f));
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) (px * 0.55),
                new float[] { 0f, 1f }, new Color[] { withAlpha(col, 90), withAlpha(col, 0) }));
        g2.fill(new Ellipse2D.Double(cx - px * 0.55, cy - px * 0.55, px * 1.1, px * 1.1));

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

        // 推進機の炎(機体の後ろ)
        if (!ghost)
            drawFlame(g2, cx, cy + px * 0.42, px, engine, col, time, phase);

        // 機体
        BufferedImage img = image(hullTier, left, (int) Math.round(px), type);
        if (img != null) {
            g2.drawImage(img, (int) Math.round(cx - img.getWidth() / 2.0), (int) Math.round(cy - img.getHeight() / 2.0),
                    null);
        } else {
            drawFallback(g2, cx, cy, px, col);
        }

        // 何番目のサブ機かが分かる光点(機体の下。番号の数だけ並ぶ)
        if (!ghost) {
            Color ac = TYPE_ACCENT[Math.max(0, Math.min(TYPE_ACCENT.length - 1, type))];
            double dot = Math.max(1.5, px * 0.05);
            double gap = dot * 2.6;
            for (int i = 0; i <= type; i++) {
                double x = cx + (i - type / 2.0) * gap;
                g2.setColor(withAlpha(ac, 230));
                g2.fill(new Ellipse2D.Double(x - dot / 2, cy + px * 0.5, dot, dot));
            }
        }

        if (!ghost) {
            if (cannon > 0)
                drawCannon(g2, cx, cy, px, cannon);
            if (emitter > 0)
                drawEmitter(g2, cx, cy, px, emitter, time);
            if (core > 0)
                drawCore(g2, cx, cy, px, core, time);
        }

        g2.setComposite(oldComposite);
        if (oldAa != null)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /**
     * 画像がないときの簡易な機体
     */
    private static void drawFallback(Graphics2D g2, double cx, double cy, double px, Color col) {
        Path2D.Double body = new Path2D.Double();
        body.moveTo(cx, cy - px * 0.45);
        body.lineTo(cx + px * 0.2, cy + px * 0.3);
        body.lineTo(cx, cy + px * 0.42);
        body.lineTo(cx - px * 0.2, cy + px * 0.3);
        body.closePath();
        g2.setColor(col);
        g2.fill(body);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fill(new Ellipse2D.Double(cx - px * 0.06, cy - px * 0.05, px * 0.12, px * 0.12));
    }

    private static void drawFlame(Graphics2D g2, double x, double y, double px, int engine, Color col, double time,
            double phase) {
        double len = px * (0.16 + 0.07 * engine) * (0.85 + 0.15 * Math.sin(time * 30 + phase));
        double w = px * (0.07 + 0.012 * engine);
        Path2D.Double flame = new Path2D.Double();
        flame.moveTo(x - w, y);
        flame.quadTo(x, y + len * 1.2, x, y + len);
        flame.quadTo(x, y + len * 1.2, x + w, y);
        flame.closePath();
        g2.setPaint(new java.awt.GradientPaint((float) x, (float) y, new Color(255, 255, 255, 230),
                (float) x, (float) (y + len), withAlpha(col, 40)));
        g2.fill(flame);
    }

    /**
     * 主砲(Mkの数だけ砲身が並ぶ)
     */
    private static void drawCannon(Graphics2D g2, double cx, double cy, double px, int tier) {
        double bw = px * 0.045;
        double bl = px * 0.17;
        double gap = px * 0.085;
        for (int i = 0; i < tier; i++) {
            double x = cx + (i - (tier - 1) / 2.0) * gap;
            double y = cy - px * 0.40;
            RoundRectangle2D.Double barrel = new RoundRectangle2D.Double(x - bw / 2, y, bw, bl, 3, 3);
            g2.setColor(new Color(70, 75, 90));
            g2.fill(barrel);
            g2.setColor(new Color(255, 230, 150));
            g2.fill(new Ellipse2D.Double(x - bw / 2, y - bw * 0.3, bw, bw * 0.8)); // 砲口の光
        }
    }

    /**
     * エミッター(機首のレンズ。Mkが上がるほど大きく光る)
     */
    private static void drawEmitter(Graphics2D g2, double cx, double cy, double px, int tier, double time) {
        double r = px * (0.045 + 0.012 * tier);
        double y = cy - px * 0.16;
        double pulse = 0.8 + 0.2 * Math.sin(time * 6);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, y), (float) (r * 2.6),
                new float[] { 0f, 1f }, new Color[] { new Color(120, 240, 255, (int) (200 * pulse)),
                        new Color(120, 240, 255, 0) }));
        g2.fill(new Ellipse2D.Double(cx - r * 2.6, y - r * 2.6, r * 5.2, r * 5.2));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(cx - r * 0.7, y - r * 0.7, r * 1.4, r * 1.4));
    }

    /**
     * コア(機体の中心で回る金色のリング。Mkの数だけ光点が回る)
     */
    private static void drawCore(Graphics2D g2, double cx, double cy, double px, int tier, double time) {
        double y = cy + px * 0.12;
        double ringR = px * 0.15;
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, y), (float) (ringR * 1.4),
                new float[] { 0f, 1f }, new Color[] { new Color(255, 215, 90, 150), new Color(255, 215, 90, 0) }));
        g2.fill(new Ellipse2D.Double(cx - ringR * 1.4, y - ringR * 1.4, ringR * 2.8, ringR * 2.8));
        g2.setColor(new Color(255, 225, 120));
        double dot = px * 0.035;
        for (int i = 0; i < tier; i++) {
            double ang = time * 2.5 + i * Math.PI * 2 / tier;
            g2.fill(new Ellipse2D.Double(cx + Math.cos(ang) * ringR - dot, y + Math.sin(ang) * ringR - dot, dot * 2,
                    dot * 2));
        }
    }
}
