package com.game.shooting_game.gameScene.ship.types;

import java.util.concurrent.ThreadLocalRandom;

import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.ship.EntranceType;
import com.game.shooting_game.gameScene.ship.path.MovePath;
import com.game.shooting_game.gameScene.ship.path.PathLibrary;
import com.game.shooting_game.utils.Vector2;

/**
 * 小敵
 * 画面外から現れて決められた経路を通り、画面外へ消える(その間に弾を撃つこともある)
 * 見た目・弾・HPなどはShipConfigのEnemy系の定義をそのまま使う
 */
public class SmallEnemy extends Enemy {
    private final MovePath path; // 通る経路
    private final double delay; // 動き出すまでの時間
    private double timer; // 出現してからの経過時間
    private double distance; // 経路上の移動距離

    /**
     * @param id         船ID
     * @param mode       色
     * @param level      レベル
     * @param pathId     経路ID(PathConfig.json)
     * @param mirror     経路を左右反転するか
     * @param delay      動き出すまでの時間(縦列で出すときのずらし用)
     * @param speedScale 速度倍率
     */
    public SmallEnemy(String id, ColorMode mode, int level, String pathId, boolean mirror, double delay,
            double speedScale) {
        super(id, mode, level,
                PathLibrary.startPos(pathId, mirror),
                PathLibrary.startPos(pathId, mirror),
                EntranceType.LINEAR);

        this.path = PathLibrary.create(pathId, mirror);
        this.delay = Math.max(0, delay);
        this.speed *= speedScale > 0 ? speedScale : 1;

        // 通過中に撃てるよう初弾までの時間を短くする
        coolDownTimers.values().forEach(
                idMap -> idMap.replaceAll((bulletId, time) -> 0.4 + ThreadLocalRandom.current().nextDouble(0.8)));

        animation("path");
    }

    @Override
    public void move(double dt) {
        timer += dt;
        // 出番まで画面外で待機
        if (timer < delay)
            return;

        distance += speed * dt;
        // 経路の終わりまで来たら除去(ドロップなし)
        if (distance >= path.getLength()) {
            requestRemoval();
            return;
        }

        Vector2 p = path.positionAt(distance);
        pos.setX(p.getX());
        pos.setY(p.getY());

        // 進行方向を向く(画像は上向きが基準)
        vec = path.headingAt(distance);
        rotate = vec + Math.PI / 2;
    }

    @Override
    protected boolean canFire() {
        // 画面内にいる間だけ撃つ
        return timer >= delay && inScreen();
    }
}
