package src.gameScene.bullet;

import src.ShootingGameMain;
import src.gameScene.GameManager;
import src.gameScene.ship.ShipManager;
import src.gameScene.ship.ShipManager.ShipKinds;
import src.gameScene.bullet.BulletManager.BulletConfig;
import src.gameScene.bullet.BulletManager.BulletKinds;
import src.interfaces.Entity;
import src.interfaces.GameObject;
import src.interfaces.Moveable;
import src.utils.Vector2;
import src.utils.UIBuilder;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;
import lombok.Getter;

public abstract class Bullet implements Moveable, GameObject, Entity {
    protected final ShootingGameMain MAIN;
    protected final GameManager GM;
    protected final BulletManager BM;
    protected final ShipManager SHIP_M;

    @Getter
    protected Vector2 pos; // 位置
    protected double speed = 0; // 速度
    @Getter
    protected double collisionSize = 0; // 当たり判定の大きさ
    protected double vec; // 向いてる方向
    @Getter
    protected double damage = 0; // 攻撃力
    protected double lifeTime = 10; // 存在可能時間
    protected double lifeTimer; // 存在時間
    protected float alpha; // 透明度
    @Getter
    protected ShipKinds owner; // 弾を発射者
    protected BulletKinds kind; // 弾の種類
    protected BufferedImage[] bulletImages; // 画像
    protected int currentFrame = 0; // 表示画像フレーム
    protected double frameTimer = 0; // 表示画像フレーム計算タイマー
    protected double frameRate = 0; // 画像の切替速度

    public Bullet(ShipKinds owner, BulletKinds kind, int id, Vector2 pos, double vec) {
        MAIN = ShootingGameMain.getInstance();
        GM = GameManager.getInstance();
        BM = BulletManager.getInstance();
        SHIP_M = ShipManager.getInstance();

        this.owner = owner;
        this.pos = pos;
        this.vec = vec;
        this.kind = kind;
        BulletConfig bulletConfig = this.BM.getBulletConfig().get(owner).get(kind).get(id);
        this.collisionSize = bulletConfig.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();
        this.speed = bulletConfig.getSpeed() * ShootingGameMain.getScreenSizeAdj();
        this.damage = bulletConfig.getDamage();
        this.bulletImages = Optional.ofNullable(BM.getBulletImage())
                .map(images -> images.get(owner).get(kind).get(id))
                .orElse(null);
        this.frameRate = BM.getBulletConfig().get(owner).get(kind).get(id).getFrameRate();
    }

    @Override
    public void move(double dt) {
        pos.setX(pos.getX() + Math.cos(vec) * speed * dt);
        pos.setY(pos.getY() + Math.sin(vec) * speed * dt);
    }

    @Override
    public void update(double dt) {
        move(dt);
        lifeTimer += dt;

        // 何フレーム目の画像を表示するかの計算
        if (bulletImages != null) {
            frameTimer += dt;
            if (frameTimer >= frameRate) {
                frameTimer = 0;
                currentFrame = (currentFrame + 1) % bulletImages.length;
            }
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform oldTransform = g2.getTransform();

        UIBuilder.applyHighQualityRenderSettings(g2);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Vector2 drawPos = new Vector2(pos.getX(), pos.getY());
        // 画像が存在するときは読み込む
        if (bulletImages != null) {
            // 回転処理
            g2.translate((int) drawPos.getX(), (int) drawPos.getY());
            g2.rotate(vec + Math.PI / 2);
            // 描画処理
            g.drawImage(bulletImages[currentFrame],
                    -bulletImages[currentFrame].getWidth() / 2, -bulletImages[currentFrame].getHeight() / 2,
                    bulletImages[currentFrame].getWidth(), bulletImages[currentFrame].getHeight(),
                    MAIN);
        }
        g2.setTransform(oldTransform);
        // 透明度を1に戻す
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * 画面内にいるか
     * 
     * @return いればtrue、いなければfalse
     */
    public boolean inScreen() {

        // 画面の-50%から+150%の範囲にいるかをチェック
        double minX = -MAIN.getWidth() / 2.0;
        double minY = -MAIN.getHeight() / 2.0;
        double maxX = MAIN.getWidth() * 1.5;
        double maxY = MAIN.getHeight() * 1.5;

        return minX < pos.getX() && pos.getX() < maxX && minY < pos.getY() && pos.getY() < maxY;
    }

    /**
     * 生存しているか
     * 
     * @return 生きていればtrue、そうでなければfalse
     */
    public boolean isLife() {
        return lifeTimer <= lifeTime;
    }

    /**
     * 自身(弾)の削除
     */
    public void destroySelf() {
        BM.removeBullet(this);
    }
}