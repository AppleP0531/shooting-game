package com.game.shooting_game.gameResultScene;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.stage.DifficultyService;
import com.game.shooting_game.model.StageRewards;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class GameResultSceneUI {
    private static final double TEXT_TOP = 172; // 難易度・★ブロックの上端(adj前)
    private static final double LINE_H = 15; // 1行の高さ
    private static final double STAR_ROW_H = 32; // ★の行の高さ

    private final ShootingGameMain MAIN;
    private final GameManager GM;

    public GameResultSceneUI() {
        MAIN = ShootingGameMain.getInstance();
        GM = GameManager.getInstance();
    }

    /**
     * 描画
     *
     * @param g
     */
    public void draw(Graphics g) {
        drawTitle(g);
        showRewards(g);
    }

    /**
     * タイトルの描画
     *
     * @param g
     */
    private void drawTitle(Graphics g) {
        String text = GM.getGameResult() ? "Game Clear" : "Game Over";
        Color textColor = GM.getGameResult() ? Color.CYAN : Color.RED;
        double size = 40 * ShootingGameMain.getScreenSizeAdj();
        Vector2 pos = new Vector2(
                MAIN.getWidth() / 2,
                100 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, size, textColor, Font.BOLD, text);
    }

    /**
     * 報酬の表示
     *
     * @param g
     */
    private void showRewards(Graphics g) {
        StageRewards rewards = StageRewards.getInstance();
        // スコアを表示
        String scoreText = "Score: " + rewards.getScore();
        Vector2 scoreTextPos = new Vector2(
                MAIN.getWidth() / 2,
                150 * ShootingGameMain.getScreenSizeAdj());
        double scoreTextSize = 20 * ShootingGameMain.getScreenSizeAdj();
        UIBuilder.drawString(g, Anchor.CENTER, scoreTextPos, scoreTextSize, Color.WHITE, Font.PLAIN,
                scoreText);

        // 難易度(ノーマル以外)と、ミッションの★
        double adj = ShootingGameMain.getScreenSizeAdj();
        double lineY = TEXT_TOP * adj;
        double small = 12 * adj;
        double cx = MAIN.getWidth() / 2.0;
        if (!rewards.getDifficultyLabel().isEmpty()) {
            UIBuilder.drawString(g, Anchor.CENTER, new Vector2(cx, lineY), small,
                    new Color(255, 200, 90), Font.BOLD, "DIFFICULTY: " + rewards.getDifficultyLabel());
            lineY += LINE_H * adj;
        }
        DifficultyService.MissionResult mr = rewards.getMissionResult();
        if (mr == null)
            return;

        // ★の行(達成は黄色、未達成は灰色の枠。今回はじめて獲得した★は大きく脈打つ)
        double pulse = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1.8e8);
        double starY = lineY + STAR_ROW_H * adj / 2;
        Graphics2D g2 = (Graphics2D) g;
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        java.awt.Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < mr.missions().size(); i++) {
            boolean done = mr.missions().get(i).done();
            boolean fresh = i < mr.newlyEarned().length && mr.newlyEarned()[i];
            double r = (fresh ? 13 + 2 * pulse : 10) * adj;
            double x = cx + (i - 1) * 34 * adj;
            if (done) {
                g2.setColor(fresh ? new Color(255, 240, 120) : new Color(255, 205, 40));
                g2.fill(starShape(x, starY, r));
            } else {
                g2.setColor(new Color(110, 110, 120));
                g2.setStroke(new BasicStroke((float) Math.max(1, 1.5 * adj)));
                g2.draw(starShape(x, starY, r));
            }
        }
        g2.setStroke(oldStroke);
        if (oldAa != null)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
        lineY += STAR_ROW_H * adj;

        // ミッションの文(今回満たした: 緑の✓、満たさなかった: 灰色)
        for (int i = 0; i < mr.missions().size(); i++) {
            boolean now = i < mr.achievedNow().length && mr.achievedNow()[i];
            boolean fresh = i < mr.newlyEarned().length && mr.newlyEarned()[i];
            String text = (now ? "✓ " : "× ") + mr.missions().get(i).text() + (fresh ? "  NEW!" : "");
            Color c = fresh ? new Color(255, 240, 120)
                    : now ? new Color(120, 230, 140) : new Color(140, 140, 150);
            UIBuilder.drawString(g, Anchor.CENTER, new Vector2(cx, lineY), small, c,
                    fresh ? Font.BOLD : Font.PLAIN, text);
            lineY += LINE_H * adj;
        }

        // ★の合計
        int gained = rewards.getStarsAfter() - rewards.getStarsBefore();
        UIBuilder.drawString(g, Anchor.CENTER, new Vector2(cx, lineY), small, new Color(255, 215, 80), Font.PLAIN,
                "★合計 " + rewards.getStarsAfter() + (gained > 0 ? " (+" + gained + ")" : ""));
    }

    /**
     * スコア表示の下に並ぶ文字・★ブロックの下端のY座標(報酬リストの上端を決めるのに使う)
     */
    public static double textBottom() {
        StageRewards rewards = StageRewards.getInstance();
        double adj = ShootingGameMain.getScreenSizeAdj();
        double y = TEXT_TOP * adj;
        if (!rewards.getDifficultyLabel().isEmpty())
            y += LINE_H * adj;
        DifficultyService.MissionResult mr = rewards.getMissionResult();
        if (mr != null)
            y += STAR_ROW_H * adj + LINE_H * adj * (mr.missions().size() + 1);
        return y;
    }

    /** 中心(x, y)・外半径 r の5角の星 */
    private static Path2D starShape(double x, double y, double r) {
        Path2D p = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double rad = k % 2 == 0 ? r : r * 0.45;
            double a = -Math.PI / 2 + k * Math.PI / 5;
            double px = x + Math.cos(a) * rad;
            double py = y + Math.sin(a) * rad;
            if (k == 0)
                p.moveTo(px, py);
            else
                p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }
}
