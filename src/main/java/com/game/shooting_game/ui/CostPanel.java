package com.game.shooting_game.ui;

import com.game.shooting_game.domain.cost.CostService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.data.UserData;
import com.game.shooting_game.domain.cost.Cost;

/**
 * 必要コスト(コイン・素材)の表示部品
 * 素材は「どのレア度のものを何個」と決まっていて、同じレア度のものだけが数えられる。
 * 各欄は レア度の色の枠つきアイコン + 「所持数 / 必要数」。足りないものは赤、足りているものは緑。
 * 1行目がコイン(全幅)、2行目以降が素材(3列)。
 */
public class CostPanel extends JPanel {
    private static final Color ENOUGH = new Color(140, 255, 160);
    private static final Color SHORT = new Color(255, 100, 100);
    private static final int ITEM_COLS = 3;
    private static final int DEFAULT_ITEM_ROWS = 4;
    private static final Rarity[] RARITIES = Rarity.values();

    private record Entry(ResourceKinds kind, long have, long need) {
        boolean enough() {
            return have >= need;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<ResourceKinds, BufferedImage> iconCache = new HashMap<>();
    private final int maxItemRows; // 素材を並べる最大の行数(行数が少ないほど、パネルを小さくできる)
    private final int iconSize;
    private final int rowH;
    private final int legendH;
    private final Font font;
    private final Font smallFont;

    public CostPanel() {
        this(DEFAULT_ITEM_ROWS);
    }

    /**
     * @param maxItemRows 素材を並べる最大の行数(1行に3種類。画面が狭いパネルは小さくする)
     */
    public CostPanel(int maxItemRows) {
        this.maxItemRows = Math.max(1, maxItemRows);
        double adj = ShootingGameMain.getScreenSizeAdj();
        iconSize = (int) (22 * adj);
        rowH = (int) (32 * adj);
        legendH = (int) (22 * adj);
        font = new Font("SansSerif", Font.BOLD, Math.max(9, (int) (12 * adj)));
        smallFont = new Font("SansSerif", Font.BOLD, Math.max(8, (int) (10 * adj)));
        setOpaque(false);
        setLayout(null);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * レア度の色
     */
    public static Color rarityColor(Rarity r) {
        return switch (r) {
            case COMMON -> new Color(170, 175, 185);
            case UNCOMMON -> new Color(90, 210, 110);
            case RARE -> new Color(90, 160, 255);
            case EPIC -> new Color(190, 110, 255);
            case LEGENDARY -> new Color(255, 195, 60);
        };
    }

    /**
     * レア度の1文字表記
     */
    public static String rarityLetter(Rarity r) {
        return switch (r) {
            case COMMON -> "C";
            case UNCOMMON -> "U";
            case RARE -> "R";
            case EPIC -> "E";
            case LEGENDARY -> "L";
        };
    }

    /**
     * レア度の日本語名
     */
    public static String rarityName(Rarity r) {
        return switch (r) {
            case COMMON -> "コモン";
            case UNCOMMON -> "アンコモン";
            case RARE -> "レア";
            case EPIC -> "エピック";
            case LEGENDARY -> "レジェンド";
        };
    }

    /**
     * 表示するコストの設定
     * 
     * @param cost コスト(nullなら何も表示しない)
     */
    public void setCost(Cost cost) {
        entries.clear();
        if (cost != null) {
            UserData ud = DataManager.getUserData();
            long coins = ud == null ? 0 : CostService.coins();
            entries.add(new Entry(ResourceKinds.COIN, coins, cost.coins()));

            // 素材名の順 → レア度の低い順
            List<Map.Entry<ResourceKinds, Integer>> list = new ArrayList<>(cost.items().entrySet());
            list.sort((a, b) -> {
                int c = a.getKey().getResName().compareTo(b.getKey().getResName());
                return c != 0 ? c : a.getKey().getRarity().compareTo(b.getKey().getRarity());
            });
            for (Map.Entry<ResourceKinds, Integer> e : list)
                entries.add(new Entry(e.getKey(), CostService.owned(e.getKey()), e.getValue()));
        }
        repaint();
    }

    /**
     * 最大の表示に必要な高さ(コイン1行 + 素材の最大行数 + 凡例)
     */
    public int maxHeight() {
        return (1 + maxItemRows) * rowH + legendH;
    }

    private BufferedImage icon(ResourceKinds kind) {
        if (iconCache.containsKey(kind))
            return iconCache.get(kind);

        BufferedImage img = null;
        try {
            var map = ResourceManager.getInstance().getResourceImage();
            Map<Rarity, BufferedImage[]> rarityMap = map == null ? null : map.get(kind.getResName());
            BufferedImage[] frames = rarityMap == null ? null : rarityMap.get(kind.getRarity());
            if (frames != null && frames.length > 0 && frames[0] != null)
                img = UIBuilder.resizeImage(frames[0], iconSize);
        } catch (RuntimeException ignored) {
            // 画像が読めない場合は仮のアイコンを描く
        }
        iconCache.put(kind, img);
        return img;
    }

    /**
     * 欄の位置(x, y, 幅)
     */
    private int[] cell(int index) {
        if (index == 0)
            return new int[] { 0, 0, getWidth() };
        int i = index - 1;
        int colW = getWidth() / ITEM_COLS;
        return new int[] { (i % ITEM_COLS) * colW, (1 + i / ITEM_COLS) * rowH, colW };
    }

    /**
     * レア度の札を描く(幅を返す)
     */
    private static int chip(Graphics2D g2, int x, int y, int h, String text, Color bg, Color fg) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 8;
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 6, 6);
        g2.setColor(fg);
        g2.drawString(text, x + 4, y + (h + fm.getAscent()) / 2 - 2);
        return w;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        boolean hasItem = entries.size() > 1;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int[] c = cell(i);
            int x = c[0], y = c[1], w = c[2];
            boolean coin = e.kind().getResName() == ResourceName.COIN;
            Color ring = coin ? new Color(240, 200, 60) : rarityColor(e.kind().getRarity());

            // 欄の背景(足りないときは赤い縁)
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRoundRect(x + 2, y + 2, w - 4, rowH - 4, 8, 8);
            g2.setColor(e.enough() ? new Color(140, 255, 160, 60) : new Color(255, 100, 100, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x + 2, y + 2, w - 5, rowH - 5, 8, 8);

            // アイコン(レア度の色の枠つき)
            int ix = x + 6;
            int iy = y + (rowH - iconSize) / 2;
            g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 60));
            g2.fillRoundRect(ix - 2, iy - 2, iconSize + 4, iconSize + 4, 8, 8);
            BufferedImage img = icon(e.kind());
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            if (img != null) {
                g2.drawImage(img, ix + (iconSize - img.getWidth()) / 2, iy + (iconSize - img.getHeight()) / 2, null);
            } else {
                g2.setColor(coin ? new Color(240, 200, 60) : new Color(110, 140, 190));
                g2.fillOval(ix, iy, iconSize, iconSize);
                g2.setColor(Color.BLACK);
                String s = coin ? "$" : e.kind().getResName().getDisplayName().substring(0, 1);
                g2.drawString(s, ix + (iconSize - fm.stringWidth(s)) / 2, iy + (iconSize + fm.getAscent()) / 2 - 2);
            }
            g2.setColor(ring);
            g2.setStroke(new BasicStroke((float) (1.6 * adj)));
            g2.drawRoundRect(ix - 2, iy - 2, iconSize + 4, iconSize + 4, 8, 8);

            // レア度の1文字(アイコンの右下)
            if (!coin) {
                g2.setFont(smallFont);
                int ch = (int) (11 * adj);
                chip(g2, ix + iconSize - (int) (6 * adj), iy + iconSize - (int) (6 * adj), ch,
                        rarityLetter(e.kind().getRarity()), ring, Color.BLACK);
            }

            // 所持数 / 必要数
            g2.setFont(font);
            fm = g2.getFontMetrics();
            String text = haveText(e.have()) + " / " + String.format("%,d", e.need());
            g2.setColor(e.enough() ? ENOUGH : SHORT);
            g2.drawString(text, ix + iconSize + 10, y + (rowH + fm.getAscent()) / 2 - 2);
        }

        // 凡例(レア度の色)と変換のヒント
        if (hasItem) {
            g2.setFont(smallFont);
            FontMetrics fm = g2.getFontMetrics();
            int h = legendH - 6;
            int y = getHeight() - legendH + 3;
            int x = 4;
            for (Rarity r : RARITIES)
                x += chip(g2, x, y, h, rarityName(r), rarityColor(r), Color.BLACK) + 3;
            g2.setColor(new Color(200, 200, 200));
            String hint = "5個→1つ上";
            g2.drawString(hint, Math.max(x + 6, getWidth() - fm.stringWidth(hint) - 4),
                    y + (h + fm.getAscent()) / 2 - 2);
        }
        g2.dispose();
    }

    /**
     * 所持数の表示(管理者モードの無制限は∞)
     */
    private static String haveText(long have) {
        return have >= CostService.UNLIMITED ? "∞" : String.format("%,d", have);
    }

    @Override
    public String getToolTipText(MouseEvent ev) {
        for (int i = 0; i < entries.size(); i++) {
            int[] c = cell(i);
            if (ev.getX() >= c[0] && ev.getX() < c[0] + c[2] && ev.getY() >= c[1] && ev.getY() < c[1] + rowH) {
                Entry e = entries.get(i);
                ResourceKinds k = e.kind();
                if (k.getResName() == ResourceName.COIN)
                    return "コイン";
                return k.getResName().getDisplayName() + "(" + rarityName(k.getRarity()) + ")  所持 " + haveText(e.have())
                        + " / 必要 " + e.need();
            }
        }
        return null;
    }
}
