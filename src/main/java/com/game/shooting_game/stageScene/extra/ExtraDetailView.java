package com.game.shooting_game.stageScene.extra;

import static com.game.shooting_game.ui.UIStyle.px;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.domain.module.ModuleKind;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.ModuleIcons;
import com.game.shooting_game.ui.ResourceIcons;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.ui.UIStyle;

/**
 * エクストラのステージ詳細の中身(説明・条件・ボスのHP・報酬・ハイスコア)
 * 文字の羅列をやめ、条件は札(回復・シールドは、アイテムの絵に斜線)、ボスのHPは絵とゲージの帯、
 * 報酬は素材・モジュールのアイコン(レア度の色の枠)にして見せる。
 */
public class ExtraDetailView extends JPanel {
    /** 条件1つ(iconは null なら文字だけ。bannedなら絵に斜線を引く) */
    public record Condition(String text, String icon, boolean banned) {
    }

    /** ボス1体(gaugeHpText・totalText は倍率をかけたあとのHP) */
    public record BossInfo(BufferedImage image, String name, int gauges, String gaugeHpText, String totalText) {
    }

    /** 報酬1つ(絵・枠の色・下の文字・右上の札) */
    public record Reward(BufferedImage image, String fallback, Color ring, String label, String badge) {
        public static Reward module(ModuleKind kind, Rarity rarity, int percent, String badge, int iconSize) {
            return new Reward(kind == null ? null : ModuleIcons.get(kind, iconSize), kind == null ? "?" : kind.getShortName(),
                    CostPanel.rarityColor(rarity), percent + "%", badge);
        }

        public static Reward resource(ResourceKinds kind, String range, int iconSize) {
            return new Reward(ResourceIcons.get(kind, iconSize), kind.getResName().getDisplayName(),
                    CostPanel.rarityColor(kind.getRarity()), range, null);
        }
    }

    private static final Color HEADING_LINE = new Color(110, 210, 255, 140);
    private static final Color CHIP_BG = new Color(255, 255, 255, 28);
    private static final Color BAN = new Color(255, 90, 90);

    private final int viewWidth;
    private String description = "";
    private final List<Condition> conditions = new ArrayList<>();
    private final List<BossInfo> bosses = new ArrayList<>();
    private final List<Reward> rewards = new ArrayList<>();
    private int highScore;

    // レイアウトの結果
    private final List<String> descLines = new ArrayList<>();
    private int descY, condHeadingY, bossHeadingY, rewardHeadingY, scoreY, totalHeight;
    private final List<java.awt.Rectangle> condChips = new ArrayList<>();
    private final List<Integer> bossRowYs = new ArrayList<>();
    private final List<java.awt.Rectangle> rewardCells = new ArrayList<>();
    private final int bossRowH = px(54);
    private final int cellW = px(62);
    private final int cellH = px(66);
    private final int icon = px(34);

    public ExtraDetailView(int viewWidth) {
        this.viewWidth = viewWidth;
        setOpaque(false);
    }

    public int iconSize() {
        return icon;
    }

    /**
     * 表示する内容の設定
     */
    public void setData(String description, List<Condition> conditions, List<BossInfo> bosses, List<Reward> rewards,
            int highScore) {
        this.description = description == null ? "" : description;
        this.conditions.clear();
        this.conditions.addAll(conditions);
        this.bosses.clear();
        this.bosses.addAll(bosses);
        this.rewards.clear();
        this.rewards.addAll(rewards);
        this.highScore = highScore;
        layoutItems();
        setPreferredSize(new Dimension(viewWidth, totalHeight));
        revalidate();
        repaint();
    }

    private void layoutItems() {
        int pad = px(8);
        int y = pad;

        // 説明(折り返す)
        FontMetrics bfm = getFontMetrics(UIStyle.font(12, false));
        descLines.clear();
        for (String para : description.split("\n"))
            wrap(para, bfm, viewWidth - 2 * pad, descLines);
        descY = y;
        y += descLines.size() * (bfm.getHeight() + px(2)) + px(8);

        // 条件の札
        condChips.clear();
        if (!conditions.isEmpty()) {
            condHeadingY = y;
            y += px(24);
            int chipH = px(26);
            int x = pad;
            for (Condition c : conditions) {
                int w = bfm.stringWidth(c.text()) + px(14) + (c.icon() == null ? 0 : px(22));
                if (x + w > viewWidth - pad && x > pad) {
                    x = pad;
                    y += chipH + px(5);
                }
                condChips.add(new java.awt.Rectangle(x, y, w, chipH));
                x += w + px(5);
            }
            y += px(26) + px(8);
        }

        // ボスのHP
        bossRowYs.clear();
        if (!bosses.isEmpty()) {
            bossHeadingY = y;
            y += px(24);
            for (int i = 0; i < bosses.size(); i++) {
                bossRowYs.add(y);
                y += bossRowH + px(4);
            }
            y += px(6);
        }

        // 報酬
        rewardHeadingY = y;
        y += px(24);
        int cols = Math.max(1, (viewWidth - 2 * pad) / cellW);
        int gridX = pad + (viewWidth - 2 * pad - cols * cellW) / 2;
        rewardCells.clear();
        for (int i = 0; i < rewards.size(); i++)
            rewardCells.add(new java.awt.Rectangle(gridX + (i % cols) * cellW, y + (i / cols) * cellH, cellW, cellH));
        y += ((rewards.size() + cols - 1) / cols) * cellH + px(10);

        // ハイスコア
        scoreY = y;
        y += px(26) + pad;
        totalHeight = y;
    }

    /** 文字数で折り返す(日本語は単語の切れ目がないので、1文字ずつ幅を測る) */
    private static void wrap(String text, FontMetrics fm, int width, List<String> out) {
        StringBuilder line = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (fm.stringWidth(line.toString() + ch) > width && line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
            }
            line.append(ch);
        }
        out.add(line.toString());
    }

    private void heading(Graphics2D g2, String text, int y) {
        g2.setFont(UIStyle.font(13, true));
        g2.setColor(UIStyle.HEADING);
        g2.drawString(text, px(8), y + px(14));
        g2.setColor(HEADING_LINE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(px(8), y + px(19), viewWidth - px(8), y + px(19));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int pad = px(8);

        // 説明
        g2.setFont(UIStyle.font(12, false));
        g2.setColor(new Color(205, 215, 230));
        FontMetrics bfm = g2.getFontMetrics();
        for (int i = 0; i < descLines.size(); i++)
            g2.drawString(descLines.get(i), pad, descY + bfm.getAscent() + i * (bfm.getHeight() + px(2)));

        // 条件
        if (!conditions.isEmpty()) {
            heading(g2, "条件", condHeadingY);
            for (int i = 0; i < conditions.size(); i++)
                drawCondition(g2, conditions.get(i), condChips.get(i), bfm);
        }

        // ボスのHP
        if (!bosses.isEmpty()) {
            heading(g2, "ボスのHP", bossHeadingY);
            for (int i = 0; i < bosses.size(); i++)
                drawBoss(g2, bosses.get(i), bossRowYs.get(i));
        }

        // 報酬
        heading(g2, "クリア報酬", rewardHeadingY);
        for (int i = 0; i < rewards.size(); i++)
            drawReward(g2, rewards.get(i), rewardCells.get(i));

        // ハイスコア
        g2.setFont(UIStyle.font(13, true));
        g2.setColor(new Color(255, 214, 80));
        g2.drawString("ハイスコア", pad, scoreY + px(16));
        g2.setColor(Color.WHITE);
        g2.drawString(highScore > 0 ? String.format("%,d", highScore) : "-",
                pad + g2.getFontMetrics().stringWidth("ハイスコア") + px(10), scoreY + px(16));
        g2.dispose();
    }

    private void drawCondition(Graphics2D g2, Condition c, java.awt.Rectangle r, FontMetrics fm) {
        g2.setColor(CHIP_BG);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(c.banned() ? new Color(255, 90, 90, 140) : new Color(255, 255, 255, 70));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        int tx = r.x + px(7);
        if (c.icon() != null) {
            int s = px(18);
            BufferedImage img = itemIcon(c.icon(), s);
            int ix = r.x + px(5), iy = r.y + (r.height - s) / 2;
            if (img != null)
                g2.drawImage(img, ix, iy, null);
            if (c.banned()) { // 禁止の斜線
                g2.setColor(BAN);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(ix, iy + s, ix + s, iy);
            }
            tx = r.x + px(5) + s + px(5);
        }
        g2.setFont(UIStyle.font(12, false));
        g2.setColor(Color.WHITE);
        g2.drawString(c.text(), tx, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
    }

    private static final java.util.Map<String, BufferedImage> itemIcons = new java.util.HashMap<>();

    private static synchronized BufferedImage itemIcon(String name, int size) {
        String key = name + "@" + size;
        if (!itemIcons.containsKey(key)) {
            BufferedImage img = null;
            try {
                // アイテムの絵は 4x4 のスプライトシート(15コマ)なので、1コマ目だけ使う
                BufferedImage sheet = UIBuilder.loadImage("/static/img/item/" + name + ".png");
                img = UIBuilder.resizeImage(UIBuilder.cutSpriteSheet(sheet, new com.game.shooting_game.utils.Vector2(4, 4), 15)[0],
                        size);
            } catch (java.io.IOException | RuntimeException e) {
                // 絵がなければ文字だけの札になる
            }
            itemIcons.put(key, img);
        }
        return itemIcons.get(key);
    }

    private void drawBoss(Graphics2D g2, BossInfo b, int y) {
        int pad = px(8);
        int w = viewWidth - 2 * pad;
        g2.setColor(CHIP_BG);
        g2.fillRoundRect(pad, y, w, bossRowH, 12, 12);

        // ボスの絵
        int s = bossRowH - px(8);
        if (b.image() != null) {
            double sc = Math.min((double) s / b.image().getWidth(), (double) s / b.image().getHeight());
            int iw = (int) (b.image().getWidth() * sc), ih = (int) (b.image().getHeight() * sc);
            g2.drawImage(b.image(), pad + px(4) + (s - iw) / 2, y + (bossRowH - ih) / 2, iw, ih, null);
        }
        int tx = pad + px(4) + s + px(8);

        // ゲージの帯(1ゲージ = 1マス)
        int barW = Math.min(px(120), w - (tx - pad) - px(8));
        int gap = px(2);
        int seg = Math.max(px(4), (barW - gap * (b.gauges() - 1)) / Math.max(1, b.gauges()));
        for (int i = 0; i < b.gauges(); i++) {
            g2.setColor(new Color(255, 90, 90));
            g2.fillRoundRect(tx + i * (seg + gap), y + px(8), seg, px(8), 4, 4);
        }
        g2.setFont(UIStyle.font(11, false));
        g2.setColor(new Color(200, 215, 235));
        g2.drawString(b.gauges() + " ゲージ", tx + b.gauges() * (seg + gap) + px(4), y + px(16));

        // HP(1ゲージ分を大きく、合計を小さく)
        g2.setFont(UIStyle.font(14, true));
        g2.setColor(Color.WHITE);
        g2.drawString("HP " + b.gaugeHpText() + " ×" + b.gauges(), tx, y + px(34));
        g2.setFont(UIStyle.font(11, false));
        g2.setColor(new Color(170, 185, 205));
        g2.drawString("合計 " + b.totalText(), tx, y + px(48));
    }

    private void drawReward(Graphics2D g2, Reward r, java.awt.Rectangle a) {
        int ix = a.x + (a.width - icon) / 2;
        int iy = a.y + px(2);
        g2.setColor(new Color(r.ring().getRed(), r.ring().getGreen(), r.ring().getBlue(), 50));
        g2.fillRoundRect(ix - 2, iy - 2, icon + 4, icon + 4, 10, 10);
        if (r.image() != null)
            g2.drawImage(r.image(), ix + (icon - r.image().getWidth()) / 2, iy + (icon - r.image().getHeight()) / 2, null);
        else {
            g2.setFont(UIStyle.font(13, true));
            g2.setColor(Color.WHITE);
            String s = r.fallback().length() > 3 ? r.fallback().substring(0, 3) : r.fallback();
            g2.drawString(s, ix + (icon - g2.getFontMetrics().stringWidth(s)) / 2, iy + icon / 2 + px(5));
        }
        g2.setColor(r.ring());
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(ix - 2, iy - 2, icon + 4, icon + 4, 10, 10);

        // 右上の札(初回・毎日)
        if (r.badge() != null) {
            g2.setFont(UIStyle.font(9, true));
            FontMetrics fm = g2.getFontMetrics();
            int bw = fm.stringWidth(r.badge()) + px(6), bh = fm.getHeight() - 2;
            int bx = ix + icon - bw + px(4), by = iy - px(4);
            g2.setColor(new Color(90, 230, 140, 235));
            g2.fillRoundRect(bx, by, bw, bh, 6, 6);
            g2.setColor(Color.BLACK);
            g2.drawString(r.badge(), bx + px(3), by + fm.getAscent() - 1);
        }

        // 下の文字(確率・個数)
        g2.setFont(UIStyle.font(11, true));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(r.label(), a.x + (a.width - fm.stringWidth(r.label())) / 2, iy + icon + px(14));
    }
}
