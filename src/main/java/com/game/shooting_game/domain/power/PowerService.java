package com.game.shooting_game.domain.power;

import com.game.shooting_game.data.DataManager;
import com.game.shooting_game.domain.module.ModuleService;
import com.game.shooting_game.domain.module.ModuleStat;
import com.game.shooting_game.domain.skill.SkillService;
import com.game.shooting_game.domain.unit.UnitService;
import com.game.shooting_game.gameScene.stage.Difficulty;
import com.game.shooting_game.utils.GameBalance;

/**
 * 戦力値(自機の強さを1つの数にしたもの)と、ステージの推定必要戦力値
 * <p>
 * 自分の戦力値 = 100 × 火力の倍率 × √(耐久 ÷ 100)。火力は弾の火力倍率(機体のレベル・ランク・スキル・モジュール)、
 * 耐久は最大HPとシールドの合計。
 * 推定必要戦力値 = 基準 × ステージ番号の敵HP倍率 × (難易度の敵HP倍率 ÷ ノーマルの値) × √(ステージ番号のダメージ倍率 × 難易度のダメージ倍率 ÷ ノーマルの値)。
 * 難易度はノーマルを1として比べる。基準は BalanceConfig.json の requiredBase(ステージ1・ノーマルでの必要戦力値)。
 */
public final class PowerService {
    /** 足りている目安(自分の戦力値が必要戦力値のこの割合以上なら「ぎりぎり」) */
    public static final double BARELY_RATE = 0.8;

    /** 足りているかの判定 */
    public enum Verdict {
        ENOUGH, // 足りる
        BARELY, // ぎりぎり(80%以上)
        SHORT // 不足
    }

    private PowerService() {
    }

    // ----- 純粋な計算 -----
    /** 戦力値(火力の倍率と耐久から) */
    public static double power(double attackRate, double durability) {
        return 100.0 * Math.max(0, attackRate) * Math.sqrt(Math.max(0, durability) / 100.0);
    }

    /**
     * 推定必要戦力値
     *
     * @param stageIndex ステージの番号(エクストラは、解放に要るステージ番号)
     * @param diff       難易度(nullならノーマル)
     */
    public static double required(int stageIndex, Difficulty diff) {
        Difficulty d = diff == null ? Difficulty.NORMAL : diff;
        Difficulty n = Difficulty.NORMAL;
        double hp = GameBalance.stageHpRate(stageIndex) * d.getEnemyHpRate() / n.getEnemyHpRate();
        double dmg = GameBalance.stageDamageRate(stageIndex) * d.getEnemyDamageRate() / n.getEnemyDamageRate();
        return GameBalance.requiredBase() * hp * Math.sqrt(dmg);
    }

    /** 自分の戦力値が必要戦力値に足りているか */
    public static Verdict verdict(double own, double required) {
        if (own >= required)
            return Verdict.ENOUGH;
        return own >= required * BARELY_RATE ? Verdict.BARELY : Verdict.SHORT;
    }

    // ----- 機体から読む -----
    /** 耐久(最大HPとシールドの合計。戦闘に入る前の見積もり。ステージ条件は含めない) */
    public static double durability(String shipId) {
        double hp = UnitService.baseHp(shipId) * (1 + SkillService.getBonusFor(shipId, "MAX_HP"))
                * UnitService.hpRate(shipId);
        hp = hp * (1 + ModuleService.stat(ModuleStat.HP_PCT)) + ModuleService.stat(ModuleStat.HP_FLAT);
        double shield = hp * UnitService.baseShieldRate(shipId) * (1 + ModuleService.stat(ModuleStat.SHIELD_PCT))
                + ModuleService.stat(ModuleStat.SHIELD_FLAT);
        return hp + Math.max(0, shield);
    }

    /** 火力の倍率(機体のレベル・ランク・その機体のスキル・モジュール) */
    public static double attackRate(String shipId) {
        return (1 + SkillService.getBonusFor(shipId, "DAMAGE")) * UnitService.attackRate(shipId)
                * (1 + ModuleService.stat(ModuleStat.ATK_PCT));
    }

    /** 機体の戦力値(その機体のスキル・いまのモジュールで計算) */
    public static double powerOf(String shipId) {
        return power(attackRate(shipId), durability(shipId));
    }

    /** 選んでいる機体の戦力値(データがなければ0) */
    public static double currentPower() {
        var ud = DataManager.getUserData();
        if (ud == null || ud.getSelectedShip() == null)
            return 0;
        return powerOf(ud.getSelectedShip());
    }
}
