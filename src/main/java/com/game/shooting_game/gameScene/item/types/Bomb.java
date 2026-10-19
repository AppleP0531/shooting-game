package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import java.util.ArrayList;
import java.util.List;

import com.game.shooting_game.gameScene.bullet.BulletManager;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager.ShipKinds;
import com.game.shooting_game.skill.SkillService;
import com.game.shooting_game.utils.Vector2;

public class Bomb extends Item {
    private static final double ENEMY_DAMAGE = 40; // 通常の敵へのダメージ
    private static final double BOSS_DAMAGE_RATE = 0.02; // ボスの最大HPに対する割合

    public Bomb(Vector2 pos) {
        super(ItemKinds.BOMB, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(Color.ORANGE);
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        double power = 1 + SkillService.getBonus("BOMB_POWER");

        // 敵弾を全消し
        BulletManager.getInstance().clearEnemyBullets();

        // 画面内の敵にダメージ(ボスは割合で控えめに)
        List<Ship> targets = new ArrayList<>();
        targets.addAll(SHIP_M.getEnemyList());
        targets.addAll(SHIP_M.getBossList());
        for (Ship s : targets) {
            if (!s.inScreen())
                continue;
            if (s.getOwner() == ShipKinds.BOSS)
                s.addDamage(s.getMaxHp() * BOSS_DAMAGE_RATE * power);
            else
                s.addDamage(ENEMY_DAMAGE * power);
        }
    }
}
