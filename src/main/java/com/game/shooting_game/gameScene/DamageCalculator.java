package com.game.shooting_game.gameScene;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.gameScene.GameManager.ColorMode;
import com.game.shooting_game.gameScene.bullet.Bullet;
import com.game.shooting_game.gameScene.item.ItemManager;
import com.game.shooting_game.gameScene.ship.Ship;
import com.game.shooting_game.gameScene.ship.ShipManager;
import com.game.shooting_game.gameScene.ship.types.Player;
import com.game.shooting_game.gameScene.stage.StageModifiers;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.utils.GameUtils;

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
        return rate * UnitService.attackRate(shipId()) * (1 + ModuleService.stat(ModuleStat.ATK_PCT))
                * ItemManager.atkUpMultiplier();
    }

    /**
     * ビームの火力倍率(機体のレベル・★。ビーム自体の強化は発射時に反映済み)
     */
    public static double beamAttackRate() {
        return UnitService.attackRate(shipId()) * (1 + ModuleService.stat(ModuleStat.ATK_PCT))
                * ItemManager.atkUpMultiplier();
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
        double attack = GameUtils.attackDamage(bullet.getDamage(), bulletAttackRate(), ModuleService.stat(ModuleStat.ATK_FLAT));
        return GameUtils.damageToEnemy(attack, colorRate(bullet.getMode(), target));
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
        double damage = GameUtils.damageToEnemy(dps * tick * beamAttackRate(), colorRate(mode, target));
        if (target.isColorless())
            damage *= bossRate;
        return damage;
    }

    // ----- 会心 -----
    /** 会心率の基礎値 */
    public static final double BASE_CRIT_RATE = 0.05;
    /** 会心ダメージ(倍率)の基礎値 */
    public static final double BASE_CRIT_MULT = 1.5;

    /**
     * 会心率(基礎 + スキル + モジュール。0〜1)
     */
    public static double critRate() {
        return Math.clamp(BASE_CRIT_RATE + SkillService.getBonus("CRIT_RATE")
                + ModuleService.stat(ModuleStat.CRIT_RATE), 0.0, 1.0);
    }

    /**
     * 会心ダメージ(会心時にダメージにかかる倍率)
     */
    public static double critMult() {
        return BASE_CRIT_MULT + SkillService.getBonus("CRIT_DMG") + ModuleService.stat(ModuleStat.CRIT_DMG);
    }

    /**
     * 自機の攻撃を敵に当てる(会心を抽選し、当たればダメージを増やして会心表示にする)
     * ダメージに上限は設けない。大きさは火力の伸び(BalanceConfig.json)で調整する。
     */
    public static void hitEnemy(Ship target, double damage) {
        if (Math.random() < critRate())
            target.addCritDamage(damage * critMult());
        else
            target.addDamage(damage);
    }

    // ----- 自機が受ける -----
    /**
     * 自機が受けるダメージの軽減率(0〜0.8)
     */
    public static double defenseRate() {
        return Math.min(GameUtils.MAX_DAMAGE_REDUCTION, ModuleService.stat(ModuleStat.DEF_PCT));
    }

    /**
     * 自機が受けるダメージ
     * 
     * @param raw 軽減前のダメージ
     */
    public static double toPlayer(double raw) {
        if (raw <= 0)
            return raw;
        // 割合の軽減 → 敵が強い条件(軽減のあとにかける) → 実数値の防御力(モジュール)を引く。どれだけ固めても3割は通る
        return GameUtils.damageToPlayer(raw, defenseRate(), StageModifiers.enemyDamageRate(),
                ModuleService.stat(ModuleStat.DEF_FLAT));
    }

    /**
     * 自機が接触で受けるダメージ(最大HPの割合)
     */
    public static double ramToPlayer(Player p) {
        return toPlayer(p.getMaxHp() * 0.25);
    }
}
