package com.game.shooting_game.gameScene;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.unit.UnitService;

/**
 * ダメージ計算の専用クラス
 * 
 * ダメージの増減はここに集める。弾・ビーム・接触の当たり判定の側では計算せず、ここのメソッドを呼ぶ。
 * 
 * 敵に与えるダメージ = 基礎ダメージ × 火力倍率 × 色の倍率
 * - 火力倍率: スキル(DAMAGE・暴走中はOOC_DAMAGE) × 機体のレベル・★
 * - 色の倍率: 同じ色の敵には減衰(ボスは色の概念がないので減衰しない)
 * 自機が受けるダメージ = 基礎ダメージ × (1 - 軽減率)
 * - 軽減率: モジュール(防御力 %)。実数値の防御力はそのあとに引く。最低でも3割は通る。
 * モジュールの攻撃力(%・実数値)もここで反映する。
 */
public final class DamageCalculator {
    private DamageCalculator() {
    }

    private static Player player() {
        return ShipManager.getInstance().getPlayer();
    }

    private static String shipId() {
        Player p = player();
        if (p != null && p.getShipId() != null)
            return p.getShipId();
        return DataManager.getUserData() == null ? null : DataManager.getUserData().getSelectedShip();
    }

    // ----- 自機が敵に与える -----
    /**
     * 弾の火力倍率(スキル + 機体のレベル・★)
     */
    public static double bulletAttackRate() {
        double rate = 1 + SkillService.getBonus("DAMAGE");
        // 暴走状態(Lv5)中は追加で上昇
        Player p = player();
        if (p != null && p.getShipLevel() >= 5)
            rate *= 1 + SkillService.getBonus("OOC_DAMAGE");
        return rate * UnitService.attackRate(shipId()) * (1 + ModuleService.stat(ModuleStat.ATK_PCT));
    }

    /**
     * ビームの火力倍率(機体のレベル・★。ビーム自体の強化は発射時に反映済み)
     */
    public static double beamAttackRate() {
        return UnitService.attackRate(shipId()) * (1 + ModuleService.stat(ModuleStat.ATK_PCT));
    }

    /**
     * 色による倍率(同じ色には減衰、ボスは色の概念がないので等倍)
     */
    public static double colorRate(ColorMode attackMode, Ship target) {
        if (!target.isColorless() && attackMode == target.getMode())
            return GameManager.getInstance().sameColorDamageRate();
        return 1.0;
    }

    /**
     * 自機の弾が敵に与えるダメージ
     */
    public static double bulletToEnemy(Bullet bullet, Ship target) {
        // 実数値の攻撃力(モジュール)は倍率のあとに足す
        return (bullet.getDamage() * bulletAttackRate() + ModuleService.stat(ModuleStat.ATK_FLAT))
                * colorRate(bullet.getMode(), target) / StageModifiers.enemyHpRate(); // 敵が硬い条件はダメージを割って表す
    }

    /**
     * 自機のビームが敵に与えるダメージ(判定1回分)
     * 
     * @param dps         ビームの1秒あたりのダメージ(倍率込み)
     * @param tick        判定の間隔(秒)
     * @param mode        ビームの色
     * @param bossRate    ボスへの倍率
     * @param target      当たった敵
     */
    public static double beamToEnemy(double dps, double tick, ColorMode mode, double bossRate, Ship target) {
        double damage = dps * tick * beamAttackRate() * colorRate(mode, target) / StageModifiers.enemyHpRate();
        if (target.isColorless())
            damage *= bossRate;
        return damage;
    }

    // ----- 自機が受ける -----
    /**
     * 自機が受けるダメージの軽減率(0〜0.8)
     */
    public static double defenseRate() {
        return Math.min(0.8, ModuleService.stat(ModuleStat.DEF_PCT));
    }

    /**
     * 自機が受けるダメージ
     * 
     * @param raw 軽減前のダメージ
     */
    public static double toPlayer(double raw) {
        if (raw <= 0)
            return raw;
        double v = raw * (1 - Math.min(0.8, Math.max(0, defenseRate())));
        v *= StageModifiers.enemyDamageRate(); // 敵が強い条件(軽減のあとにかける)
        v -= ModuleService.stat(ModuleStat.DEF_FLAT); // 実数値の防御力(モジュール)は割合の軽減のあとに引く
        return Math.max(raw * 0.3, v); // どれだけ固めても3割は通る
    }

    /**
     * 自機が接触で受けるダメージ(最大HPの割合)
     */
    public static double ramToPlayer(Player p) {
        return toPlayer(p.getMaxHp() * 0.25);
    }
}
