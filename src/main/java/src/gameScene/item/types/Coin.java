package src.gameScene.item.types;

import src.ShootingGameMain;
import src.ResourceManager.ResourceKinds;
import src.gameScene.item.Item;
import src.utils.GameUtils;
import src.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;

public class Coin extends Item {
    private Vector2 moveDir; // 移動方向
    private double floorY; // 床
    private double minX; // バウンド時のX最小値
    private double maxX; // バウンド時のX最大値
    private double elapsedTime; // 出現してからの経過時間
    private Vector2 targetPos;

    public Coin(Vector2 pos) {
        super(ResourceKinds.COIN, pos);
        if (0 < vec && vec < Math.PI)
            vec += Math.PI;

        speed = 250 * ShootingGameMain.getScreenSizeAdj();
        moveDir = new Vector2(Math.cos(vec) * speed / 3, Math.sin(vec) * speed);
        floorY = pos.getY() + Math.random() * 100 * ShootingGameMain.getScreenSizeAdj();
        minX = Math.max(0, pos.getX() - 100 * ShootingGameMain.getScreenSizeAdj());
        maxX = Math.min(MAIN.getWidth(), pos.getX() + 100 * ShootingGameMain.getScreenSizeAdj());
        targetPos = new Vector2(20, 20);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            int size = (int) (6 * ShootingGameMain.getScreenSizeAdj());

            g.setColor(Color.YELLOW);
            g.fillPolygon(new int[] {
                    (int) pos.getX() + size / 2,
                    (int) pos.getX(),
                    (int) pos.getX() - size / 2,
                    (int) pos.getX()
            }, new int[] {
                    (int) pos.getY(),
                    (int) pos.getY() + size,
                    (int) pos.getY(),
                    (int) pos.getY() - size
            }, 4);
        }
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        elapsedTime += dt;

        double dist = Math.pow(2 * ShootingGameMain.getScreenSizeAdj(), 5);
        if (GameUtils.distSq(pos, targetPos) <= dist
                || pos.getX() < targetPos.getX()
                || pos.getY() < targetPos.getY()) {
            effect();
            destroySelf();
        }
    }

    @Override
    public void move(double dt) {
        // ドロップしてから1秒間はドロップ処理をして、そのあとにtargetPosに向かう
        if (elapsedTime <= 1.5) {
            double gravity = 800 * ShootingGameMain.getScreenSizeAdj();

            pos.setX(pos.getX() + moveDir.getX() * dt);
            pos.setY(pos.getY() + moveDir.getY() * dt);

            moveDir.setY(moveDir.getY() + gravity * dt);

            if (pos.getY() > floorY) {
                pos.setY(floorY);
                moveDir.setY(moveDir.getY() * -0.6);

                if (moveDir.getY() > -50) {
                    moveDir = new Vector2(0, 0);
                }
            }

            pos.setX(Math.max(pos.getX(), minX));
            pos.setX(Math.min(pos.getX(), maxX));
        } else {
            speed = 500 * ShootingGameMain.getScreenSizeAdj();
            vec = GameUtils.targetVec(pos, targetPos);

            pos.setX(pos.getX() + Math.cos(vec) * speed * dt);
            pos.setY(pos.getY() + Math.sin(vec) * speed * dt);
        }
    }

    @Override
    public void effect() {
        GM.addCoin(100);
    }
}
