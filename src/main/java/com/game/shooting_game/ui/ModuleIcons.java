package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.game.shooting_game.domain.module.Module;
import com.game.shooting_game.domain.module.ModuleKind;

/**
 * モジュールのアイコン(種類ごとの絵 + レア度の色の枠 + レベル)
 * 絵は /static/img/module/{種類のid}.png。読めないときは、レア度の色の板に種類の文字を出す。
 */
public final class ModuleIcons {
    private static final String PATH = "/static/img/module/";
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    private ModuleIcons() {
    }

    /**
     * 種類の絵(枠・レベルなし)を指定サイズで取得する。画像がなければnull
     */
    public static synchronized BufferedImage get(ModuleKind kind, int size) {
        String key = kind.getId() + "@" + size;
        if (cache.containsKey(key))
            return cache.get(key);

        BufferedImage img = null;
        try {
            img = UIBuilder.resizeImage(UIBuilder.loadImage(PATH + kind.getId() + ".png"), size);
        } catch (IOException | RuntimeException e) {
            // 画像がないときは呼び出し側で文字の仮描画にする
        }
        cache.put(key, img);
        return img;
    }

    /**
     * モジュール1個を描く(絵 + レア度の枠。大きいときはレベルの札も付く)
     *
     * @param g    描画先
     * @param m    モジュール
     * @param x    左上のx
     * @param y    左上のy
     * @param size 1辺の長さ
     */
    public static void draw(Graphics2D g, Module m, int x, int y, int size) {
        draw(g, m, x, y, size, false);
    }

    /**
     * モジュール1個を描く(equippedなら右上に「装備中」の札が付く。大きいときだけ)
     */
    public static void draw(Graphics2D g, Module m, int x, int y, int size, boolean equipped) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color rc = CostPanel.rarityColor(m.getRarity());
        int arc = Math.max(4, size / 5);
        BufferedImage img = get(m.getKind(), size);
        if (img != null) {
            g2.drawImage(img, x + (size - img.getWidth()) / 2, y + (size - img.getHeight()) / 2, null);
        } else {
            g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 90));
            g2.fillRoundRect(x, y, size, size, arc, arc);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, size / 2)));
            FontMetrics fm = g2.getFontMetrics();
            String s = m.getKind().getShortName();
            g2.setColor(Color.WHITE);
            g2.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent()) / 2 - 2);
        }

        // レア度の枠
        g2.setColor(rc);
        g2.setStroke(new BasicStroke(Math.max(1.5f, size / 22f)));
        g2.drawRoundRect(x, y, size - 1, size - 1, arc, arc);

        // レベルの札(大きいときだけ)
        if (size >= 28) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, size / 4)));
            FontMetrics fm = g2.getFontMetrics();
            String lv = "Lv" + m.getLevel();
            int w = fm.stringWidth(lv) + 6;
            int h = fm.getHeight() - 2;
            int bx = x + size - w - 1;
            int by = y + size - h - 1;
            g2.setColor(new Color(0, 0, 0, 190));
            g2.fillRoundRect(bx, by, w, h, 6, 6);
            g2.setColor(rc.brighter());
            g2.drawString(lv, bx + 3, by + fm.getAscent() - 1);
        }

        // 装備中の札(右上)
        if (equipped && size >= 28) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, size / 6 + 1)));
            FontMetrics fm = g2.getFontMetrics();
            String text = "装備中";
            int w = fm.stringWidth(text) + 6;
            int h = fm.getHeight() - 2;
            int bx = x + size - w - 1;
            int by = y + 1;
            g2.setColor(new Color(90, 230, 140, 235));
            g2.fillRoundRect(bx, by, w, h, 6, 6);
            g2.setColor(Color.BLACK);
            g2.drawString(text, bx + 3, by + fm.getAscent() - 1);
        }
        g2.dispose();
    }

    /**
     * ボタンや一覧に付けるアイコンにする
     */
    public static ImageIcon icon(Module m, int size) {
        return icon(m, size, false);
    }

    public static ImageIcon icon(Module m, int size, boolean equipped) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        draw(g, m, 0, 0, size, equipped);
        g.dispose();
        return new ImageIcon(img);
    }
}
