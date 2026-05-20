package com.game.shooting_game.gameResultScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.UIBuilder;
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
}