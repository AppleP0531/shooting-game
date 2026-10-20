package com.game.shooting_game.gameScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import javax.swing.JButton;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.ui.UIBuilder;

/**
 * ステージ中に使う、ショップで買った特殊アイテム(ボム・フリーズ)のボタン。
 * キー表示・アイコン・残り数を自分で描く。残りが0のときは暗くして押せなくする。
 */
public final class ItemSlotButton extends JButton {
    private final ItemKinds kind;
    private final String keyHint;
    private final Color color;
    private int count = -1;

    public ItemSlotButton(ItemKinds kind, String keyHint, Color color) {
        this.kind = kind;
        this.keyHint = keyHint;
        this.color = color;
        setFocusable(false); // ゲームのキー入力を奪わない
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
    }

    /** 残り数を読み直す(変わったときだけ再描画) */
    public void refresh() {
        int n = ShopService.owned(kind);
        if (n != count) {
            count = n;
            setEnabled(n > 0);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UIBuilder.applyHighQualityRenderSettings(g);
        double adj = ShootingGameMain.getScreenSizeAdj();
        boolean on = count > 0;
        int w = getWidth(), h = getHeight(), arc = (int) (10 * adj);

        g.setColor(new Color(10, 20, 40, on ? (getModel().isPressed() ? 235 : 190) : 120));
        g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
        Color edge = on ? color : new Color(110, 110, 120);
        g.setColor(edge);
        g.setStroke(new BasicStroke((float) (2 * adj)));
        g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        // アイコン
        int icon = (int) (h * 0.55);
        int ix = (w - icon) / 2, iy = (int) (h * 0.12);
        Map<ItemKinds, BufferedImage[]> images = ItemManager.getInstance().getItemImage();
        BufferedImage[] frames = images == null ? null : images.get(kind);
        if (!on)
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
        if (frames != null && frames.length > 0 && frames[0] != null) {
            g.drawImage(UIBuilder.cachedResize(frames[0], icon), ix, iy, null);
        } else {
            g.setColor(edge);
            g.fillOval(ix, iy, icon, icon);
        }
        g.setComposite(java.awt.AlphaComposite.SrcOver);

        // キー表示(左上)と残り数(下)
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (12 * adj)));
        g.setColor(on ? Color.WHITE : Color.GRAY);
        g.drawString(keyHint, (int) (5 * adj), (int) (14 * adj));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (14 * adj)));
        String txt = "×" + Math.max(count, 0);
        g.drawString(txt, (w - g.getFontMetrics().stringWidth(txt)) / 2, h - (int) (6 * adj));
        g.dispose();
    }
}
