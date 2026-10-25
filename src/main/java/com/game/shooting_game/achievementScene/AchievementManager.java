package com.game.shooting_game.achievementScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.achievement.AchievementService;
import com.game.shooting_game.domain.achievement.AchievementService.Achievement;
import com.game.shooting_game.domain.achievement.AchievementService.Tier;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * 実績シーン
 * 実績を縦に並べ、目標までの進み具合と、受け取れる報酬を見せる。受け取れるものは「受け取る」で受け取る。
 */
public final class AchievementManager extends JPanel implements Manager {
    private static final String component = "[AchievementManager]";
    private static AchievementManager instance;

    private final ShootingGameMain MAIN;
    private JButton goBackBtn;
    private double scrollOffset = 0; // スクロール量(0以下)
    private double maxScroll = 0; // 最大スクロール量(正)
    private String message = ""; // 受け取りの結果
    private final List<Object[]> claimAreas = new ArrayList<>(); // {判定の四角, 実績} 描画のたびに作り直す

    private AchievementManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    public static AchievementManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, AchievementManager::new);
    }

    private static int px(double base) {
        return (int) (base * ShootingGameMain.getScreenSizeAdj());
    }

    @Override
    public void initComponents() {
        setLayout(null);
        setOpaque(false);

        goBackBtn = UIBuilder.createBackBtn(component, () -> SceneManager.getInstance().changeToPrevScene());
        this.add(goBackBtn);

        addMouseWheelListener(e -> {
            scrollOffset = Math.clamp(scrollOffset - e.getPreciseWheelRotation() * px(24), -maxScroll, 0);
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (Object[] a : new ArrayList<>(claimAreas))
                    if (((Rectangle) a[0]).contains(e.getPoint()))
                        claim((Achievement) a[1]);
            }
        });
    }

    private void claim(Achievement a) {
        Reward r = AchievementService.claim(a);
        if (r == null)
            return;
        StringBuilder sb = new StringBuilder("「" + a.title() + "」の報酬: コイン " + r.coins());
        for (Map.Entry<ResourceKinds, Integer> e : r.resources().entrySet())
            sb.append(" / ").append(e.getKey().getResName().getDisplayName()).append(" ×").append(e.getValue());
        if (r.skipTickets() > 0)
            sb.append(" / 研究スキップ券 ×").append(r.skipTickets());
        message = sb.toString();
        repaint();
    }

    @Override
    public void setup() {
        scrollOffset = 0;
        message = "";
    }

    @Override
    public void update(double dt) {
    }

    private Rectangle listArea() {
        return new Rectangle(px(10), px(60), MAIN.getWidth() - px(20), MAIN.getHeight() - px(60) - px(96));
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();

        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), new Vector2(MAIN.getWidth() / 2, 12 * adj), 24 * adj,
                Color.WHITE, Font.BOLD, "実績");

        claimAreas.clear();
        Rectangle area = listArea();
        java.awt.Shape oldClip = g2.getClip();
        g2.setClip(area);

        int rowH = px(86), gap = px(8);
        List<Achievement> list = AchievementService.all();
        int y = area.y + (int) scrollOffset;
        for (Achievement a : list) {
            drawRow(g2, a, area.x, y, area.width, rowH);
            y += rowH + gap;
        }
        g2.setClip(oldClip);
        maxScroll = Math.max(0, list.size() * (rowH + gap) - area.height);

        // 受け取りの結果(下の欄)
        if (!message.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
            g2.setColor(new Color(255, 230, 120));
            FontMetrics fm = g2.getFontMetrics();
            String m = message;
            while (fm.stringWidth(m) > MAIN.getWidth() - px(24) && m.length() > 4)
                m = m.substring(0, m.length() - 2);
            g2.drawString(m, px(12), area.y + area.height + px(18));
        }
    }

    private void drawRow(Graphics2D g2, Achievement a, int x, int y, int w, int h) {
        int reached = AchievementService.reachedTiers(a);
        int claimed = AchievementService.claimedTiers(a);
        boolean can = reached > claimed;
        int total = a.tiers().size();
        boolean allDone = claimed >= total;
        Tier next = AchievementService.nextTier(a);
        int raw = AchievementService.value(a);

        // カード
        g2.setColor(new Color(10, 20, 40, 200));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, px(14), px(14)));
        g2.setStroke(new BasicStroke((float) px(2)));
        g2.setColor(can ? new Color(255, 214, 80) : allDone ? new Color(110, 200, 130) : new Color(70, 215, 255, 160));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, px(14), px(14)));

        // 題と説明
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(15))));
        g2.drawString(a.title(), x + px(12), y + px(22));
        g2.setColor(new Color(170, 205, 235));
        g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g2.drawString(a.description(), x + px(12), y + px(40));

        // 進み具合(次の目標までのバー)
        int barX = x + px(12), barY = y + px(52), barW = w - px(24) - px(104), barH = px(10);
        g2.setColor(new Color(50, 60, 80));
        g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
        double ratio = next == null ? 1.0 : Math.min(1.0, (double) raw / next.goal());
        g2.setColor(allDone ? new Color(110, 200, 130) : new Color(70, 215, 255));
        g2.fillRoundRect(barX, barY, (int) (barW * ratio), barH, barH, barH);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
        String prog = next == null ? "達成!"
                : AchievementService.shown(a, raw) + " / " + AchievementService.shown(a, next.goal()) + " " + a.unit();
        g2.drawString(prog, barX, barY + barH + px(15));

        // 段の印(受け取り済みは塗る、届いたが未受取は金の枠)
        int dot = px(9), dx = barX + barW - total * (dot + px(3));
        for (int i = 0; i < total; i++) {
            int cx = dx + i * (dot + px(3)), cy = y + px(16);
            if (i < claimed)
                g2.setColor(new Color(110, 200, 130));
            else if (i < reached)
                g2.setColor(new Color(255, 214, 80));
            else
                g2.setColor(new Color(70, 80, 100));
            g2.fillOval(cx, cy - dot / 2, dot, dot);
        }

        // 受け取るボタン
        int bx = x + w - px(92), by = y + px(24), bw = px(80), bh = px(34);
        g2.setColor(can ? new Color(255, 190, 40) : new Color(60, 70, 90));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, px(10), px(10)));
        g2.setColor(can ? new Color(40, 24, 0) : new Color(140, 150, 170));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(13))));
        String label = can ? "受け取る" : allDone ? "受取済" : claimed + "/" + total;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, bx + (bw - fm.stringWidth(label)) / 2, by + (bh + fm.getAscent()) / 2 - px(2));
        if (can)
            claimAreas.add(new Object[] { new Rectangle(bx, by, bw, bh).intersection(listArea()), a });

        // 次の段の報酬の見出し
        if (next != null) {
            g2.setColor(new Color(255, 214, 80));
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
            String r = "次の報酬: コイン " + next.reward().coins()
                    + (next.reward().skipTickets() > 0 ? " + スキップ券" : "") + " ほか";
            g2.drawString(r, bx - px(4) - Math.max(0, g2.getFontMetrics().stringWidth(r) - bw - px(4)),
                    by + bh + px(14));
        }
    }
}
