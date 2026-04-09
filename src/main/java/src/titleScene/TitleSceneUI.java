package src.titleScene;

import src.ShootingGameMain;
import src.utils.Vector2;
import src.utils.UIBuilder;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;

/**
 * タイトルシーンのUI描画クラス
 */
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
        UIBuilder.drawString(g, pos, size, Color.CYAN, Font.BOLD, "Shooting Game");
    }
}