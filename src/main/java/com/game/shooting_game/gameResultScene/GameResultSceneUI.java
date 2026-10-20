package com.game.shooting_game.gameResultScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.model.StageRewards;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class GameResultSceneUI {
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

        // 難易度(ノーマル以外)と、HARDで新しく達成したミッション
        double lineY = 172 * ShootingGameMain.getScreenSizeAdj();
        double small = 12 * ShootingGameMain.getScreenSizeAdj();
        if (!rewards.getDifficultyLabel().isEmpty()) {
            UIBuilder.drawString(g, Anchor.CENTER, new Vector2(MAIN.getWidth() / 2, lineY), small,
                    new Color(255, 200, 90), Font.BOLD, "DIFFICULTY: " + rewards.getDifficultyLabel());
            lineY += 15 * ShootingGameMain.getScreenSizeAdj();
        }
        for (String m : rewards.getNewMissions()) {
            UIBuilder.drawString(g, Anchor.CENTER, new Vector2(MAIN.getWidth() / 2, lineY), small,
                    new Color(150, 230, 255), Font.PLAIN, "ミッション達成: " + m);
            lineY += 15 * ShootingGameMain.getScreenSizeAdj();
        }
    }
}