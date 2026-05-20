package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Vector2;

public class Heal extends Item {
    public Heal(Vector2 pos) {
        super(ItemKinds.HEAL, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(Color.GREEN);
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // HPを20%回復
        SHIP_M.getPlayer().heal(SHIP_M.getPlayer().getMaxHp() * 0.2);
    }
}