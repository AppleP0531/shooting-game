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
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.Anchor.H;
import com.game.shooting_game.ui.Anchor.V;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class GameSceneUI {
    private final ShootingGameMain MAIN;
    private final ResourceManager RM;
    private final ItemManager IM;

    private BufferedImage cachedCoinImage = null;

    public record GameSceneUIData(
            String animationName,
            double animateTimer,
            int score,
            int coin,
            Map<String, Double> effectList,
            double resumeTimer, // 再開カウントの経過時間(負なら数えていない)
            double leaveFade, // 離脱フェードの進み具合(0〜1。負ならフェードしていない)
            java.util.List<com.game.shooting_game.gameScene.stage.DifficultyService.LiveMission> missions) { // プレイ中に出すミッション(出さないなら空)
    }

    public GameSceneUI() {
        MAIN = ShootingGameMain.getInstance();
        RM = ResourceManager.getInstance();
        IM = ItemManager.getInstance();

        double iconSize = 20 * ShootingGameMain.getScreenSizeAdj();
        ResourceKinds resKind = ResourceKinds.COIN;
        BufferedImage originImage = RM.getResourceImage()
                .get(resKind.getResName())
                .get(resKind.getRarity())[0];
        cachedCoinImage = UIBuilder.resizeImage(originImage, iconSize); // 画像のリサイズ
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
        drawMissions(g, uiData.missions);
        drawResumeCount(g, uiData.resumeTimer);
        drawLeaveFade(g, uiData.leaveFade);
    }

    /**
     * 再開前の3・2・1(ゲームは止まったまま。画面は少し暗くなり、数字は大きい状態から縮みながら薄くなる)
     */
    private void drawResumeCount(Graphics g, double timer) {
        if (timer < 0)
            return;

        double adj = ShootingGameMain.getScreenSizeAdj();
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(0, 0, MAIN.getWidth(), MAIN.getHeight());

        int number = 3 - (int) timer;
        double frac = timer - Math.floor(timer);
        if (number < 1)
            return;

        double scale = 1 + 0.5 * Math.pow(1 - frac, 2); // 1.5倍から1倍へ
        int alpha = (int) (255 * (frac < 0.7 ? 1 : (1 - frac) / 0.3));
        Vector2 pos = new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() / 2);
        UIBuilder.drawString(g, new Anchor(Anchor.CENTER), pos, 64 * adj * scale,
                new Color(255, 255, 255, Math.clamp(alpha, 0, 255)), Font.BOLD, String.valueOf(number));
    }

    /**
     * 離脱するときの暗転(だんだん黒くなる)
     */
    private void drawLeaveFade(Graphics g, double progress) {
        if (progress < 0)
            return;
        g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, progress))));
        g.fillRect(0, 0, MAIN.getWidth(), MAIN.getHeight());
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
                case "Regen" -> originImage = IM.getItemImage().get(ItemKinds.REGEN)[0];
                case "AtkUp" -> originImage = IM.getItemImage().get(ItemKinds.ATK_UP)[0];
            }

            // 画像のリサイズ
            BufferedImage image = UIBuilder.resizeImage(originImage, iconSize);

            // 透明度の変更
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            // 画像の描画
            Vector2 imagePos = new Vector2(
                    pos.getX(),
                    pos.getY() + ind * (int) distance);
            UIBuilder.drawImage(g2, Anchor.CENTER, imagePos, image);
        } catch (Exception e) {
            switch (effectName) {
                case "OOC" -> g.setColor(new Color(1, 1, 0, alpha));
                case "Shield" -> g.setColor(new Color(0, 1, 1, alpha));
                case "Regen" -> g.setColor(new Color(0.5f, 1f, 0.6f, alpha));
                case "AtkUp" -> g.setColor(new Color(1, 0.2f, 0.2f, alpha));
                default -> {
                    // 特殊効果(Status:名前)は、効果の色で出す
                    if (effectName.startsWith("Status:")) {
                        Color c = com.game.shooting_game.gameScene.status.StatusEffect
                                .valueOf(effectName.substring(7)).getColor();
                        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255)));
                    }
                }
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
        Vector2 pos = new Vector2(
                30 * ShootingGameMain.getScreenSizeAdj(),
                15 * ShootingGameMain.getScreenSizeAdj());

        // 画像があれば描画
        if (cachedCoinImage != null) {
            UIBuilder.drawImage(g, new Anchor(H.RIGHT, V.CENTER), pos, cachedCoinImage);
        } else {
            double iconSize = 8 * ShootingGameMain.getScreenSizeAdj();
            Vector2 iconPos = new Vector2(
                    10 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2,
                    10 * ShootingGameMain.getScreenSizeAdj() + iconSize / 2);

            g.setColor(Color.YELLOW);
            g.fillPolygon(new int[] {
                    (int) iconPos.getX() + (int) iconSize / 2,
                    (int) iconPos.getX(),
                    (int) iconPos.getX() - (int) iconSize / 2,
                    (int) iconPos.getX()
            }, new int[] {
                    (int) iconPos.getY(),
                    (int) iconPos.getY() + (int) iconSize,
                    (int) iconPos.getY(),
                    (int) iconPos.getY() - (int) iconSize
            }, 4);
        }

        String text = String.valueOf(coin);
        int size = (int) (20 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.CENTER), pos, size, Color.WHITE, Font.BOLD, text);
    }

    /**
     * スコアの描画
     * 
     * @param g
     * @param score スコア
     */
    /**
     * ミッションの状況(左上。HPのゲージの下。達成見込みは緑、もう満たせないものは赤、あとで満たせるものは黄色。記録済みは★を金色に)
     */
    private void drawMissions(Graphics g, java.util.List<com.game.shooting_game.gameScene.stage.DifficultyService.LiveMission> missions) {
        if (missions == null || missions.isEmpty())
            return;
        double adj = ShootingGameMain.getScreenSizeAdj();
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int) (10 * adj))));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int x = (int) (8 * adj), y = (int) (54 * adj), line = fm.getHeight() + (int) (2 * adj);
        for (var m : missions) {
            Color c = switch (m.state()) {
                case OK -> new Color(120, 230, 150);
                case FAILED -> new Color(255, 110, 110);
                case PENDING -> new Color(255, 220, 100);
            };
            String mark = switch (m.state()) {
                case OK -> "○";
                case FAILED -> "×";
                case PENDING -> "△";
            };
            String text = mark + " " + m.text() + "  (" + m.progress() + ")";
            g2.setColor(new Color(0, 0, 0, 200));
            for (int[] d : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } })
                g2.drawString(text, x + d[0] + (int) (14 * adj), y + d[1]);
            g2.setColor(c);
            g2.drawString(text, x + (int) (14 * adj), y);
            // 記録済みの★(金)か、まだの★(うすい枠)
            g2.setColor(m.earned() ? new Color(255, 205, 50) : new Color(150, 160, 190));
            g2.drawString(m.earned() ? "★" : "☆", x, y);
            y += line;
        }
        g2.dispose();
    }

    private void drawScore(Graphics g, int score) {
        String text = "Score: " + score;
        Vector2 pos = new Vector2(
                MAIN.getWidth() - (int) (10 * ShootingGameMain.getScreenSizeAdj()),
                15 * ShootingGameMain.getScreenSizeAdj());
        int size = (int) (20 * ShootingGameMain.getScreenSizeAdj());
        UIBuilder.drawString(g, new Anchor(H.RIGHT, V.CENTER), pos, size, Color.WHITE, Font.BOLD, text);
    }

}
