package com.game.shooting_game.idleScene;

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
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.idle.IdleService;
import com.game.shooting_game.domain.power.PowerService;
import com.game.shooting_game.domain.power.PowerService.Verdict;
import com.game.shooting_game.domain.star.StarRewardService.Reward;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * 放置シーン
 * 上に、いまの放置先とたまった報酬(受け取る・やめる)、下に、放置先を選ぶ欄(ステージ番号を送って選ぶ)。
 * アプリを閉じている間も、時間がたつほど報酬がたまる(上限あり)。
 */
public final class IdleManager extends JPanel implements Manager {
    private static final String component = "[IdleManager]";
    private static IdleManager instance;

    private final ShootingGameMain MAIN;
    private JButton goBackBtn;
    private int candidate = 1; // 選ぶ欄で見ているステージ
    private String message = "";
    private double repaintTimer = 0;
    private final List<Object[]> hitAreas = new ArrayList<>(); // {判定の四角, 種類, 値} 描画のたびに作り直す

    private IdleManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    public static IdleManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, IdleManager::new);
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

    private int maxStage() {
        return Math.max(1, Math.min(200, DataManager.getUserData().getProgress().getClearedStage()));
    }

    private void handle(String kind, int value) {
        int now = IdleService.nowSec();
        switch (kind) {
            case "move" -> candidate = Math.max(1, Math.min(maxStage(), candidate + value));
            case "claim" -> {
                Reward r = IdleService.claim(now);
                message = r == null ? "" : "受け取り: " + describe(r);
            }
            case "stop" -> {
                Reward r = IdleService.pending(now);
                IdleService.stop(now);
                message = "放置をやめた(" + describe(r) + ")";
            }
            case "select" -> {
                if (IdleService.select(candidate, now))
                    message = "ステージ" + candidate + "で放置を始めた";
                else
                    message = "このステージは選べません(戦力値が足りないか、クリアしていません)";
            }
            default -> {
            }
        }
        repaint();
    }

    private static String describe(Reward r) {
        StringBuilder sb = new StringBuilder("コイン " + r.coins());
        for (Map.Entry<ResourceKinds, Integer> e : r.resources().entrySet())
            sb.append(" / ").append(e.getKey().getResName().getDisplayName()).append(" ×").append(e.getValue());
        return sb.toString();
    }

    @Override
    public void setup() {
        message = "";
        repaintTimer = 0;
        candidate = IdleService.stage() > 0 ? IdleService.stage() : maxStage();
    }

    @Override
    public void update(double dt) {
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
        int now = IdleService.nowSec();
        int w = MAIN.getWidth();
        int margin = px(10);

        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), new Vector2(w / 2, px(12)), px(24), Color.WHITE,
                Font.BOLD, "放置");

        // いまの放置先とたまった報酬
        int top = px(54), h1 = px(190);
        drawCard(g2, margin, top, w - 2 * margin, h1, new Color(70, 215, 255));
        int stage = IdleService.stage();
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, px(15))));
        g2.setColor(Color.WHITE);
        if (stage <= 0) {
            g2.drawString("放置していません", margin + px(12), top + px(26));
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
            g2.setColor(new Color(170, 205, 235));
            g2.drawString("下で、放置するステージを選んでください。", margin + px(12), top + px(52));
        } else {
            g2.drawString("放置先: ステージ" + stage, margin + px(12), top + px(26));
            int el = IdleService.elapsed(now);
            boolean full = el >= IdleService.MAX_SECONDS;
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
            g2.setColor(new Color(170, 205, 235));
            g2.drawString("放置時間 " + format(el) + " / " + format(IdleService.MAX_SECONDS) + (full ? "  (上限)" : ""),
                    margin + px(12), top + px(48));
            int barX = margin + px(12), barY = top + px(56), barW = w - 2 * margin - px(24), barH = px(8);
            g2.setColor(new Color(50, 60, 80));
            g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
            g2.setColor(full ? new Color(255, 190, 40) : new Color(70, 215, 255));
            g2.fillRoundRect(barX, barY, (int) (barW * Math.min(1.0, (double) el / IdleService.MAX_SECONDS)), barH, barH,
                    barH);

            // たまった報酬
            Reward r = IdleService.pending(now);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(13))));
            g2.setColor(new Color(255, 214, 80));
            g2.drawString("たまった報酬", margin + px(12), top + px(88));
            g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
            g2.setColor(Color.WHITE);
            int y = top + px(106);
            g2.drawString("コイン " + String.format("%,d", r.coins()), margin + px(16), y);
            for (Map.Entry<ResourceKinds, Integer> e : r.resources().entrySet()) {
                y += px(16);
                if (y > top + h1 - px(40))
                    break;
                g2.drawString(e.getKey().getResName().getDisplayName() + " ×" + e.getValue(), margin + px(16), y);
            }
            int by = top + h1 - px(32), bw = (w - 2 * margin - px(36)) / 2;
            drawButton(g2, margin + px(12), by, bw, px(24), "受け取る", true);
            hitAreas.add(new Object[] { new Rectangle(margin + px(12), by, bw, px(24)), "claim", 0 });
            drawButton(g2, margin + px(24) + bw, by, bw, px(24), "やめる", true);
            hitAreas.add(new Object[] { new Rectangle(margin + px(24) + bw, by, bw, px(24)), "stop", 0 });
        }

        // 放置先を選ぶ欄
        int top2 = top + h1 + px(12), h2 = px(170);
        drawCard(g2, margin, top2, w - 2 * margin, h2, new Color(150, 200, 230));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, px(13))));
        g2.setColor(new Color(150, 200, 230));
        g2.drawString("放置先を選ぶ", margin + px(12), top2 + px(22));

        int btn = px(34), cy = top2 + px(44);
        int cx = w / 2;
        drawButton(g2, margin + px(12), cy, btn + px(14), btn, "-10", candidate > 1);
        hitAreas.add(new Object[] { new Rectangle(margin + px(12), cy, btn + px(14), btn), "move", -10 });
        drawButton(g2, margin + px(12) + btn + px(18), cy, btn, btn, "◀", candidate > 1);
        hitAreas.add(new Object[] { new Rectangle(margin + px(12) + btn + px(18), cy, btn, btn), "move", -1 });
        drawButton(g2, w - margin - px(12) - btn - px(14), cy, btn + px(14), btn, "+10", candidate < maxStage());
        hitAreas.add(new Object[] { new Rectangle(w - margin - px(12) - btn - px(14), cy, btn + px(14), btn), "move", 10 });
        drawButton(g2, w - margin - px(12) - 2 * btn - px(18) - px(14), cy, btn, btn, "▶", candidate < maxStage());
        hitAreas.add(new Object[] { new Rectangle(w - margin - px(12) - 2 * btn - px(18) - px(14), cy, btn, btn), "move", 1 });
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(13, px(17))));
        g2.setColor(Color.WHITE);
        String st = "ステージ" + candidate;
        g2.drawString(st, cx - g2.getFontMetrics().stringWidth(st) / 2, cy + px(24));

        // 戦力値
        double req = PowerService.required(candidate, Difficulty.NORMAL);
        double own = PowerService.currentPower();
        Verdict v = PowerService.verdict(own, req);
        g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, px(12))));
        g2.setColor(new Color(170, 205, 235));
        g2.drawString("推定必要戦力値 " + String.format("%,d", Math.round(req)), margin + px(16), top2 + px(104));
        g2.setColor(switch (v) {
            case ENOUGH -> new Color(110, 220, 140);
            case BARELY -> new Color(255, 214, 80);
            case SHORT -> new Color(255, 110, 110);
        });
        g2.drawString("あなた " + String.format("%,d", Math.round(own)), margin + px(16), top2 + px(122));

        boolean can = IdleService.canSelect(candidate);
        int sy = top2 + h2 - px(34);
        drawButton(g2, margin + px(12), sy, w - 2 * margin - px(24), px(26), can ? "このステージで放置する" : "選べません(戦力値不足・未クリア)", can);
        hitAreas.add(new Object[] { new Rectangle(margin + px(12), sy, w - 2 * margin - px(24), px(26)), "select", 0 });

        // 説明とメッセージ
        g2.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, px(10))));
        g2.setColor(new Color(150, 170, 190));
        int ny = top2 + h2 + px(18);
        g2.drawString("たまる量は実際の周回の" + Math.round(IdleService.EFFICIENCY * 100) + "%、上限は" + IdleService.MAX_SECONDS / 3600
                + "時間。レア素材は出ません。アプリを閉じていてもたまります。", margin, ny);
        if (!message.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
            g2.setColor(new Color(255, 230, 120));
            FontMetrics fm = g2.getFontMetrics();
            String m = message;
            while (fm.stringWidth(m) > w - px(24) && m.length() > 4)
                m = m.substring(0, m.length() - 2);
            g2.drawString(m, margin, ny + px(22));
        }
    }

    private static String format(int sec) {
        return String.format("%d:%02d:%02d", sec / 3600, sec % 3600 / 60, sec % 60);
    }

    private void drawCard(Graphics2D g2, int x, int y, int w, int h, Color edge) {
        g2.setColor(new Color(10, 20, 40, 200));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, px(14), px(14)));
        g2.setStroke(new BasicStroke((float) px(2)));
        g2.setColor(edge);
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, px(14), px(14)));
    }

    private void drawButton(Graphics2D g2, int x, int y, int w, int h, String label, boolean enabled) {
        g2.setColor(enabled ? new Color(255, 190, 40) : new Color(60, 70, 90));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, px(8), px(8)));
        g2.setColor(enabled ? new Color(40, 24, 0) : new Color(140, 150, 170));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, px(12))));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + (h + fm.getAscent()) / 2 - px(2));
    }
}
