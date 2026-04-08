package src.gameScene.bullet.types;

import src.gameScene.ship.ShipManager.ShipKinds;
import src.utils.Vector2;
import src.gameScene.bullet.Bullet;
import src.gameScene.bullet.BulletManager.BulletKinds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

/**
 * 直進弾を処理するクラス
 */
public class Straight extends Bullet {
    // コンストラクタ
    public Straight(ShipKinds owner, int id, Vector2 pos, double vec) {
        super(owner, BulletKinds.STRAIGHT, id, pos, vec);
    }

    @Override
    public void draw(Graphics g) {
        alpha = Math.min((float) lifeTimer * 4, 0.9f);
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (bulletImages == null) {
            Graphics2D g2 = (Graphics2D) g;
            // プレイヤーなら青、敵なら赤
            g2.setColor(owner == ShipKinds.PLAYER ? new Color(0, 1, 1, alpha) : new Color(1, 0, 0, alpha));

            int s = 15;
            g2.setStroke(new BasicStroke(4));
            g2.drawLine((int) pos.getX(), (int) pos.getY(),
                    (int) (pos.getX() + Math.cos(vec) * s),
                    (int) (pos.getY() + Math.sin(vec) * s));

            // 線の太さをリセット
            g2.setStroke(new BasicStroke(1));
        }
    }
}