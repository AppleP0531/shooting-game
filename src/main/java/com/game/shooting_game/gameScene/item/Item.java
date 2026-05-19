package com.game.shooting_game.gameScene.item;

import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;
import com.game.shooting_game.utils.animation.AnimationManager;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.gameScene.GameManager;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.item.ItemManager.ItemConfig;
import com.game.shooting_game.interfaces.Entity;
import com.game.shooting_game.interfaces.Moveable;
import com.game.shooting_game.interfaces.GameObject;
import com.game.shooting_game.interfaces.Collectible;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Optional;

import com.game.shooting_game.ResourceManager;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.ResourceManager.ResourceKinds;

import lombok.Getter;

public abstract class Item implements Moveable, GameObject, Entity {
    protected final ShootingGameMain MAIN;
    protected final AnimationManager AM;
    protected final ResourceManager RM;
    protected final GameManager GM;
    protected final ShipManager SHIP_M;
    protected final ItemManager IM;

    private final double ATTRACT_RANGE = 200; // 引き寄せが始まる距離
    private final double ATTRACT_SPEED = 500; // 引き寄せスピード
    @Getter
    protected double collisionSize = 0; // 当たり判定の大きさ
    @Getter
    protected Collectible kind;
    @Getter
    protected Vector2 pos; // 位置
    protected double vec; // 方向
    protected double speed = 0; // 速度
    protected String animationId; // アニメーションId

    public Item(Collectible kind, Vector2 pos) {
        MAIN = ShootingGameMain.getInstance();
        AM = AnimationManager.getInstance();
        RM = ResourceManager.getInstance();
        GM = GameManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        IM = ItemManager.getInstance();

        this.kind = kind;
        this.pos = pos;
        this.vec = Math.random() * 2 * Math.PI; // ランダムな方向に飛ぶ
        BufferedImage[] itemImages = null;
        double frameRate = 0;
        if (kind instanceof ItemKinds) {
            ItemConfig itemConfig = IM.getItemConfig().get(kind);
            this.speed = itemConfig.getSpeed() * ShootingGameMain.getScreenSizeAdj();
            this.collisionSize = itemConfig.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();

            // 画像の取得
            itemImages = Optional.ofNullable(IM.getItemImage())
                    .map(images -> images.get(kind))
                    .orElse(null);
            frameRate = itemConfig.getFrameRate();
        } else if (kind instanceof ResourceKinds) {
            itemImages = Optional.ofNullable(RM.getResourceImage())
                    .map(images -> images.get(kind).get("common"))
                    .orElse(null);
        }

        // アニメーション登録
        if (itemImages != null) {
            animationId = AM.addAnimation(new Animation(itemImages, frameRate));
        }
    }

    @Override
    public void update(double dt) {
        move(dt);
    }

    @Override
    public void draw(Graphics g) {
        // 画像が存在するときは読み込む
        if (animationId != null) {
            BufferedImage image = AM.getCurrentImage(animationId);
            g.drawImage(
                    image,
                    (int) pos.getX() - image.getWidth() / 2,
                    (int) pos.getY() - image.getHeight() / 2,
                    image.getWidth(), image.getHeight(),
                    MAIN);
        }
    }

    @Override
    public void move(double dt) {
        // プレイヤーとの距離が近くなったらプレイヤーに近づく
        double distSq = GameUtils.distSq(SHIP_M.getPlayer().getPos(), pos);
        if (distSq < Math.pow(ATTRACT_RANGE, 2)) {
            vec = GameUtils.targetVec(this.getPos(), SHIP_M.getPlayer().getPos());
            speed = ATTRACT_SPEED;
        } else {
            ItemConfig itemConfig = IM.getItemConfig().get(kind);
            speed = itemConfig.getSpeed();
        }

        pos.setX(pos.getX() + Math.cos(vec) * speed * dt);
        pos.setY(pos.getY() + Math.sin(vec) * speed * dt);

        // 画面端で跳ね返る処理
        if (pos.getX() < collisionSize / 2 || MAIN.getWidth() - collisionSize / 2 < pos.getX()) {
            vec = Math.PI - vec;
            pos.setX(Math.clamp(pos.getX(), collisionSize / 2, MAIN.getWidth() - collisionSize / 2));
        }
        if (pos.getY() < collisionSize / 2 || MAIN.getHeight() - collisionSize / 2 < pos.getY()) {
            vec = -vec;
            pos.setY(Math.clamp(pos.getY(), collisionSize / 2, MAIN.getHeight() - collisionSize / 2));
        }
    }

    /**
     * 取得時の効果
     */
    public abstract void effect();

    /**
     * 自身(アイテム)の削除
     */
    public void destroySelf() {
        IM.removeItem(this);
    }
}