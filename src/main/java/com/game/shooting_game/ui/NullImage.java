package com.game.shooting_game.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * 画像ファイルが見つからないときに、代わりに出すダミー画像(「?」の入った市松模様。要素ごとに色が違う)
 *
 * img/null/<要素名>.png があればそれを使い、なければ同じ見た目をコードで描く。
 * どの要素の画像が欠けているかが、ひと目で分かるようにするためのもの。
 */
public final class NullImage {
    /** 要素の種類(色が違う) */
    public enum Kind {
        BULLET(new Color(255, 80, 200)), // 弾
        ITEM(new Color(80, 220, 120)), // アイテム
        RESOURCE(new Color(255, 190, 60)), // 素材
        ENEMY(new Color(255, 90, 90)), // 敵
        BOSS(new Color(190, 60, 60)), // ボス
        PLAYER(new Color(80, 160, 255)), // 自機
        OBJECT(new Color(170, 170, 170)), // そのほかの物(シールドなど)
        MODULE(new Color(180, 120, 255)), // モジュール
        UI(new Color(255, 255, 255)); // ボタンなど

        private final Color color;

        Kind(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    private static final int SIZE = 96;
    private static final Map<Kind, BufferedImage> CACHE = new EnumMap<>(Kind.class);

    private NullImage() {
    }

    /** ダミー画像(1枚) */
    public static synchronized BufferedImage of(Kind kind) {
        return CACHE.computeIfAbsent(kind, k -> {
            try {
                return UIBuilder.loadImage("/static/img/null/" + k.name().toLowerCase() + ".png");
            } catch (java.io.IOException | RuntimeException e) {
                return render(k);
            }
        });
    }

    /** 使うコマ数ぶんに並べたダミー画像(アニメーションの添字がずれないよう、同じ絵を並べる) */
    public static BufferedImage[] frames(Kind kind, int count) {
        BufferedImage[] frames = new BufferedImage[Math.max(1, count)];
        java.util.Arrays.fill(frames, of(kind));
        return frames;
    }

    /** コードでダミー画像を描く(img/null の画像もこれで作った) */
    public static BufferedImage render(Kind kind) {
        BufferedImage im = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = kind.getColor();
        Color dark = new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4);
        int cell = SIZE / 6;
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 6; x++) {
                g.setColor((x + y) % 2 == 0 ? c : dark);
                g.fillRect(x * cell, y * cell, cell, cell);
            }
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(4f));
        g.drawRect(2, 2, SIZE - 5, SIZE - 5);
        g.setFont(new Font("SansSerif", Font.BOLD, 60));
        var fm = g.getFontMetrics();
        int tx = (SIZE - fm.stringWidth("?")) / 2;
        int ty = (SIZE - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(Color.BLACK);
        g.drawString("?", tx + 2, ty + 2);
        g.setColor(Color.WHITE);
        g.drawString("?", tx, ty);
        g.dispose();
        return im;
    }
}
