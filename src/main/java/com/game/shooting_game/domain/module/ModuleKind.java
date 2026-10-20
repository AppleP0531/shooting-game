package com.game.shooting_game.domain.module;

import com.game.shooting_game.ResourceManager.ResourceKinds.ResourceName;

/**
 * モジュールの種類(メイン能力が決まる)
 * 
 * @see ModuleStat
 */
public enum ModuleKind {
    ATC("atc_mod", "攻撃モジュール", "攻", ModuleStat.ATK_PCT, 1.0, ResourceName.GEAR,
            "自機の弾とビームのダメージが増える"),
    DEF("def_mod", "防御モジュール", "防", ModuleStat.DEF_PCT, 0.6, ResourceName.SCREW,
            "受けるダメージが減る"),
    SHIELD("shield_mod", "シールドモジュール", "盾", ModuleStat.SHIELD_PCT, 2.5, ResourceName.ENERGY_CELL,
            "HPの前にダメージを受けるシールドが増える"),
    REPAIR("repair_mod", "リペアモジュール", "修", ModuleStat.REPAIR_PCT, 2.5, ResourceName.CIRCUIT,
            "シールドが回復する速さが上がる"),
    HP("hp_mod", "体力モジュール", "体", ModuleStat.HP_PCT, 1.5, ResourceName.COPPER_PLATE,
            "最大HPが増える"),
    HEAL("heal_mod", "ヒールモジュール", "癒", ModuleStat.HEAL_PCT, 2.0, ResourceName.LENS,
            "回復アイテムの回復量が増える"),
    CRIT("crit_mod", "会心モジュール", "会", ModuleStat.CRIT_RATE, 0.5, ResourceName.GEAR,
            "弾とビームが会心になる確率が上がる"),
    CRITDMG("critdmg_mod", "会心威力モジュール", "威", ModuleStat.CRIT_DMG, 1.5, ResourceName.SCREW,
            "会心が出たときのダメージ倍率が上がる");

    private final String id;
    private final String title;
    private final String shortName;
    private final ModuleStat mainStat;
    private final double mainScale; // メイン能力の強さの倍率(能力の種類ごとの釣り合い)
    private final ResourceName subMaterial; // レシピに使う素材(基板のほかに1種)
    private final String detail;

    ModuleKind(String id, String title, String shortName, ModuleStat mainStat, double mainScale,
            ResourceName subMaterial, String detail) {
        this.id = id;
        this.title = title;
        this.shortName = shortName;
        this.mainStat = mainStat;
        this.mainScale = mainScale;
        this.subMaterial = subMaterial;
        this.detail = detail;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getShortName() {
        return shortName;
    }

    public ModuleStat getMainStat() {
        return mainStat;
    }

    public double getMainScale() {
        return mainScale;
    }

    public ResourceName getSubMaterial() {
        return subMaterial;
    }

    public String getDetail() {
        return detail;
    }
}
