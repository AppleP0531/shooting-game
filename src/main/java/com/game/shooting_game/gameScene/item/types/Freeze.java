package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Vector2;

public class Freeze extends Item {
    private static final double TIME = 4.0; // 敵が凍る時間(秒)

    public Freeze(Vector2 pos) {
        super(ItemKinds.FREEZE, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(new Color(140, 230, 255));
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // 一定時間、敵(ボス以外)の動きと弾を止める。ボスは動きと弾の間隔が半分になる
        SHIP_M.freezeEnemies(TIME);
    }
}
