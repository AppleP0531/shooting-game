package com.game.shooting_game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.domain.tutorial.TutorialService;
import com.game.shooting_game.domain.tutorial.TutorialService.Guide;

/**
 * チュートリアルの表示(画面全体を暗くして、下に説明のパネルを出す)
 * <p>
 * 出ている間は、下の画面へのクリックを受け止める(説明を読み終えるまで、ほかの操作はできない)。
 * 「次へ」(Enter・Space・クリック)でページを進め、最後のページで閉じる。「スキップ」(Esc)で、残りを飛ばして閉じる。
 * 閉じたガイドは、見たものとして記録する。
 */
public final class TutorialOverlay extends JComponent {
    private static TutorialOverlay instance;

    private Guide guide;
    private int page;
    private Runnable onClose;
    private final Rectangle nextBtn = new Rectangle();
    private final Rectangle skipBtn = new Rectangle();

    private TutorialOverlay() {
        setOpaque(false);
        setVisible(false);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (nextBtn.contains(e.getPoint()))
                    next();
                else if (skipBtn.contains(e.getPoint()))
                    close();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> next();
                    case KeyEvent.VK_ESCAPE -> close();
                    default -> {
                    }
                }
                e.consume();
            }
        });
    }

    public static synchronized TutorialOverlay getInstance() {
        if (instance == null)
            instance = new TutorialOverlay();
        return instance;
    }

    /** 表示中か(ゲームを止めておく判断に使う) */
    public boolean isShowingGuide() {
        return guide != null;
    }

    /**
     * ガイドを出す(どのスレッドから呼んでもよい)。すでに別のガイドが出ているときは、何もしない
     *
     * @param onClose 閉じたときの処理(ゲームの再開など。画面のスレッドで呼ばれる)
     * @return 出せたらtrue
     */
    public synchronized boolean show(Guide g, Runnable onClose) {
        if (g == null || guide != null)
            return false;
        guide = g;
        page = 0;
        this.onClose = onClose;
        SwingUtilities.invokeLater(() -> {
            if (getParent() != null)
                setBounds(0, 0, getParent().getWidth(), getParent().getHeight());
            setVisible(true);
            requestFocusInWindow();
            repaint();
        });
        return true;
    }

    private void next() {
        if (guide == null)
            return;
        if (page + 1 >= guide.pages().size())
            close();
        else {
            page++;
            repaint();
        }
    }

    private void close() {
        Guide g;
        Runnable r;
        synchronized (this) {
            g = guide;
            r = onClose;
            guide = null;
            onClose = null;
        }
        if (g == null)
            return;
        TutorialService.markSeen(g);
        setVisible(false);
        if (r != null)
            r.run();
        ShootingGameMain.getInstance().requestFocusInWindow(); // ゲームのキー入力へ戻す
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Guide gd = guide;
        if (gd == null)
            return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double adj = ShootingGameMain.getScreenSizeAdj();
        int w = getWidth(), h = getHeight();

        // 画面を暗くする
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, w, h);

        // 説明のパネル(下)
        int margin = (int) (14 * adj);
        int pw = w - 2 * margin;
        int ph = (int) (230 * adj);
        int px = margin, py = h - ph - (int) (60 * adj);
        g.setColor(new Color(10, 20, 40, 235));
        g.fill(new RoundRectangle2D.Double(px, py, pw, ph, 18 * adj, 18 * adj));
        g.setStroke(new BasicStroke((float) (2 * adj)));
        g.setColor(new Color(70, 215, 255));
        g.draw(new RoundRectangle2D.Double(px, py, pw, ph, 18 * adj, 18 * adj));

        // 見出しとページ
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, (int) (16 * adj))));
        g.setColor(new Color(255, 214, 80));
        g.drawString(gd.title(), px + (int) (14 * adj), py + (int) (26 * adj));
        String pager = (page + 1) + " / " + gd.pages().size();
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, (int) (11 * adj))));
        g.setColor(new Color(170, 205, 235));
        g.drawString(pager, px + pw - (int) (14 * adj) - g.getFontMetrics().stringWidth(pager), py + (int) (26 * adj));

        // 本文(折り返して並べる)
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(11, (int) (14 * adj))));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int tx = px + (int) (14 * adj);
        int ty = py + (int) (52 * adj);
        for (String line : wrap(gd.pages().get(page), fm, pw - (int) (28 * adj))) {
            g.drawString(line, tx, ty);
            ty += fm.getHeight() + (int) (3 * adj);
        }

        // ボタン
        int bh = (int) (30 * adj), bw = (int) (96 * adj);
        int by = py + ph - bh - (int) (12 * adj);
        boolean last = page + 1 >= gd.pages().size();
        nextBtn.setBounds(px + pw - bw - (int) (14 * adj), by, bw, bh);
        skipBtn.setBounds(px + (int) (14 * adj), by, (int) (84 * adj), bh);
        drawButton(g, nextBtn, last ? "はじめる" : "次へ ▶", new Color(255, 190, 40), new Color(40, 24, 0));
        if (!last)
            drawButton(g, skipBtn, "スキップ", new Color(60, 70, 90), new Color(200, 210, 225));
        else
            skipBtn.setBounds(0, 0, 0, 0);
        g.dispose();
    }

    private void drawButton(Graphics2D g, Rectangle r, String label, Color bg, Color fg) {
        double adj = ShootingGameMain.getScreenSizeAdj();
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10 * adj, 10 * adj));
        g.setColor(fg);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (13 * adj))));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + (r.height + fm.getAscent()) / 2 - (int) (2 * adj));
    }

    /** 文字列を、幅に収まるように折り返す(\n は改行。日本語は1文字ずつ区切れる) */
    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            StringBuilder cur = new StringBuilder();
            for (char c : para.toCharArray()) {
                // 行頭に来てはいけない句読点・閉じ括弧は、前の行にはみ出して付ける
                boolean noLineStart = "。、」』）)！？!?…ー".indexOf(c) >= 0;
                if (fm.stringWidth(cur.toString() + c) > width && cur.length() > 0 && !noLineStart) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                cur.append(c);
            }
            lines.add(cur.toString());
        }
        return lines;
    }
}
