package src.gameScene.ship.types;

import src.ShootingGameMain;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.gameScene.ship.Ship;
import src.gameScene.ship.ShipManager.ShipConfig;
import src.gameScene.ship.ShipManager.ShipDefinition;
import src.gameScene.ship.ShipManager.ShipKinds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class SubShip extends Ship {
    private static final String component = "[Ship:SubShip]";
    private final Player player;

    private Vector2 posDiff;

    public SubShip(Player player, String id, Vector2 pos) {
        super(ShipKinds.PLAYER, id, 0, pos);
        this.player = player;

        ShipDefinition def = SHIP_M.getShipConfig().get(owner).get(shipId);
        if (def != null && def.getDatas() != null) {
            // 画像が存在すれば読み込む
            for (String object : animationId.keySet()) {
                if (animationId.get(object) != -1) {
                    ShipConfig datas = def.getDatas();

                    int sheetCount = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets().length;
                    useCount.put(object, new int[sheetCount]);
                    for (int i = 0; i < sheetCount; i++) {
                        useCount.get(
                                object)[i] = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets()[i]
                                        .getUseCount();
                    }
                    imageSize.put(object, (int) (datas.getImageSize() * ShootingGameMain.getScreenSizeAdj()));
                } else {
                    switch (object) {
                        case "ship" -> {
                            imageSize.put("ship", (int) (30 * ShootingGameMain.getScreenSizeAdj()));
                        }
                    }
                }
            }
        } else {
            // データがなかった時用のデフォルト値
            speed = 250 * ShootingGameMain.getScreenSizeAdj();
        }
        posDiff = new Vector2(
                pos.getX() - player.getPos().getX(),
                pos.getY() - player.getPos().getY());

        collisionSize = 0;
        rotate = 0;

        // 船タイプログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Current shipId set to " + ANSI.YELLOW + shipId + "." + ANSI.RESET);
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        switch (player.getAnimationName()) {
            case "none" -> {
                // 弾
                if (player.isAutoFire() || player.isSpace())
                    addNewBullet(shipLevel, shipId, shipState);

                // 船の状態
                shipState = player.getShipState();
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.draw(g);

        // 画像が存在しなければ仮描画
        for (String object : animationId.keySet()) {
            if (animationId.get(object) == -1) {
                switch (object) {
                    case "ship" -> {
                        g2.rotate(rotate, pos.getX(), pos.getY());
                        g2.setColor(new Color(0, 255, 255));
                        g.fillPolygon(
                                new int[] {
                                        (int) pos.getX(),
                                        (int) pos.getX() - imageSize.get(object) / 8,
                                        (int) pos.getX() + imageSize.get(object) / 8
                                },
                                new int[] {
                                        (int) pos.getY() - imageSize.get(object) / 3,
                                        (int) pos.getY() + imageSize.get(object) / 3,
                                        (int) pos.getY() + imageSize.get(object) / 3
                                },
                                3);
                        // 回転リセット
                        g2.rotate(-rotate, pos.getX(), pos.getY());
                    }
                }
            }
        }

    }

    @Override
    public void move(double dt) {
        // 追跡のなめらかさ
        double lerpFactor = 8.0;

        // 目標地点
        double targetX = player.getPos().getX() + posDiff.getX();
        double targetY = player.getPos().getY() + posDiff.getY();

        // 移動量の計算
        double nextX = pos.getX() + (targetX - pos.getX()) * lerpFactor * dt;
        double nextY = pos.getY() + (targetY - pos.getY()) * lerpFactor * dt;

        pos.setX(nextX);
        pos.setY(nextY);
    }

    @Override
    public void drawHpBar(Graphics g) {
        // 何もしない
    }
}
