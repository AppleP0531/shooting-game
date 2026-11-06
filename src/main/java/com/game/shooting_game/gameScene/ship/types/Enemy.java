package com.game.shooting_game.gameScene.ship.types;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.List;

import com.game.shooting_game.ShootingGameMain;
import com.game.shooting_game.gameScene.beam.BeamEmitter;
import com.game.shooting_game.gameScene.beam.BeamManager;
import com.game.shooting_game.gameScene.beam.BeamSpec;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipDefinition;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.utils.ANSI;
import com.game.shooting_game.utils.GameUtils;
import com.game.shooting_game.utils.LogUtils;
import com.game.shooting_game.utils.Vector2;

public class Enemy extends Ship {
    private static final String component = "[Ship:Enemy]";

    private Vector2 stayPos; // 待機位置
    private double elapsedTime; // 出現してからの経過時間
    private int breakAwayTime = 20; // 離脱する時間(20秒後に離脱)
    private EntranceType entrance; // 入場軌道
    private Vector2 origin; // 入場開始位置
    private double entranceDuration; // 入場にかける時間
    private double entranceTimer; // 入場経過時間
    private double entranceDelay = 0; // 入場開始までの待ち時間
    private double waitTimer = 0; // 入場待ちの経過時間
    private final List<BeamEmitter> beamEmitters = new ArrayList<>(); // ビームの発射管理

    public Enemy(String id, ColorMode mode, int level, Vector2 spawnPos, Vector2 stayPos) {
        this(id, mode, level, spawnPos, stayPos, EntranceType.LINEAR);
    }

    public Enemy(String id, ColorMode mode, int level, Vector2 spawnPos, Vector2 stayPos, EntranceType entrance) {
        super(ShipKinds.ENEMY, id, mode, level, spawnPos);

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

        // ビームを撃つ敵ならその設定を読み込む
        if (def != null && def.getBeams() != null)
            for (BeamSpec spec : def.getBeams())
                beamEmitters.add(new BeamEmitter(spec));

        // 待機位置に向かうようにvecを設定
        this.stayPos = stayPos;
        vec = GameUtils.targetVec(spawnPos, stayPos);

        // 入場軌道(直線以外は距離と速度から入場時間を決める)
        this.entrance = entrance == null ? EntranceType.LINEAR : entrance;
        this.origin = new Vector2(spawnPos);
        double dist = Math.sqrt(GameUtils.distSq(spawnPos, stayPos));
        this.entranceDuration = Math.max(0.5, dist * this.entrance.lengthFactor() / Math.max(1, speed));

        // 入場アニメーション
        animation("admission");

        // 船タイプログ
        LogUtils.info(component,
                "Current shipId set to " + ANSI.YELLOW + shipId + "." + ANSI.RESET);
    }

    @Override
    public void update(double dt) {
        super.update(dt);

        if (canFire())
            addNewBullet(shipLevel, shipId, shipState);

        // ビームは待機位置に着いてから撃つ(予兆があるので、色を見てかわせる)
        if ("none".equals(animationName) && SHIP_M.getPlayer() != null) {
            for (BeamEmitter emitter : beamEmitters)
                if (emitter.update(dt, 1.0))
                    BeamManager.getInstance().fireHostile(
                            emitter.getSpec(), this, emitter.pickColor(SHIP_M.getPlayer().getMode()));
        }
    }

    /**
     * 弾を撃てる状態かどうか(サブクラスで制限する用)
     * 
     * @return 撃てる場合true
     */
    protected boolean canFire() {
        return waitTimer >= entranceDelay;
    }

    /**
     * 入場開始までの待ち時間の設定(同じウェーブで順番に登場させる用)
     * 
     * @param delay 待ち時間(秒)
     */
    public void setEntranceDelay(double delay) {
        this.entranceDelay = Math.max(0, delay);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId.get("ship") == null) {
            g2.rotate(rotate, pos.getX(), pos.getY());

            if (mode == ColorMode.RED)
                g2.setColor(Color.RED);
            else
                g2.setColor(Color.BLUE);

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
                // 入場待ち(離脱までの時間には数えない)
                if (waitTimer < entranceDelay) {
                    waitTimer += dt;
                    elapsedTime -= dt;
                    return;
                }

                if (entrance == EntranceType.LINEAR) {
                    // 待機位置まで直線移動
                    if (Math.pow(5 * ShootingGameMain.getScreenSizeAdj(), 2) < GameUtils.distSq(pos, stayPos))
                        super.move(dt);
                    else {
                        pos = stayPos;
                        animation("none");
                    }
                } else {
                    // 入場軌道に沿って待機位置まで移動
                    entranceTimer += dt;
                    double t = entranceTimer / entranceDuration;
                    if (t >= 1) {
                        pos = stayPos;
                        animation("none");
                    } else {
                        Vector2 p = entrance.evaluate(origin, stayPos, t);
                        pos.setX(p.getX());
                        pos.setY(p.getY());
                    }
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

                // 更新中にリストを変更しないよう除去要求にする
                if (pos.getY() > MAIN.getHeight() * 1.2)
                    requestRemoval();
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