package src.gameResultScene;

import src.ShootingGameMain;
import src.utils.Vector2;
import src.utils.Anchor;
import src.utils.UIBuilder;
import src.gameScene.GameManager;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;

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