package src.gameScene.item;

import src.ResourceManager;
import src.ShootingGameMain;
import src.ResourceManager.ResourceKinds;
import src.utils.Vector2;
import src.utils.GameUtils;
import src.gameScene.GameManager;
import src.gameScene.ship.ShipManager;
import src.gameScene.item.ItemManager.ItemKinds;
import src.gameScene.item.ItemManager.ItemConfig;
import src.interfaces.Entity;
import src.interfaces.Moveable;
import src.interfaces.GameObject;
import src.interfaces.Collectible;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Optional;

import lombok.Getter;

/**
 * アイテムの親クラス
 */
public abstract class Item implements Moveable, GameObject, Entity {
    protected final ShootingGameMain MAIN;
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
    protected BufferedImage[] itemImages; // 画像
    protected int currentFrame = 0; // 表示画像フレーム
    protected double frameTimer = 0; // 表示画像フレーム計算タイマー
    protected double frameRate = 0; // 画像の切替速度

    public Item(Collectible kind, Vector2 pos) {
        MAIN = ShootingGameMain.getInstance();
        RM = ResourceManager.getInstance();
        GM = GameManager.getInstance();
        SHIP_M = ShipManager.getInstance();
        IM = ItemManager.getInstance();

        this.kind = kind;
        this.pos = pos;
        this.vec = Math.random() * 2 * Math.PI; // ランダムな方向に飛ぶ
        if (kind instanceof ItemKinds) {
            ItemConfig itemConfig = IM.getItemConfig().get(kind);
            this.speed = itemConfig.getSpeed() * ShootingGameMain.getScreenSizeAdj();
            this.collisionSize = itemConfig.getCollisionSize() * ShootingGameMain.getScreenSizeAdj();
            this.itemImages = Optional.ofNullable(IM.getItemImage())
                    .map(images -> images.get(kind))
                    .orElse(null);
            this.frameRate = itemConfig.getFrameRate();
        } else if (kind instanceof ResourceKinds) {
            this.itemImages = Optional.ofNullable(RM.getResourceImage())
                    .map(images -> images.get(kind).get("common"))
                    .orElse(null);
        }
    }

    @Override
    public void update(double dt) {
        move(dt);

        // 何フレーム目の画像を表示するかの計算
        if (itemImages != null) {
            frameTimer += dt;
            if (frameTimer >= frameRate) {
                frameTimer = 0;
                currentFrame = (currentFrame + 1) % itemImages.length;
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        // 画像が存在するときは読み込む
        if (itemImages != null) {
            g.drawImage(
                    itemImages[currentFrame],
                    (int) pos.getX() - itemImages[currentFrame].getWidth() / 2,
                    (int) pos.getY() - itemImages[currentFrame].getHeight() / 2,
                    itemImages[currentFrame].getWidth(), itemImages[currentFrame].getHeight(),
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