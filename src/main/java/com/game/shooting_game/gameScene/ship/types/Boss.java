package com.game.shooting_game.gameScene.ship.types;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public class Boss extends Ship {
    private static final String component = "[Ship:Boss]";

    private Vector2 stayPos; // 待機位置

    public Boss(String id, ColorMode mode, int level, Vector2 spawnPos, Vector2 stayPos) {
        super(ShipKinds.BOSS, id, mode, level, spawnPos);

        // データがなかった時用のデフォルト値
        ShipDefinition def = SHIP_M.getShipConfig().get(owner).get(shipId);
        if (def == null || def.getDatas() == null) {
            collisionSize = 3 * ShootingGameMain.getScreenSizeAdj();
            speed = 300 * ShootingGameMain.getScreenSizeAdj();
        }
        for (String object : animationId.keySet()) {
            if (animationId.get(object) == null) {
                switch (object) {
                    case "ship" -> {
                        imageSize.put("ship", (int) (30 * ShootingGameMain.getScreenSizeAdj()));
                    }
                }
            }
        }

        rotate = Math.PI;

        // 待機位置に向かうようにvecを設定
        this.stayPos = stayPos;
        vec = GameUtils.targetVec(spawnPos, stayPos);

        // 入場アニメーション
        animation("admission");

        // 船タイプログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Current shipId set to " + ANSI.YELLOW + shipId + "." + ANSI.RESET);
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        addNewBullet(shipLevel, shipId, shipState);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.draw(g);

        // 画像が存在しなければ仮描画
        // 船
        if (animationId.get("ship") == null) {
            g2.rotate(rotate, pos.getX(), pos.getY());

            if (mode == ColorMode.RED)
                g2.setColor(Color.RED);
            else
                g2.setColor(Color.BLUE);

            g.fillPolygon(
                    new int[] {
                            (int) pos.getX(),
                            (int) pos.getX() - imageSize.get("ship") / 2,
                            (int) pos.getX() + imageSize.get("ship") / 2
                    },
                    new int[] {
                            (int) pos.getY() - imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2
                    },
                    3);
            // 回転リセット
            g2.rotate(-rotate, pos.getX(), pos.getY());
        }

        switch (animationName) {
            case "death" -> {
                clear();
            }
        }
    }

    @Override
    public void move(double dt) {
        // 待機位置に到達したら停止
        if (25 < GameUtils.distSq(pos, stayPos)) {
            super.move(dt);
        } else {
            pos = stayPos;
            vec = 0;
        }
    }

    @Override
    public void drawHpBar(Graphics g) {

    }
}