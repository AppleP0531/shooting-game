package com.game.shooting_game.domain.module;

/**
 * モジュールの能力の種類
 * 能力ごとに「割合(%)」か「実数値」かが決まっている。メインは割合、サブは割合か実数値のどちらか。
 */
public enum ModuleStat {
    ATK_PCT("攻撃力", true),
    ATK_FLAT("攻撃力", false),
    DEF_PCT("防御力", true),
    DEF_FLAT("防御力", false),
    SHIELD_PCT("シールド量", true),
    SHIELD_FLAT("シールド量", false),
    REPAIR_PCT("シールド回復速度", true),
    HP_PCT("体力", true),
    HP_FLAT("体力", false),
    HEAL_PCT("回復量", true),
    HEAL_FLAT("回復量", false);

    private final String label;
    private final boolean percent;

    ModuleStat(String label, boolean percent) {
        this.label = label;
        this.percent = percent;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPercent() {
        return percent;
    }

    /**
     * 値の表示(例: +12.5% / +8)
     */
    public String format(double value) {
        if (percent)
            return String.format("+%.1f%%", value * 100);
        return String.format("+%.1f", value);
    }
}
