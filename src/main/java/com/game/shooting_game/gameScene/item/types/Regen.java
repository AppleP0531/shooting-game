package com.game.shooting_game.gameScene.item.types;

import java.awt.Color;
import java.awt.Graphics;

import com.game.shooting_game.gameScene.item.Item;
import com.game.shooting_game.gameScene.item.ItemManager.ItemKinds;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.utils.Vector2;

public class Regen extends Item {
    public Regen(Vector2 pos) {
        super(ItemKinds.REGEN, pos);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        // 画像が存在しなければ仮描画
        if (animationId == null) {
            g.setColor(new Color(120, 255, 160));
            g.fillOval((int) pos.getX() - (int) collisionSize / 2, (int) pos.getY() - (int) collisionSize / 2,
                    (int) collisionSize, (int) collisionSize);
        }
    }

    @Override
    public void effect() {
        if (StageModifiers.noHeal())
            return; // 回復なしのステージ条件
        // 8秒かけて、最大HPの15%を回復(回復量のボーナスは通常の回復と同じ)
        IM.startRegen(SHIP_M.getPlayer().getMaxHp() * 0.15
                * (1 + SkillService.getBonus("HEAL_BONUS") + ModuleService.stat(ModuleStat.HEAL_PCT)), 8.0);
    }
}
