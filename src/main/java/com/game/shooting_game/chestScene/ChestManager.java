package com.game.shooting_game.chestScene;

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
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.chest.ChestService;
import com.game.shooting_game.domain.chest.ChestService.Opened;
import com.game.shooting_game.domain.chest.ChestService.Rank;
import com.game.shooting_game.domain.shop.ShopService;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * 宝箱シーン
 * 上に鍵の所持数と「開ける」(鍵を使って宝箱を開け始める)、下に開けている宝箱のスロット(最大10個)を並べる。
 * 開き終わった宝箱は「受け取る」で中身をもらい、開いている最中の宝箱は研究スキップ券ですぐ開けられる。
 */
public final class ChestManager extends JPanel implements Manager {
    private static final String component = "[ChestManager]";
    private static ChestManager instance;
    private static final Color[] RANK_COLORS = { new Color(205, 127, 50), new Color(200, 210, 225),
            new Color(255, 205, 60) };

    private final ShootingGameMain MAIN;
    private JButton goBackBtn;
    private String message = "";
    private double repaintTimer = 0;
    private final Random rnd = new Random();
    private final List<Object[]> hitAreas = new ArrayList<>(); // {判定の四角, 種類("open"/"skip"/"claim"), 値} 描画のたびに作り直す

    private ChestManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    public static ChestManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, ChestManager::new);
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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (Object[] a : new ArrayList<>(hitAreas))
                    if (((Rectangle) a[0]).contains(e.getPoint())) {
                        handle((String) a[1], (int) a[2]);
                        break;
                    }
            }
        });
    }

    private void handle(String kind, int value) {
        int now = ChestService.nowSec();
        switch (kind) {
            case "open" -> {
                Rank rank = Rank.values()[value];
                if (ChestService.open(rank, now) >= 0)
                    message = rank.getLabel() + "の宝箱を開け始めた(" + ChestService.formatTime(ChestService.openSeconds(rank)) + ")";
                else if (ChestService.keyCount(rank) <= 0)
                    message = rank.getLabel() + "の鍵がありません";
                else
                    message = "宝箱のスロットがいっぱいです(最大" + ChestService.MAX_SLOTS + "個)";
            }
            case "skip" -> message = ChestService.skip(value, now) ? "研究スキップ券を使って、すぐ開けた" : "研究スキップ券がありません";
            case "claim" -> {
                Opened o = ChestService.claim(value, now, rnd);
                if (o != null) {
                    StringBuilder sb = new StringBuilder("宝箱の中身: コイン " + o.loot().coins());
                    for (Map.Entry<ResourceKinds, Integer> e : o.loot().resources().entrySet())
                        sb.append(" / ").append(e.getKey().getResName().getDisplayName()).append(" ×").append(e.getValue());
                    if (!o.modules().isEmpty())
                        sb.append(" / モジュール ×").append(o.modules().size());
                    message = sb.toString();
                }
            }
            default -> {
            }
        }
        repaint();
    }

    @Override
    public void setup() {
        message = "";
        repaintTimer = 0;
    }

    @Override
    public void update(double dt) {
        // 残り時間の表示を、1秒ごとに描き直す
        repaintTimer += dt;
        if (repaintTimer >= 1.0) {
            repaintTimer = 0;
            repaint();
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hitAreas.clear();
        int now = ChestService.nowSec();
        int w = MAIN.getWidth();

        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), new Vector2(w / 2, px(12)), px(24), Color.WHITE,
                Font.BOLD, "宝箱");

        // 鍵(ランクごとに、所持数と「開ける」)
        int top = px(54), keyH = px(64), gap = px(8), margin = px(10);
        int keyW = (w - 2 * margin - 2 * gap) / 3;
        for (Rank rank : Rank.values()) {
            int i = rank.ordinal();
            int x = margin + i * (keyW + gap);
            int count = ChestService.keyCount(rank);
            drawCard(g2, x, top, keyW, keyH, RANK_COLORS[i]);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(14))));
            g2.setColor(RANK_COLORS[i]);
            g2.drawString(rank.getLabel() + "の鍵", x + px(8), top + px(20));
            g2.setColor(Color.WHITE);
            g2.drawString("×" + count, x + keyW - px(8) - g2.getFontMetrics().stringWidth("×" + count), top + px(20));
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
            g2.setColor(new Color(170, 205, 235));
            g2.drawString(ChestService.formatTime(ChestService.openSeconds(rank)), x + px(8), top + px(36));
            boolean can = count > 0 && ChestService.freeSlot() >= 0;
            drawButton(g2, x + px(6), top + keyH - px(26), keyW - px(12), px(20), "開ける", can);
            hitAreas.add(new Object[] { new Rectangle(x + px(6), top + keyH - px(26), keyW - px(12), px(20)), "open", i });
        }

        // スロット(開けている宝箱。2列×5行)
        int slotTop = top + keyH + px(14);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
        g2.setColor(new Color(150, 200, 230));
        g2.drawString("開けている宝箱 " + ChestService.usedSlots() + " / " + ChestService.MAX_SLOTS, margin, slotTop);
        int rows = ChestService.MAX_SLOTS / 2;
        int slotAreaTop = slotTop + px(8);
        int slotH = Math.min(px(66), (MAIN.getHeight() - slotAreaTop - px(110)) / rows - gap);
        int slotW = (w - 2 * margin - gap) / 2;
        for (int s = 0; s < ChestService.MAX_SLOTS; s++) {
            int x = margin + (s % 2) * (slotW + gap);
            int y = slotAreaTop + (s / 2) * (slotH + gap);
            drawSlot(g2, s, x, y, slotW, slotH, now);
        }

        // メッセージ(下の欄)
        if (!message.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
            g2.setColor(new Color(255, 230, 120));
            FontMetrics fm = g2.getFontMetrics();
            String m = message;
            while (fm.stringWidth(m) > w - px(24) && m.length() > 4)
                m = m.substring(0, m.length() - 2);
            g2.drawString(m, px(12), MAIN.getHeight() - px(66));
        }
        // 研究スキップ券の所持数
        g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(11))));
        g2.setColor(new Color(190, 220, 255));
        String t = "研究スキップ券 ×" + ShopService.owned(ItemKinds.SKIP_TICKET);
        g2.drawString(t, w - px(12) - g2.getFontMetrics().stringWidth(t), MAIN.getHeight() - px(18));
    }

    private void drawSlot(Graphics2D g2, int slot, int x, int y, int w, int h, int now) {
        Rank rank = ChestService.slotRank(slot);
        boolean ready = ChestService.isReady(slot, now);
        drawCard(g2, x, y, w, h, rank == null ? new Color(70, 80, 100) : RANK_COLORS[rank.ordinal()]);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
        if (rank == null) {
            g2.setColor(new Color(110, 120, 140));
            g2.drawString("空き", x + px(10), y + h / 2 + px(4));
            return;
        }
        g2.setColor(RANK_COLORS[rank.ordinal()]);
        g2.drawString(rank.getLabel() + "の宝箱", x + px(8), y + px(18));
        int remain = ChestService.remaining(slot, now);
        // 進み具合のバー
        int total = Math.max(1, ChestService.openSeconds(rank));
        int barX = x + px(8), barY = y + px(26), barW = w - px(16), barH = px(7);
        g2.setColor(new Color(50, 60, 80));
        g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
        g2.setColor(ready ? new Color(110, 200, 130) : new Color(70, 215, 255));
        g2.fillRoundRect(barX, barY, (int) (barW * Math.min(1.0, 1.0 - (double) remain / total)), barH, barH, barH);
        g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
        g2.setColor(Color.WHITE);
        g2.drawString(ready ? "開いた!" : ChestService.formatTime(remain), barX, barY + barH + px(13));
        int by = y + h - px(22), bh = px(18);
        if (ready) {
            drawButton(g2, x + w - px(76), by, px(68), bh, "受け取る", true);
            hitAreas.add(new Object[] { new Rectangle(x + w - px(76), by, px(68), bh), "claim", slot });
        } else {
            drawButton(g2, x + w - px(76), by, px(68), bh, "即開封", true);
            hitAreas.add(new Object[] { new Rectangle(x + w - px(76), by, px(68), bh), "skip", slot });
        }
    }

    private void drawCard(Graphics2D g2, int x, int y, int w, int h, Color edge) {
        g2.setColor(new Color(10, 20, 40, 200));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, px(12), px(12)));
        g2.setStroke(new BasicStroke((float) px(2)));
        g2.setColor(edge);
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, px(12), px(12)));
    }

    private void drawButton(Graphics2D g2, int x, int y, int w, int h, String label, boolean enabled) {
        g2.setColor(enabled ? new Color(255, 190, 40) : new Color(60, 70, 90));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, px(8), px(8)));
        g2.setColor(enabled ? new Color(40, 24, 0) : new Color(140, 150, 170));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, px(11))));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + (h + fm.getAscent()) / 2 - px(2));
    }
}
