package com.game.shooting_game.gameScene.item.types;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;

public class Shield extends Item {
    public Shield(Vector2 pos) {
        super(ItemKinds.SHIELD, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(Color.CYAN);
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // 10秒の無敵時間を付与
        SHIP_M.getPlayer().setShieldTimer(10);
    }
}