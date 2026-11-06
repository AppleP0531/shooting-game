package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Vector2;

public class DurableShield extends Item {
    private static final double DURABILITY_RATE = 0.5; // 耐久(最大HPに対する割合)

    public DurableShield(Vector2 pos) {
        super(ItemKinds.DURABLE_SHIELD, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(new Color(255, 205, 70));
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // 最大HPの50%ぶんの耐久シールドを張る(耐久が尽きるまで続く)
        SHIP_M.getPlayer().startDurableShield(SHIP_M.getPlayer().getMaxHp() * DURABILITY_RATE);
    }
}
