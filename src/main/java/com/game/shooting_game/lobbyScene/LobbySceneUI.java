package com.game.shooting_game.lobbyScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.ship.ShipFactory;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.utils.Anchor;
import com.game.shooting_game.utils.Anchor.H;
import com.game.shooting_game.utils.Anchor.V;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;

public class LobbySceneUI {
    private final ShootingGameMain MAIN;
    private final GameManager GM;
    private final BulletManager BM;
    private final ShipManager SHIP_M;

    private String latestViewingShipName;
    private double levelUpInterval; // 強化の間隔
    private double levelUpIntervalTimer; // タイマー

    public record HangarSceneUIData(
            String[] ships,
            int ind) {
    }

    public LobbySceneUI() {
        MAIN = ShootingGameMain.getInstance();
        GM = GameManager.getInstance();
        BM = BulletManager.getInstance();
        SHIP_M = ShipManager.getInstance();

        // 2秒ごとにレベルアップする
        levelUpIntervalTimer = levelUpInterval = 2;
    }

    /**
     * セットアップ
     */
    public void setup() {
        SHIP_M.clear("all");
        // ターゲットとなる敵を生成
        SHIP_M.addShip(ShipFactory.createShip(
                ShipKinds.ENEMY,
                "Enemy1",
                0,
                new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() / 4),
                new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() / 4)));
        // HPを無限大に設定(不死にする)
        SHIP_M.getEnemyList().getFirst().setHp(Double.POSITIVE_INFINITY);
        levelUpIntervalTimer = levelUpInterval;
        latestViewingShipName = null;
    }

    /**
     * 更新
     * 
     * @param dt
     */
    public void update(double dt) {
        levelUpIntervalTimer -= dt;
        if (levelUpIntervalTimer <= 0) {
            levelUpIntervalTimer = levelUpInterval;
            // 暴走状態でなければレベルを上げる、暴走状態であればレベルをリセット
            if (SHIP_M.getPlayer().getShipLevel() < 5)
                SHIP_M.getPlayer().levelUp();
            else
                SHIP_M.getPlayer().setShipLevel(0);
        }

        GM.checkCollisions();
        BM.update(dt);
        SHIP_M.update(dt);
    }

    /**
     * 描画
     * 
     * @param g
     */
    public void draw(Graphics g, HangarSceneUIData uiData) {
        drawPreviewShip(g, uiData.ships[uiData.ind]);
        drawTabBar(g, uiData.ships.length, uiData.ind);
        drawUserLevel(g);
    }

    /**
     * 船のプレビューを描画
     * 
     * @param g
     */
    private void drawPreviewShip(Graphics g, String shipName) {
        // 参照中の船が切り替わった時に更新
        if (shipName != latestViewingShipName)
            SHIP_M.setPlayer(new Player(
                    shipName,
                    new Vector2(MAIN.getWidth() / 2, MAIN.getHeight() - 160 * ShootingGameMain.getScreenSizeAdj())));

        BM.draw(g);
        SHIP_M.draw(g);

        latestViewingShipName = shipName;
    }

    /**
     * タブバーの描画
     */
    private void drawTabBar(Graphics g, int totalLength, int ind) {
        Vector2 barPos = new Vector2(
                MAIN.getWidth() / 2,
                MAIN.getHeight() - 130 * ShootingGameMain.getScreenSizeAdj());
        Vector2 barSize = new Vector2(
                120,
                10);

        for (int i = 0; i < totalLength; i++) {
            double distance = (barSize.getX() - barSize.getY() * totalLength) / (totalLength - 1);
            double startX = barPos.getX() - barSize.getX() / 2 + i * (barSize.getY() + distance);
            double startY = barPos.getY() - barSize.getY() / 2;

            // 参照中の船がわかるバーの描画
            g.setColor(Color.CYAN);
            g.drawPolygon(
                    new int[] {
                            (int) (startX + barSize.getY() / 2 - 4 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startX + barSize.getY() / 2),
                            (int) (startX + barSize.getY() / 2 + 4 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startX + barSize.getY() / 2)
                    },
                    new int[] {
                            (int) (startY + barSize.getY() / 2),
                            (int) (startY + barSize.getY() / 2 - 6 * ShootingGameMain.getScreenSizeAdj()),
                            (int) (startY + barSize.getY() / 2),
                            (int) (startY + barSize.getY() / 2 + 6 * ShootingGameMain.getScreenSizeAdj())
                    },
                    4);
            if (i == ind) {
                g.fillPolygon(
                        new int[] {
                                (int) (startX + barSize.getY() / 2 - 4 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startX + barSize.getY() / 2),
                                (int) (startX + barSize.getY() / 2 + 4 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startX + barSize.getY() / 2)
                        },
                        new int[] {
                                (int) (startY + barSize.getY() / 2),
                                (int) (startY + barSize.getY() / 2 - 6 * ShootingGameMain.getScreenSizeAdj()),
                                (int) (startY + barSize.getY() / 2),
                                (int) (startY + barSize.getY() / 2 + 6 * ShootingGameMain.getScreenSizeAdj())
                        },
                        4);
            }
        }
    }

    /**
     * ユーザーレベルの描画
     * 
     * @param g
     */

    private void drawUserLevel(Graphics g) {
        int level = DataManager.getUserLevel().level();
        int exp = DataManager.getUserLevel().exp();
        int maxExp = 0;
        if (level < DataManager.getExpData().size())
            maxExp = DataManager.getExpData().get(level);

        String text = "User Level: " + level + " , Exp: " + exp + "/" + maxExp;
        UIBuilder.drawString(g, new Anchor(H.LEFT, V.TOP), new Vector2(0, 0), 20, Color.WHITE, Font.PLAIN, text);
    }
}
