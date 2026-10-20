package com.game.shooting_game.gachaScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.game.shooting_game.SceneManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.cost.CostService;
import com.game.shooting_game.domain.gacha.GachaService;
import com.game.shooting_game.domain.gacha.GachaService.Result;
import com.game.shooting_game.domain.gacha.PieceService;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.interfaces.Manager;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

/**
 * ガチャ画面
 * コインで引くと、ランダムな機体のピースが手に入る。結果と、機体ごとのピースの進み具合を出す。
 * ピースがたまった機体は、ロビーで(コインを払って)解放でき、所持している機体は、ピースをランクアップに使う。
 */
public final class GachaManager extends JPanel implements Manager {
    private static final String component = "[GachaManager]";
    private static GachaManager instance;

    private final ShootingGameMain MAIN;
    private JButton goBackBtn;
    private JButton singleBtn;
    private JButton tenBtn;
    private List<Result> lastResults = new ArrayList<>();
    private String message = "";

    private GachaManager() {
        MAIN = ShootingGameMain.getInstance();
    }

    public static GachaManager getInstance() {
        return instance = Objects.requireNonNullElseGet(instance, GachaManager::new);
    }

    @Override
    public void initComponents() {
        setLayout(null);
        setOpaque(false);
        double adj = ShootingGameMain.getScreenSizeAdj();

        goBackBtn = UIBuilder.createBackBtn(component, () -> SceneManager.getInstance().changeToPrevScene());
        this.add(goBackBtn);

        Vector2 size = new Vector2(150 * adj, 46 * adj);
        singleBtn = UIBuilder.createImageBtn(component, "/static/img/UI/gachaScene/pullOne.png", size, "x1");
        singleBtn.setBounds(MAIN.getWidth() / 2 - (int) size.getX() - (int) (6 * adj), (int) (84 * adj),
                (int) size.getX(), (int) size.getY());
        singleBtn.addActionListener(e -> pull(1));
        this.add(singleBtn);

        tenBtn = UIBuilder.createImageBtn(component, "/static/img/UI/gachaScene/pullTen.png", size, "x10");
        tenBtn.setBounds(MAIN.getWidth() / 2 + (int) (6 * adj), (int) (84 * adj), (int) size.getX(),
                (int) size.getY());
        tenBtn.addActionListener(e -> pull(10));
        this.add(tenBtn);
    }

    private void pull(int times) {
        List<Result> r = GachaService.pull(times);
        if (r.isEmpty()) {
            message = "コインが足りません("
                    + String.format("%,d", times >= 10 ? GachaService.TEN_COST : GachaService.SINGLE_COST)
                    + " コイン)";
            lastResults = new ArrayList<>();
        } else {
            lastResults = r;
            message = "";
        }
        refresh();
        repaint();
    }

    private void refresh() {
        boolean admin = DataManager.getUserData() != null && DataManager.isAdminMode();
        singleBtn.setEnabled(admin || CostService.coins() >= GachaService.SINGLE_COST);
        tenBtn.setEnabled(admin || CostService.coins() >= GachaService.TEN_COST);
    }

    @Override
    public void setup() {
        lastResults = new ArrayList<>();
        message = "";
        refresh();
    }

    @Override
    public void update(double dt) {
    }

    @Override
    public void draw(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();
        int cx = MAIN.getWidth() / 2;

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, (int) (24 * adj))));
        center(g, "GACHA", cx, (int) (44 * adj));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (11 * adj))));
        g.setColor(new Color(190, 220, 255));
        center(g, "x1: " + String.format("%,d", GachaService.SINGLE_COST) + " コイン / x10: "
                + String.format("%,d", GachaService.TEN_COST) + " コイン(3個以上が最低1回出る)", cx, (int) (66 * adj));

        // 結果(機体ごとにまとめる)
        int y = (int) (150 * adj);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, (int) (14 * adj))));
        if (!message.isEmpty()) {
            g.setColor(new Color(255, 120, 120));
            center(g, message, cx, y);
        } else if (!lastResults.isEmpty()) {
            Map<String, Integer> sum = new LinkedHashMap<>();
            for (Result r : lastResults)
                sum.merge(r.shipId(), r.pieces(), Integer::sum);
            g.setColor(new Color(255, 214, 80));
            center(g, "手に入ったピース", cx, y);
            g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(11, (int) (14 * adj))));
            g.setColor(Color.WHITE);
            for (Map.Entry<String, Integer> e : sum.entrySet()) {
                y += (int) (20 * adj);
                center(g, e.getKey() + "   +" + e.getValue() + " ピース", cx, y);
            }
        }

        // 機体ごとのピースの進み具合
        int top = (int) (300 * adj);
        int cardX = (int) (24 * adj), cardW = MAIN.getWidth() - 2 * cardX;
        var costs = GachaService.shipCosts();
        int rowH = (int) (44 * adj);
        int cardH = (int) (28 * adj) + rowH * costs.size();
        g.setColor(new Color(10, 20, 40, 200));
        g.fillRoundRect(cardX, top, cardW, cardH, (int) (14 * adj), (int) (14 * adj));
        g.setColor(new Color(70, 215, 255));
        g.setStroke(new BasicStroke((float) (2 * adj)));
        g.drawRoundRect(cardX, top, cardW, cardH, (int) (14 * adj), (int) (14 * adj));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (12 * adj))));
        g.setColor(new Color(150, 200, 230));
        g.drawString("機体のピース(解放: " + PieceService.UNLOCK_PIECES + " 個 + コイン。ランクアップにも使います)",
                cardX + (int) (10 * adj), top + (int) (18 * adj));

        int ry = top + (int) (28 * adj);
        for (String id : costs.keySet()) {
            boolean owned = DataManager.getUserData().getShips().containsKey(id);
            int have = PieceService.owned(id);
            int need = owned ? PieceService.rankUpPieces(UnitService.getRank(id)) : PieceService.UNLOCK_PIECES;
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (13 * adj))));
            g.drawString(id + (owned ? "  Rank " + UnitService.getRank(id) : "  未解放"), cardX + (int) (10 * adj),
                    ry + (int) (16 * adj));
            // バー
            int barX = cardX + (int) (10 * adj), barY = ry + (int) (22 * adj);
            int barW = cardW - (int) (20 * adj), barH = (int) (10 * adj);
            double ratio = need <= 0 ? 1.0 : Math.min(1.0, (double) have / need);
            g.setColor(new Color(255, 255, 255, 40));
            g.fillRoundRect(barX, barY, barW, barH, barH, barH);
            g.setColor(have >= need && need > 0 ? new Color(255, 214, 80) : new Color(70, 215, 255));
            g.fillRoundRect(barX, barY, (int) (barW * ratio), barH, barH, barH);
            g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (10 * adj))));
            g.setColor(Color.WHITE);
            String t = need <= 0 ? have + " ピース(ランク最大)"
                    : have + " / " + need + " ピース" + (owned ? "(次のランクアップ)" : "");
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t, barX + barW - fm.stringWidth(t), ry + (int) (16 * adj));
            ry += rowH;
        }
        g.dispose();
    }

    private static void center(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}
