package src.gameScene.item.types;

import src.gameScene.item.Item;
import src.gameScene.item.ItemManager.ItemKinds;
import src.utils.Vector2;

import java.awt.Color;
import java.awt.Graphics;

/**
 * レベルアップアイテムを処理するクラス
 */
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
        if (itemImages == null) {
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