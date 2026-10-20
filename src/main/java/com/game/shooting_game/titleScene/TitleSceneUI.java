package com.game.shooting_game.titleScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class TitleSceneUI {
    private final ShootingGameMain MAIN;

    public TitleSceneUI() {
        MAIN = ShootingGameMain.getInstance();
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
        double size = 40 * ShootingGameMain.getScreenSizeAdj();
        Vector2 pos = new Vector2(
                MAIN.getWidth() / 2,
                100 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, size, Color.CYAN, Font.BOLD, "Shooting Game");
    }
}