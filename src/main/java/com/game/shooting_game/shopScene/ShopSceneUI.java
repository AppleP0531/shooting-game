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
        int h = (int) (170 * adj);
        int gap = (int) (30 * adj);
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

        // アイコン
        int iconSize = (int) (64 * adj);
        Vector2 iconPos = new Vector2(r.x + 20 * adj + iconSize / 2.0, r.y + 20 * adj + iconSize / 2.0);
        if (c.icon() != null) {
            UIBuilder.drawImage(g, Anchor.CENTER, iconPos, UIBuilder.cachedResize(c.icon(), iconSize));
        } else {
            g.setColor(NEON);
            g.fillOval((int) iconPos.getX() - iconSize / 2, (int) iconPos.getY() - iconSize / 2, iconSize, iconSize);
        }

        double textX = r.x + 20 * adj + iconSize + 20 * adj;
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(textX, r.y + 18 * adj), 22 * adj,
                Color.WHITE, Font.BOLD, c.item().name());
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(textX, r.y + 52 * adj), 13 * adj,
                new Color(200, 215, 230), Font.PLAIN, c.item().description());
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(textX, r.y + 84 * adj), 18 * adj,
                COIN_COLOR, Font.BOLD, String.format("%,d コイン", c.item().price()));
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(textX, r.y + 112 * adj), 16 * adj,
                Color.WHITE, Font.PLAIN, "所持 " + c.owned() + "/" + c.item().maxOwned());
    }
}
