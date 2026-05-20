package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.utils.Vector2;

public class LevelUp extends Item {
    public LevelUp(Vector2 pos) {
        super(ItemKinds.LEVEL_UP, pos);

        // レベルアップアイテム取得時にキルカウントをリセット
        GM.setKillCountSinceLastLevelUp(0);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(Color.YELLOW);
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        // 船のレベルを1段階あげる
        SHIP_M.getPlayer().levelUp();
    }
}