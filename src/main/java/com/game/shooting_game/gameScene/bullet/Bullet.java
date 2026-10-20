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
import com.game.shooting_game.ui.Anchor;
import com.game.shooting_game.ui.UIBuilder;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;

public abstract class Bullet implements Moveable, GameObject, Entity {
    public static enum Direction {
        RIGHT,
        LEFT;

        public int getSign() {
            return this == RIGHT ? 1 : -1;
        }
    }

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
    protected double imageSize; // 画像の大きさ
    @Getter
    protected double collisionSize = 0; // 当たり判定の大きさ
    protected double vec; // 向いてる方向
    protected double damage = 0; // 攻撃力(設定値)
    protected double damageScale = 1.0; // 攻撃力の倍率(サブ機など)
    protected double lifeTime = 10; // 存在可能時間
    protected double lifeTimer; // 存在時間
    protected float alpha; // 透明度
    @Getter
    protected ShipKinds owner; // 弾を発射者
    @Getter
    protected BulletKinds kind; // 弾の種類
    protected String animationId; // アニメーションId
    protected final BulletConfig config; // この弾の設定
    protected double maxSpeed; // 加速する弾の最高速度
    protected double accel; // 加速度(画面倍率込み)
    private int pierceLeft; // 残りの貫通数
    private final java.util.Set<Object> hitTargets = java.util.Collections
            .newSetFromMap(new java.util.IdentityHashMap<>()); // 貫通弾が当たった相手(同じ相手に2度当たらない)

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
        this.imageSize = bulletConfig.getImageSize() * ShootingGameMain.getScreenSizeAdj();
        this.collisionSize = bulletConfig.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();
        this.speed = bulletConfig.getSpeed() * ShootingGameMain.getScreenSizeAdj();
        this.damage = bulletConfig.getDamage();
        this.config = bulletConfig;
        this.maxSpeed = this.speed;
        this.accel = bulletConfig.getAccel() * ShootingGameMain.getScreenSizeAdj();
        this.pierceLeft = bulletConfig.getPierce();
        // 加速する弾は初速から始める
        if (bulletConfig.getStartSpeed() > 0)
            this.speed = bulletConfig.getStartSpeed() * ShootingGameMain.getScreenSizeAdj();

        // 画像の取得とアニメーション登録
        String bulletId = id + "_" + mode.toString().toLowerCase();
        BufferedImage[] bulletImages = Optional.ofNullable(BM.getBulletImage())
                .map(images -> images.get(owner))
                .map(o -> o.get(kind))
                .map(k -> k.get(bulletId))
                .orElse(null);
        if (bulletImages != null) {
            double frameRate = BM.getBulletConfig().get(owner).get(kind).get(id).getFrameRate();
            this.animationId = AM.addAnimation(new Animation(bulletImages, imageSize, frameRate));
        }
    }

    /**
     * 速度の倍率変更(弾幕の模様作り用)
     * 
     * @param scale 倍率
     */
    public void scaleSpeed(double scale) {
        speed *= scale;
    }

    @Override
    public void move(double dt) {
        pos.setX(pos.getX() + Math.cos(vec) * speed * dt);
        pos.setY(pos.getY() + Math.sin(vec) * speed * dt);
    }

    @Override
    public void update(double dt) {
        // 加速(最高速度まで)
        if (accel != 0)
            speed = Math.max(0, Math.min(maxSpeed, speed + accel * dt));

        move(dt);
        lifeTimer += dt;
    }

    /**
     * 敵に当たったことを記録する(貫通弾は同じ相手に2度ダメージを与えない)
     * 
     * @param target 当たった相手
     * @return 初めて当たったならtrue(ダメージを与えてよい)
     */
    public boolean registerHit(Object target) {
        return hitTargets.add(target);
    }

    /**
     * 当たった後に消えるか(貫通数が残っていれば通り抜けて、残りを1減らす)
     * 
     * @return 消えるならtrue
     */
    public boolean consumeHit() {
        if (pierceLeft > 0) {
            pierceLeft--;
            return false;
        }
        return true;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform oldTransform = g2.getTransform();

        UIBuilder.applyHighQualityRenderSettings(g2);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Vector2 drawPos = new Vector2(pos.getX(), pos.getY());
        // 画像が存在するときは読み込む(削除直後でアニメーションが解放済みの場合は描画しない)
        BufferedImage image = animationId == null ? null : AM.getCurrentImage(animationId);
        if (image != null) {
            // 回転処理
            g2.translate((int) drawPos.getX(), (int) drawPos.getY());
            g2.rotate(vec + Math.PI / 2);
            // 敵の弾は、自機の弾やビームに紛れないよう、うしろに暗い縁(ふち)を敷く
            if (owner != ShipKinds.PLAYER) {
                int r = Math.max(image.getWidth(), image.getHeight()) / 2 + 2;
                g2.setColor(new java.awt.Color(0, 0, 0, 120));
                g2.fillOval(-r, -r, r * 2, r * 2);
            }
            // 描画処理
            UIBuilder.drawImage(g2, Anchor.CENTER, new Vector2(0, 0), image);
        }
        g2.setTransform(oldTransform);
        // 透明度を1に戻す
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * 攻撃力(倍率込み)
     * 
     * @return 攻撃力
     */
    public double getDamage() {
        return damage * damageScale;
    }

    /**
     * 攻撃力の倍率の設定
     * 
     * @param scale 倍率
     */
    public void setDamageScale(double scale) {
        this.damageScale = scale;
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
    /**
     * 貫通弾か(ブロックなどを通り抜けられる)
     */
    public boolean isPiercing() {
        return config.getPierce() > 0;
    }

    public void destroySelf() {
        BM.removeBullet(this);
    }
}