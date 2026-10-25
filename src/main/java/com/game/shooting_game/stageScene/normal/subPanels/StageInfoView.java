package com.game.shooting_game.stageScene.normal.subPanels;

import static com.game.shooting_game.ui.UIStyle.px;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;
import com.game.shooting_game.gameScene.stage.DifficultyService.Mission;
import com.game.shooting_game.ui.CostPanel;
import com.game.shooting_game.ui.ResourceIcons;
import com.game.shooting_game.ui.UIStyle;

/**
 * ステージ詳細の中身(ハイスコア・報酬・出現する敵)
 * 報酬は、素材のアイコン(レア度の色の枠)の下に「最小〜最大」の個数を並べる。敵は札にして折り返す。
 */
public class StageInfoView extends JPanel {
    /** 報酬1種(素材と、もらえる個数の範囲) */
    public record Reward(ResourceKinds kind, String range) {
    }

    private record Cell(Rectangle area, Reward reward) {
    }

    private static final Color HEADING_LINE = new Color(110, 210, 255, 140);
    private static final Color CHIP_BG = new Color(255, 255, 255, 28);

    private final int viewWidth;
    private int highScore;
    private final List<Reward> rewards = new ArrayList<>();
    private final List<String> enemies = new ArrayList<>();
    private final List<Mission> missions = new ArrayList<>(); // 空なら非表示
    private int missionStars;
    private int missionHeadingY;
    private final List<Integer> missionRowYs = new ArrayList<>();

    // レイアウトの結果(setDataで作る)
    private final List<Cell> cells = new ArrayList<>();
    private final List<Rectangle> enemyChips = new ArrayList<>();
    private int rewardHeadingY;
    private int enemyHeadingY;
    private int scoreY;
    private int totalHeight;
    private int powerY; // 戦力値の行のY(requiredPower が0以下なら出さない)
    private int requiredPower; // 推定必要戦力値(0以下なら出さない)
    private int ownPower; // 自分の戦力値

    public StageInfoView(int viewWidth) {
        this.viewWidth = viewWidth;
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * 表示する内容の設定
     */
    public void setData(int highScore, List<Reward> rewards, List<String> enemies) {
        this.highScore = highScore;
        this.rewards.clear();
        this.rewards.addAll(rewards);
        this.enemies.clear();
        this.enemies.addAll(enemies);
        layoutItems();
        setPreferredSize(new Dimension(viewWidth, totalHeight));
        revalidate();
        repaint();
    }

    /**
     * 戦力値の表示の設定(推定必要戦力値が0以下なら、この行を出さない)
     */
    public void setPower(int required, int own) {
        this.requiredPower = required;
        this.ownPower = own;
        layoutItems();
        setPreferredSize(new Dimension(viewWidth, totalHeight));
        revalidate();
        repaint();
    }

    /**
     * 難易度ごとのミッション表示の設定(空のリストなら、ミッションの項目を出さない)
     */
    public void setMissions(List<Mission> missions, int stars) {
        this.missions.clear();
        if (missions != null)
            this.missions.addAll(missions);
        this.missionStars = stars;
        layoutItems();
        setPreferredSize(new Dimension(viewWidth, totalHeight));
        revalidate();
        repaint();
    }

    private Font headingFont() {
        return UIStyle.font(13, true);
    }

    private Font bodyFont() {
        return UIStyle.font(12, false);
    }

    private void layoutItems() {
        cells.clear();
        enemyChips.clear();
        int pad = px(8);
        int y = pad;

        // ハイスコア
        scoreY = y;
        y += px(26);

        // 戦力値(推定必要戦力値と自分の戦力値)
        powerY = y;
        if (requiredPower > 0)
            y += px(26);

        // ミッション
        missionRowYs.clear();
        if (!missions.isEmpty()) {
            missionHeadingY = y;
            y += px(24);
            for (int i = 0; i < missions.size(); i++) {
                missionRowYs.add(y);
                y += px(20);
            }
            y += px(8);
        }

        // 報酬(素材のアイコンを格子に並べる)
        rewardHeadingY = y;
        y += px(24);
        int cellW = px(60);
        int cellH = px(64);
        int cols = Math.max(1, (viewWidth - 2 * pad) / cellW);
        int gridX = pad + (viewWidth - 2 * pad - cols * cellW) / 2;
        for (int i = 0; i < rewards.size(); i++) {
            Rectangle r = new Rectangle(gridX + (i % cols) * cellW, y + (i / cols) * cellH, cellW, cellH);
            cells.add(new Cell(r, rewards.get(i)));
        }
        int rows = (rewards.size() + cols - 1) / cols;
        y += rows * cellH + px(10);

        // 出現する敵(札を折り返して並べる)
        enemyHeadingY = y;
        y += px(24);
        FontMetrics fm = getFontMetrics(bodyFont());
        int chipH = fm.getHeight() + px(6);
        int x = pad;
        for (String e : enemies) {
            int w = fm.stringWidth(e) + px(14);
            if (x + w > viewWidth - pad && x > pad) {
                x = pad;
                y += chipH + px(5);
            }
            enemyChips.add(new Rectangle(x, y, w, chipH));
            x += w + px(5);
        }
        y += (enemies.isEmpty() ? 0 : chipH) + pad;
        totalHeight = y;
    }

    private void heading(Graphics2D g2, String text, int y) {
        g2.setFont(headingFont());
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

        // ハイスコア
        g2.setFont(headingFont());
        g2.setColor(new Color(255, 214, 80));
        g2.drawString("ハイスコア", px(8), scoreY + px(16));
        g2.setColor(Color.WHITE);
        String score = highScore > 0 ? String.format("%,d", highScore) : "-";
        g2.drawString(score, px(8) + g2.getFontMetrics().stringWidth("ハイスコア") + px(10), scoreY + px(16));

        // 戦力値(足りる=緑 / ぎりぎり=黄 / 不足=赤)
        if (requiredPower > 0) {
            g2.setFont(headingFont());
            g2.setColor(new Color(255, 214, 80));
            String head = "推定必要戦力値";
            g2.drawString(head, px(8), powerY + px(16));
            int x = px(8) + g2.getFontMetrics().stringWidth(head) + px(10);
            g2.setColor(Color.WHITE);
            String req = String.format("%,d", requiredPower);
            g2.drawString(req, x, powerY + px(16));
            x += g2.getFontMetrics().stringWidth(req) + px(10);
            var verdict = com.game.shooting_game.domain.power.PowerService.verdict(ownPower, requiredPower);
            g2.setColor(switch (verdict) {
                case ENOUGH -> new Color(110, 220, 140);
                case BARELY -> new Color(255, 214, 80);
                case SHORT -> new Color(255, 110, 110);
            });
            g2.drawString("あなた " + String.format("%,d", ownPower) + switch (verdict) {
                case ENOUGH -> "  十分";
                case BARELY -> "  ぎりぎり";
                case SHORT -> "  不足";
            }, x, powerY + px(16));
        }

        // ミッション(★は黄色、☆は灰色。フォントに依存しないよう多角形で描く)
        if (!missions.isEmpty()) {
            int sz = px(14);
            heading(g2, "ミッション", missionHeadingY);
            for (int i = 0; i < 3; i++)
                drawStar(g2, px(8) + px(80) + i * (sz + px(2)), missionHeadingY + px(15) - sz, sz,
                        i < missionStars);
            g2.setFont(bodyFont());
            FontMetrics mfm = g2.getFontMetrics();
            for (int i = 0; i < missions.size(); i++) {
                Mission m = missions.get(i);
                int ry = missionRowYs.get(i);
                drawStar(g2, px(10), ry + (px(20) - sz) / 2, sz, m.done());
                g2.setFont(bodyFont());
                g2.setColor(m.done() ? Color.WHITE : new Color(180, 180, 180));
                g2.drawString(m.text(), px(10) + sz + px(6), ry + (px(20) + mfm.getAscent() - mfm.getDescent()) / 2);
            }
        }

        // 報酬
        heading(g2, "報酬(ステージごとに増減します)", rewardHeadingY);
        if (cells.isEmpty()) {
            g2.setFont(bodyFont());
            g2.setColor(new Color(170, 170, 170));
            g2.drawString("報酬の情報がありません", px(8), rewardHeadingY + px(42));
        }
        int icon = px(34);
        for (Cell c : cells) {
            drawReward(g2, c, icon);
        }

        // 出現する敵
        heading(g2, "出現する敵(" + enemies.size() + "種類)", enemyHeadingY);
        g2.setFont(bodyFont());
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < enemyChips.size(); i++) {
            Rectangle r = enemyChips.get(i);
            g2.setColor(CHIP_BG);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(enemies.get(i), r.x + px(7), r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
    }

    private void drawStar(Graphics2D g2, int x, int y, int size, boolean filled) {
        Polygon p = new Polygon();
        double cx = x + size / 2.0, cy = y + size / 2.0, ro = size / 2.0, ri = ro * 0.45;
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? ro : ri;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            p.addPoint((int) Math.round(cx + r * Math.cos(a)), (int) Math.round(cy + r * Math.sin(a)));
        }
        if (filled) {
            g2.setColor(new Color(255, 214, 80));
            g2.fillPolygon(p);
        } else {
            g2.setColor(new Color(150, 150, 150));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawPolygon(p);
        }
    }

    private void drawReward(Graphics2D g2, Cell c, int icon) {
        Rarity rarity = c.reward().kind().getRarity();
        Color ring = CostPanel.rarityColor(rarity);
        Rectangle a = c.area();
        int ix = a.x + (a.width - icon) / 2;
        int iy = a.y + px(2);

        g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 50));
        g2.fillRoundRect(ix - 2, iy - 2, icon + 4, icon + 4, 10, 10);
        BufferedImage img = ResourceIcons.get(c.reward().kind(), icon);
        if (img != null)
            g2.drawImage(img, ix + (icon - img.getWidth()) / 2, iy + (icon - img.getHeight()) / 2, null);
        g2.setColor(ring);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(ix - 2, iy - 2, icon + 4, icon + 4, 10, 10);

        // 個数の範囲(アイコンの下)
        g2.setFont(UIStyle.font(11, true));
        FontMetrics fm = g2.getFontMetrics();
        String text = c.reward().range();
        g2.setColor(Color.WHITE);
        g2.drawString(text, a.x + (a.width - fm.stringWidth(text)) / 2, iy + icon + px(14));
    }

    @Override
    public String getToolTipText(MouseEvent ev) {
        for (Cell c : cells) {
            if (c.area().contains(ev.getPoint())) {
                ResourceKinds k = c.reward().kind();
                return k.getResName().getDisplayName() + "(" + CostPanel.rarityName(k.getRarity()) + ")  "
                        + c.reward().range() + "個";
            }
        }
        return null;
    }
}
