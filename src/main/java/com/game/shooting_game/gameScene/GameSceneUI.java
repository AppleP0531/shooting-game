package com.game.shooting_game.gameScene;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ResourceManager.ResourceKinds;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.Anchor.H;
import com.game.shooting_game.utils.Anchor.V;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class GameSceneUI {
    private final ShootingGameMain MAIN;
    private final ResourceManager RM;
    private final ItemManager IM;

    public record GameSceneUIData(
            String animationName,
            double animateTimer,
            int score,
            int coin,
            Map<String, Double> effectList) {
    }

    public GameSceneUI() {
        MAIN = ShootingGameMain.getInstance();
        RM = ResourceManager.getInstance();
        IM = ItemManager.getInstance();
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g, GameSceneUIData uiData) {
        drawAnimation(g, uiData.animationName, uiData.animateTimer);
        drawActiveEffects(g, uiData.effectList);
        drawScore(g, uiData.score);
        drawCoin(g, uiData.coin);
    }

    /**
     * アニメーションの描画
     * 
     * @param g
     * @param animationName   アニメーション名
     * @param animateTimer    アニメーション用タイマー
     * @param countdownImages カウントダウン画像
     */
    private void drawAnimation(Graphics g, String animationName, double animateTimer) {
        switch (animationName) {
            case "countdown" -> {
                double textSize = 40 * ShootingGameMain.getScreenSizeAdj();
                if (animateTimer < 1) {
                    Vector2 pos = new Vector2(
                            MAIN.getWidth() / 2,
                            300 * ShootingGameMain.getScreenSizeAdj());
                    UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, textSize, Color.WHITE, Font.BOLD, "3");
                } else if (animateTimer < 2) {
                    Vector2 pos = new Vector2(
                            MAIN.getWidth() / 2,
                            300 * ShootingGameMain.getScreenSizeAdj());
                    UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, textSize, Color.WHITE, Font.BOLD, "2");
                } else if (animateTimer < 3) {
                    Vector2 pos = new Vector2(
                            MAIN.getWidth() / 2,
                            300 * ShootingGameMain.getScreenSizeAdj());
                    UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, textSize, Color.WHITE, Font.BOLD, "1");
                } else if (animateTimer < 4) {
                    Vector2 pos = new Vector2(
                            MAIN.getWidth() / 2,
                            300 * ShootingGameMain.getScreenSizeAdj());
                    UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, textSize, Color.WHITE, Font.BOLD, "GO!");
                }
            }
        }
    }

    /**
     * アクティブなエフェクトの描画
     * 
     * @param g
     * @param list エフェクトリスト
     */
    private void drawActiveEffects(Graphics g, Map<String, Double> list) {
        int i = 0;
        for (Map.Entry<String, Double> effectEntry : list.entrySet()) {
            drawActiveEffectIcon(g, effectEntry.getKey(), effectEntry.getValue(), i);
            i++;
        }
    }

    /**
     * エフェクトアイコンの描画
     * 
     * @param g
     * @param effectName エフェクト名
     * @param time       残り効果時間
     * @param ind        何番目か
     */
    private void drawActiveEffectIcon(Graphics g, String effectName, double time, int ind) {
        Graphics2D g2 = (Graphics2D) g;

        float defaultAlpha = 0.8f;
        float alpha = (time <= 5) ? GameUtils.calcFlashAlpha(time, 2, defaultAlpha) : defaultAlpha;

        double iconSize = 40 * ShootingGameMain.getScreenSizeAdj();
        Vector2 pos = new Vector2(MAIN.getWidth() - iconSize / 2 - 5, 100 * ShootingGameMain.getScreenSizeAdj());
        double distance = iconSize + 20 * ShootingGameMain.getScreenSizeAdj();

        BufferedImage originImage = null;
        try {
            switch (effectName) {
                case "OOC" -> originImage = IM.getItemImage().get(ItemKinds.LEVEL_UP)[0];
                case "Shield" -> originImage = IM.getItemImage().get(ItemKinds.SHIELD)[0];
            }

            // 画像のリサイズ
            BufferedImage image = UIBuilder.resizeImage(originImage, iconSize);

            // 透明度の変更
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            // 画像の描画
            g.drawImage(
                    image,
                    (int) pos.getX() - (int) iconSize / 2,
                    (int) pos.getY() + ind * (int) distance - (int) iconSize / 2,
                    image.getWidth(), (int) image.getHeight(),
                    null);
        } catch (Exception e) {
            switch (effectName) {
                case "OOC" -> g.setColor(new Color(1, 1, 0, alpha));
                case "Shield" -> g.setColor(new Color(0, 1, 1, alpha));
            }
            g.fillOval((int) pos.getX() - (int) iconSize / 2,
                    (int) pos.getY() + ind * (int) distance - (int) iconSize / 2,
                    (int) iconSize, (int) iconSize);
        }
        // 透明度を1に戻す

        g2.setComposite(AlphaComposite.SrcOver);

        // テキスト (10を下回ったら小数第1位まで表示)
        String text = (time >= 10 ? "%.0f" : "%.1f").formatted(time);
        double size = 10 * ShootingGameMain.getScreenSizeAdj();
        Vector2 textPos = new Vector2(
                pos.getX(),
                pos.getY() + iconSize / 2 + ind * distance);
        UIBuilder.drawString(g, new Anchor(H.CENTER, V.TOP), textPos, size, Color.WHITE, Font.BOLD, text);
    }

    /**
     * コインの描画
     * 
     * @param g
     * @param coin コイン
     */
    private void drawCoin(Graphics g, int coin) {
        BufferedImage originImage = null;

        try {
            double iconSize = 20 * ShootingGameMain.getScreenSizeAdj();
            Vector2 pos = new Vector2(
                    5 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2,
                    9 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2);

            ResourceKinds resKind = ResourceKinds.COIN;
            originImage = RM.getResourceImage()
                    .get(resKind.getResName())
                    .get(resKind.getRarity())[0];
            BufferedImage image = UIBuilder.resizeImage(originImage, iconSize); // 画像のリサイズ

            g.drawImage( // 画像の描画
                    image,
                    (int) pos.getX() - (int) iconSize / 2,
                    (int) pos.getY() - (int) iconSize / 2,
                    image.getWidth(), (int) image.getHeight(),
                    MAIN);
        } catch (Exception e) { // 画像の読み込みに失敗した場合の処理
            double iconSize = 8 * ShootingGameMain.getScreenSizeAdj();
            Vector2 pos = new Vector2(
                    10 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2,
                    10 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2);

            g.setColor(Color.YELLOW);
            g.fillPolygon(new int[] {
                    (int) pos.getX() + (int) iconSize / 2,
                    (int) pos.getX(),
                    (int) pos.getX() - (int) iconSize / 2,
                    (int) pos.getX()
            }, new int[] {
                    (int) pos.getY(),
                    (int) pos.getY() + (int) iconSize,
                    (int) pos.getY(),
                    (int) pos.getY() - (int) iconSize
            }, 4);
        }

        String text = String.valueOf(coin);
        Vector2 pos = new Vector2(
                (int) (30 * ShootingGameMain.getScreenSizeAdj()),
                0);
        int size = (int) (20 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), pos, size, Color.WHITE, Font.BOLD, text);
    }

    /**
     * スコアの描画
     * 
     * @param g
     * @param score スコア
     */
    private void drawScore(Graphics g, int score) {
        String text = "Score: " + score;
        Vector2 pos = new Vector2(
                MAIN.getWidth() - (int) (100 * ShootingGameMain.getScreenSizeAdj()),
                0);
        int size = (int) (20 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(H.RIGHT, V.TOP), pos, size, Color.WHITE, Font.BOLD, text);
    }

}
