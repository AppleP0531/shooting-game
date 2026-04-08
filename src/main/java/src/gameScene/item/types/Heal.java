package src.gameScene.item.types;

import src.gameScene.item.Item;
import src.gameScene.item.ItemManager.ItemKinds;
import src.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;

/**
 * 回復アイテムを処理するクラス
 */
public class Heal extends Item {

    // コンストラクタ
    public Heal(Vector2 pos) {
        super(ItemKinds.HEAL, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (itemImages == null) {
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