package src.gameScene.ship.types;

import src.ShootingGameMain;
import src.utils.ANSI;
import src.utils.Vector2;
import src.utils.LogUtils;
import src.utils.GameUtils;
import src.gameScene.ship.Ship;
import src.gameScene.ship.ShipManager.ShipKinds;
import src.gameScene.ship.ShipManager.ShipDefinition;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class Enemy extends Ship {
    private static final String component = "[Ship:Enemy]";

    private Vector2 stayPos; // 待機位置
    private double elapsedTime; // 出現してからの経過時間
    private int breakAwayTime = 20; // 離脱する時間(20秒後に離脱)

    public Enemy(String id, int level, Vector2 spawnPos, Vector2 stayPos) {
        super(ShipKinds.ENEMY, id, level, spawnPos);

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
        for (String object : animationId.keySet()) {
            if (animationId.get(object) == -1) {
                switch (object) {
                    case "ship" -> {
                        g2.rotate(rotate, pos.getX(), pos.getY());
                        g2.setColor(Color.RED);
                        g.fillPolygon(
                                new int[] {
                                        (int) pos.getX(),
                                        (int) pos.getX() - imageSize.get(object) / 2,
                                        (int) pos.getX() + imageSize.get(object) / 2
                                },
                                new int[] {
                                        (int) pos.getY() - imageSize.get(object) / 2,
                                        (int) pos.getY() + imageSize.get(object) / 2,
                                        (int) pos.getY() + imageSize.get(object) / 2
                                },
                                3);
                        // 回転リセット
                        g2.rotate(-rotate, pos.getX(), pos.getY());
                    }
                }
            }
        }

        switch (animationName) {
            case "death" -> {
                clear();
            }
        }
    }

    @Override
    public void move(double dt) {
        elapsedTime += dt;

        switch (animationName) {
            case "admission" -> {
                // 待機位置まで移動
                if (Math.pow(5 * ShootingGameMain.getScreenSizeAdj(), 2) < GameUtils.distSq(pos, stayPos))
                    super.move(dt);
                else {
                    pos = stayPos;
                    animation("none");
                }
            }
            case "none" -> {
                // 時間になったら離脱する
                if (elapsedTime > breakAwayTime) {
                    vec = Math.PI / 2;
                    animation("breakAway");
                }
            }
            case "breakAway" -> {
                super.move(dt);

                if (pos.getY() > MAIN.getHeight() * 1.2)
                    clear();
            }
        }
    }

    @Override
    public void drawHpBar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Vector2 barSize = new Vector2(
                35 * ShootingGameMain.getScreenSizeAdj(),
                4 * ShootingGameMain.getScreenSizeAdj());
        Vector2 barPos = new Vector2(pos.getX() - barSize.getX() / 2,
                pos.getY() - barSize.getY() / 2 + 25 * ShootingGameMain.getScreenSizeAdj());

        g2.setColor(new Color(50, 50, 50, 150));
        // HPバーの枠
        g2.fillRoundRect((int) barPos.getX(), (int) barPos.getY(),
                (int) (barSize.getX()), (int) (barSize.getY()),
                (int) barSize.getY(), (int) barSize.getY());

        // 最大HPに対する今のHPの割合を求めてその分を塗りつぶす
        double hpRathio = hp / maxHp;
        int currentHpWidth = (int) (barSize.getX() * hpRathio);
        g2.setColor(Color.RED);
        g2.fillRoundRect((int) barPos.getX(), (int) barPos.getY(),
                (int) (currentHpWidth), (int) (barSize.getY()),
                (int) barSize.getY(), (int) barSize.getY());

    }
}