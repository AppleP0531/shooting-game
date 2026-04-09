package src.gameScene.item.types;

import src.gameScene.item.Item;
import src.gameScene.item.ItemManager.ItemKinds;
import src.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;

/**
 * シールドアイテムを処理するクラス
 */
public class Shield extends Item {
    public Shield(Vector2 pos) {
        super(ItemKinds.SHIELD, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (itemImages == null) {
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