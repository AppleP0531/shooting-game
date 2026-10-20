package com.game.shooting_game.bagScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class BagSceneUI {
    private final ShootingGameMain MAIN;

    public record BagSceneUIData() {

    }

    public BagSceneUI() {
        MAIN = ShootingGameMain.getInstance();
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g, BagSceneUIData uiData) {
        Vector2 titlePos = new Vector2(
                MAIN.getWidth() / 2,
                10 * ShootingGameMain.getScreenSizeAdj());
        double titleSize = 24 * ShootingGameMain.getScreenSizeAdj();
        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), titlePos, titleSize, Color.WHITE, Font.BOLD, "Bag");
    }
}
