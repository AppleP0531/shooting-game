package com.game.shooting_game.shopScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.shop.ShopService.ShopItem;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class ShopSceneUI {
    private static final Color NEON = new Color(0, 220, 255);
    private static final Color COIN_COLOR = new Color(255, 210, 60);

    /** 描画に必要な1商品ぶんの情報 */
    public record CardData(ShopItem item, int owned, BufferedImage icon) {
    }

    private final ShootingGameMain MAIN;

    public ShopSceneUI() {
        MAIN = ShootingGameMain.getInstance();
    }

    /** n番目のカードの範囲(ボタンの配置にも使う) */
    public static Rectangle cardRect(int index, int screenWidth) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        int margin = (int) (30 * adj);
        int h = (int) (112 * adj);
        int gap = (int) (14 * adj);
        return new Rectangle(margin, (int) (90 * adj) + index * (h + gap), screenWidth - margin * 2, h);
    }

    /**
     * 描画
     *
     * @param g
     * @param cards   商品ごとの情報
     * @param message 結果メッセージ(空なら何も出さない)
     */
    public void draw(Graphics g, List<CardData> cards, String message) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        Graphics2D g2 = (Graphics2D) g.create();
        UIBuilder.applyHighQualityRenderSettings(g2);

        UIBuilder.drawString(g2, new Anchor(H.CENTER, V.TOP), new Vector2(MAIN.getWidth() / 2, 10 * adj),
                24 * adj, Color.WHITE, Font.BOLD, "SHOP");

        for (int i = 0; i < cards.size(); i++)
            drawCard(g2, cardRect(i, MAIN.getWidth()), cards.get(i), adj);

        if (message != null && !message.isEmpty()) {
            int y = cardRect(cards.size(), MAIN.getWidth()).y;
            UIBuilder.drawString(g2, new Anchor(H.CENTER, V.TOP), new Vector2(MAIN.getWidth() / 2, y),
                    18 * adj, Color.WHITE, Font.BOLD, message);
        }
        g2.dispose();
    }

    private void drawCard(Graphics2D g, Rectangle r, CardData c, double adj) {
        g.setColor(new Color(10, 20, 40, 200));
        g.fillRoundRect(r.x, r.y, r.width, r.height, (int) (16 * adj), (int) (16 * adj));
        g.setColor(NEON);
        g.setStroke(new BasicStroke((float) (2 * adj)));
        g.drawRoundRect(r.x, r.y, r.width, r.height, (int) (16 * adj), (int) (16 * adj));

        int pad = (int) (12 * adj);

        // アイコン(左。カードの高さの中央)
        int iconSize = (int) (48 * adj);
        Vector2 iconPos = new Vector2(r.x + pad + iconSize / 2.0, r.y + r.height / 2.0);
        if (c.icon() != null) {
            UIBuilder.drawImage(g, Anchor.CENTER, iconPos, UIBuilder.cachedResize(c.icon(), iconSize));
        } else {
            g.setColor(NEON);
            g.fillOval((int) iconPos.getX() - iconSize / 2, (int) iconPos.getY() - iconSize / 2, iconSize, iconSize);
        }

        // 文字はカードの幅に収める(説明は折り返して最大2行)
        int textX = r.x + pad + iconSize + pad;
        int textW = r.x + r.width - pad - textX;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, (int) (17 * adj))));
        g.drawString(fit(g, c.item().name(), textW), textX, r.y + (int) (26 * adj));

        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (11 * adj))));
        g.setColor(new Color(200, 215, 230));
        int ly = r.y + (int) (46 * adj);
        List<String> lines = wrap(g, c.item().description(), textW, 2);
        for (String line : lines) {
            g.drawString(line, textX, ly);
            ly += (int) (14 * adj);
        }

        // 下の段: 左に価格と所持数、右下に「買う」ボタン(ShopManager が置く)
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, (int) (14 * adj))));
        g.setColor(COIN_COLOR);
        g.drawString(String.format("%,d コイン", c.item().price()), textX, r.y + r.height - (int) (30 * adj));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, (int) (12 * adj))));
        g.setColor(Color.WHITE);
        g.drawString("所持 " + c.owned() + "/" + c.item().maxOwned(), textX, r.y + r.height - (int) (12 * adj));
    }

    /** 幅に収まるように、末尾を「…」で切る */
    private static String fit(Graphics2D g, String text, int width) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= width)
            return text;
        String t = text;
        while (!t.isEmpty() && fm.stringWidth(t + "…") > width)
            t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    /** 幅に収まるように折り返す(最大の行数を超える分は、最後の行の末尾を「…」で切る) */
    private static List<String> wrap(Graphics2D g, String text, int width, int maxLines) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (fm.stringWidth(cur.toString() + ch) > width && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder();
                if (lines.size() == maxLines)
                    break;
            }
            cur.append(ch);
        }
        if (lines.size() < maxLines && cur.length() > 0)
            lines.add(cur.toString());
        else if (lines.size() == maxLines && i0(text, lines) < text.length())
            lines.set(maxLines - 1, fit(g, lines.get(maxLines - 1) + "…", width));
        return lines;
    }

    private static int i0(String text, List<String> lines) {
        int n = 0;
        for (String l : lines)
            n += l.length();
        return n;
    }
}
