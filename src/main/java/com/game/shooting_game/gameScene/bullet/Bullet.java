package com.game.shooting_game.gameScene.bullet;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;

import lombok.Getter;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletConfig;
import com.game.shooting_game.gameScene.bullet.BulletManager.BulletKinds;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.interfaces.Entity;
import com.game.shooting_game.interfaces.GameObject;
import com.game.shooting_game.interfaces.Moveable;
import com.game.shooting_game.utils.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;

public abstract class Bullet implements Moveable, GameObject, Entity {
    protected final ShootingGameMain MAIN;
    protected final AnimationManager AM;
    protected final GameManager GM;
    protected final BulletManager BM;
    protected final ShipManager SHIP_M;

    @Getter
    protected ColorMode mode; // 色のモード
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
    protected String animationId; // アニメーションId

    public Bullet(ShipKinds owner, BulletKinds kind, int id, ColorMode mode, Vector2 pos, double vec) {
        MAIN = ShootingGameMain.getInstance();
        AM = AnimationManager.getInstance();
        GM = GameManager.getInstance();
        BM = BulletManager.getInstance();
        SHIP_M = ShipManager.getInstance();

        this.owner = owner;
        this.pos = pos;
        this.vec = vec;
        this.kind = kind;
        this.mode = mode;
        BulletConfig bulletConfig = this.BM.getBulletConfig().get(owner).get(kind).get(id);
        this.collisionSize = bulletConfig.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();
        this.speed = bulletConfig.getSpeed() * ShootingGameMain.getScreenSizeAdj();
        this.damage = bulletConfig.getDamage();

        // 画像の取得とアニメーション登録
        String bulletId = id + "_" + mode.toString().toLowerCase();
        BufferedImage[] bulletImages = Optional.ofNullable(BM.getBulletImage())
                .map(images -> images.get(owner))
                .map(o -> o.get(kind))
                .map(k -> k.get(bulletId))
                .orElse(null);
        if (bulletImages != null) {
            double frameRate = BM.getBulletConfig().get(owner).get(kind).get(id).getFrameRate();
            this.animationId = AM.addAnimation(new Animation(bulletImages, frameRate));
        }
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
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform oldTransform = g2.getTransform();

        UIBuilder.applyHighQualityRenderSettings(g2);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Vector2 drawPos = new Vector2(pos.getX(), pos.getY());
        // 画像が存在するときは読み込む
        if (animationId != null) {
            BufferedImage image = AM.getCurrentImage(animationId);

            // 回転処理
            g2.translate((int) drawPos.getX(), (int) drawPos.getY());
            g2.rotate(vec + Math.PI / 2);
            // 描画処理
            g.drawImage(image,
                    -image.getWidth() / 2, -image.getHeight() / 2,
                    image.getWidth(), image.getHeight(),
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