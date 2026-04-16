package src.gameScene.bullet.types;

import src.gameScene.ship.ShipManager.ShipKinds;
import src.utils.Vector2;
import src.gameScene.bullet.Bullet;
import src.gameScene.bullet.BulletManager.BulletKinds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class Ball extends Bullet {
    public Ball(ShipKinds owner, int id, Vector2 pos, double vec) {
        super(owner, BulletKinds.BALL, id, pos, vec);
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

            int s = 16;
            g2.fillOval((int) pos.getX() - s / 2, (int) pos.getY() - s / 2, s, s);
        }
    }
}