package com.game.shooting_game.gameScene.ship.types;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.game.shooting_game.SceneManager.SceneType;
import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;
import com.game.shooting_game.utils.animation.Animation;

public class Player extends Ship implements KeyListener {
    private static final String component = "[Ship:Player]";

    // 操作
    private MouseAdapter mouseHandler; // マウス操作
    private Vector2 dragStartPos; // ドラッグ開始マウス位置
    private Vector2 dragStartPlayerPos; // ドラッグ開始時のプレイヤー位置
    private Vector2 dragMousePos; // ドラッグ中のマウス位置
    @Getter
    private boolean up = false, down = false, left = false, right = false; // キー入力
    private String lastHorizontal = ""; // 最後に押された左右
    private String lastVertical = ""; // 最後に押された上下
    private List<SubShip> subShip = new ArrayList<>();
    private double OOCTime = 10; // 暴走状態持続時間
    @Setter
    @Getter
    private double OOCTimer; // 暴走状態タイマー
    @Getter
    private double damagedTimer; // 被弾時の無敵タイマー
    private double breakawaySpeed; // 離脱速度

    public Player(String id, Vector2 pos) {
        super(ShipKinds.PLAYER, id, 0, pos);

        // 画像の読み込みとアニメーション登録
        ShipDefinition def = SHIP_M.getShipConfig().get(owner).get(shipId);
        BufferedImage[] shieldImages_blue = SHIP_M.getShipImage().get("shield_blue");
        if (shieldImages_blue != null) {
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("shield_blue", AM.addAnimation(new Animation(shieldImages_blue, frameRate)));
        }
        BufferedImage[] shieldImages_red = SHIP_M.getShipImage().get("shield_red");
        if (shieldImages_red != null) {
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("shield_red", AM.addAnimation(new Animation(shieldImages_red, frameRate)));
        }
        BufferedImage[] explosionImages = SHIP_M.getShipImage().get("explosion");
        if (explosionImages != null) {
            double frameRate = def.getDatas().getFrameRate();
            animationId.put("explosion", AM.addAnimation(new Animation(explosionImages, frameRate)));
        }

        if (def != null && def.getDatas() != null) {
            // 画像が存在すれば読み込む
            for (String object : animationId.keySet()) {
                if (animationId.get(object) != null) {
                    int sheetCount = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets().length;
                    useCount.put(object, new int[sheetCount]);
                    for (int i = 0; i < sheetCount; i++) {
                        useCount.get(
                                object)[i] = SHIP_M.getShipConfig().get(owner).get(shipId).getDatas().getSheets()[i]
                                        .getUseCount();
                    }

                    imageSize.put(object, (int) (def.getDatas().getImageSize() * ShootingGameMain.getScreenSizeAdj()));
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
            collisionSize = 80 * ShootingGameMain.getScreenSizeAdj();
            speed = 250 * ShootingGameMain.getScreenSizeAdj();
        }

        rotate = 0;

        mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPos = new Vector2(e.getX(), e.getY());
                dragStartPlayerPos = new Vector2(pos.getX(), pos.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPos = null;
                dragMousePos = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragMousePos = new Vector2(e.getX(), e.getY());
            }
        };
        mode = ColorMode.BLUE;

        // 船タイプログ
        LogUtils.systemLog(
                "[INFO]",
                component,
                "Current shipId set to " + ANSI.YELLOW + shipId + "." + ANSI.RESET);

        // サブ機の生成
        String subShipId = DataManager.getUserData().getSubShip();
        double shipDist = 40 * ShootingGameMain.getScreenSizeAdj();
        double yDiff = 5 * ShootingGameMain.getScreenSizeAdj();
        if (subShipId != null) {
            subShip.add(new SubShip(this, subShipId + "_L", new Vector2(pos.getX() - shipDist, pos.getY() + yDiff)));
            subShip.add(new SubShip(this, subShipId + "_R", new Vector2(pos.getX() + shipDist, pos.getY() + yDiff)));
        }
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        switch (animationName) {
            case "none" -> {
                // 弾
                addNewBullet(shipLevel, shipId, shipState);

                // 船がレベル5の時は暴走モードにする
                if (shipLevel >= 5) {
                    OOCTimer = Math.max(0, OOCTimer - dt);
                    shipState = "OOC";

                    if (OOCTimer <= 0)
                        shipLevel = 4;
                } else
                    shipState = "Normal";

                // 被弾無敵時間処理(タイマーが0になるまで減らす)
                damagedTimer = Math.max(0, damagedTimer - dt);
            }
        }

        subShip.forEach(s -> s.update(dt));
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        alpha = (damagedTimer > 0) ? GameUtils.calcFlashAlpha(damagedTimer, 4, 0.8) : 1;
        super.draw(g);

        // 画像が存在しなければ仮描画
        // 船
        if (animationId.get("ship") == null) {
            g2.rotate(rotate, pos.getX(), pos.getY());
            g2.setColor(new Color(0, 1, 1, alpha));
            g.fillPolygon(
                    new int[] {
                            (int) pos.getX(),
                            (int) pos.getX() - imageSize.get("ship") / 2,
                            (int) pos.getX() + imageSize.get("ship") / 2
                    },
                    new int[] {
                            (int) pos.getY() - imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2,
                            (int) pos.getY() + imageSize.get("ship") / 2
                    },
                    3);
            // 回転リセット
            g2.rotate(-rotate, pos.getX(), pos.getY());
        }

        // ロビー以外では描画
        if (SM.getSceneName() != SceneType.LOBBY) {
            // シールド画像の描画(存在するとき)
            if (animationId.get("shield_blue") != null && mode == ColorMode.BLUE) {
                BufferedImage image = AM.getCurrentImage(animationId.get("shield_blue"));

                g2.drawImage(
                        image,
                        (int) (pos.getX() - image.getWidth() / 2),
                        (int) (pos.getY() - image.getHeight() / 2),
                        (int) image.getWidth(), (int) image.getHeight(),
                        MAIN);
            }
            if (animationId.get("shield_red") != null && mode == ColorMode.RED) {
                BufferedImage image = AM.getCurrentImage(animationId.get("shield_red"));

                g2.drawImage(
                        image,
                        (int) (pos.getX() - image.getWidth() / 2),
                        (int) (pos.getY() - image.getHeight() / 2),
                        (int) image.getWidth(), (int) image.getHeight(),
                        MAIN);
            }
        }

        switch (animationName) {
            case "death" -> {
                GM.result();
            }
        }

        subShip.forEach(s -> s.draw(g));
    }

    @Override
    public void move(double dt) {
        switch (animationName) {
            case "admission" -> {
                animateTimer += dt;

                double flySpeed = 100 * ShootingGameMain.getScreenSizeAdj();
                if (pos.getY() >= MAIN.getHeight() * 6 / 7)
                    pos.setY(pos.getY() - flySpeed * dt);
            }
            case "none" -> {
                // キー移動処理
                Vector2 direction = new Vector2(0, 0);
                // 上下
                int y = (up && down) ? (lastVertical.equals("up") ? -1 : 1) : (up ? -1 : (down ? 1 : 0));
                direction.setY(y);
                // 左右
                int x = (left && right) ? (lastHorizontal.equals("left") ? -1 : 1) : (left ? -1 : (right ? 1 : 0));
                direction.setX(x);
                // キーが何かしら押されてるときに移動
                if (up || down || left || right) {
                    vec = GameUtils.targetVec(new Vector2(0, 0), direction);
                    super.move(dt);
                }

                // マウス移動処理
                if (dragStartPos != null && dragMousePos != null) {
                    Vector2 targetPos = new Vector2(
                            dragStartPlayerPos.getX() + dragMousePos.getX() - dragStartPos.getX(),
                            dragStartPlayerPos.getY() + dragMousePos.getY() - dragStartPos.getY());
                    double distSq = GameUtils.distSq(pos, targetPos);
                    // 目標地点との距離が近いときは止まる（ガタつき防止）
                    if (distSq > Math.pow(5 * ShootingGameMain.getScreenSizeAdj(), 2)) {
                        vec = GameUtils.targetVec(pos, targetPos);
                        super.move(dt);
                    }

                }

                // 画面内に収まるように移動
                pos.setX(Math.clamp(pos.getX(), imageSize.get("ship") / 2,
                        MAIN.getWidth() - imageSize.get("ship") / 2));
                pos.setY(Math.clamp(pos.getY(), imageSize.get("ship") / 2,
                        MAIN.getHeight() - imageSize.get("ship") / 2));
            }
            case "breakAway" -> {
                animateTimer += dt;
                // 1秒経過後に上に飛び始める
                if (animateTimer > 1) {
                    double acceleration = 500 * ShootingGameMain.getScreenSizeAdj();
                    breakawaySpeed += acceleration * dt;
                    pos.setY(pos.getY() - breakawaySpeed * dt);

                    if (pos.getY() < -50)
                        GM.result();
                }
            }
        }

        subShip.forEach(s -> s.move(dt));
    }

    @Override
    public void drawHpBar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Vector2 barSize = new Vector2(
                MAIN.getWidth() - 60 * ShootingGameMain.getScreenSizeAdj(),
                15 * ShootingGameMain.getScreenSizeAdj());
        int skewX = (int) (10 * ShootingGameMain.getScreenSizeAdj()); // 上辺のずらす量
        Vector2 spacing = new Vector2(
                5 * ShootingGameMain.getScreenSizeAdj(),
                25 * ShootingGameMain.getScreenSizeAdj()); // 画面端からのスペース
        Vector2 barPos = new Vector2(
                MAIN.getWidth() - barSize.getX() - skewX - spacing.getX(),
                spacing.getY());

        // HPバーの枠
        g2.setColor(new Color(50, 50, 50, 150));
        g.fillPolygon(
                new int[] {
                        (int) barPos.getX() + skewX,
                        (int) barPos.getX() + (int) barSize.getX() + skewX,
                        (int) barPos.getX() + (int) barSize.getX(),
                        (int) barPos.getX()
                },
                new int[] {
                        (int) barPos.getY(),
                        (int) barPos.getY(),
                        (int) barPos.getY() + (int) barSize.getY(),
                        (int) barPos.getY() + (int) barSize.getY()
                },
                4);

        // 最大HPに対する今のHPの割合を求めてその分を塗りつぶす
        double hpRathio = hp / maxHp;
        int currentHpWidth = (int) (barSize.getX() * hpRathio);
        g2.setColor(Color.GREEN);
        g.fillPolygon(
                new int[] {
                        (int) barPos.getX() + skewX,
                        (int) barPos.getX() + currentHpWidth + skewX,
                        (int) barPos.getX() + currentHpWidth,
                        (int) barPos.getX()
                },
                new int[] {
                        (int) barPos.getY(),
                        (int) barPos.getY(),
                        (int) barPos.getY() + (int) barSize.getY(),
                        (int) barPos.getY() + (int) barSize.getY()
                },
                4);
    }

    /*
     * キー関連
     * 論理値変数を押したときにtrue,離れた時にfalse
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                up = true;
                lastVertical = "up";
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                down = true;
                lastVertical = "down";
            }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                left = true;
                lastHorizontal = "left";
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                right = true;
                lastHorizontal = "right";
            }
            case KeyEvent.VK_SPACE -> {
                // モード切替
                if (mode == ColorMode.BLUE) {
                    mode = ColorMode.RED;
                } else {
                    mode = ColorMode.BLUE;
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                up = false;
                if (down)
                    lastVertical = "down";
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                down = false;
                if (up)
                    lastVertical = "up";
            }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                left = false;
                if (right)
                    lastHorizontal = "right";
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                right = false;
                if (left)
                    lastHorizontal = "left";
            }
        }
    }

    @Override
    public void addDamage(double damage) {
        // シールドを持っていなかった場合はダメージを負って、1秒のダメージクールタイムを設ける
        if (shieldTimer <= 0) {
            super.addDamage(damage);
            shieldTimer = damagedTimer = 1;
        }
    }

    /**
     * レベルアップ
     */
    public void levelUp() {
        // 常時暴走状態の場合処理をスキップ
        if (OOCTimer == Double.POSITIVE_INFINITY)
            return;

        if (shipLevel < 5)
            shipLevel++;

        if (shipLevel == 5)
            setOOCTimer(OOCTime);
    }

    /**
     * シールド時間の設定
     * 
     * @param time 時間
     */
    public void setShieldTimer(double time) {
        // 与えられた数字が0でないかつ、常時シールド状態の場合処理をスキップ
        if (shieldTimer == Double.POSITIVE_INFINITY && time != 0)
            return;

        shieldTimer = time;
    }

    /**
     * ハンドラの追加
     */
    public void addHandler() {
        MAIN.addMouseListener(mouseHandler);
        MAIN.addMouseMotionListener(mouseHandler);
    }

    /**
     * ハンドラの削除
     */
    public void removeHandler() {
        MAIN.removeMouseListener(mouseHandler);
        MAIN.removeMouseMotionListener(mouseHandler);
    }
}