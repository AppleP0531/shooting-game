package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.utils.Vector2;

public class Magnet extends Item {
    private static final double BASE_TIME = 4.0; // 基本の持続時間(秒)

    public Magnet(Vector2 pos) {
        super(ItemKinds.MAGNET, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(Color.MAGENTA);
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // 一定時間、画面内のアイテムをすべて引き寄せる
        IM.setMagnetTimer(BASE_TIME + SkillService.getBonus("MAGNET_TIME"));
    }
}
