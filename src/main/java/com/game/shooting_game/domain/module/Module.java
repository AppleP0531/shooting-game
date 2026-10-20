package com.game.shooting_game.domain.module;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1個のモジュール(メイン能力1つ + レベルで増えるサブ能力・最大4つ)
 * メインは種類とレア度で決まる固定の割合。サブはレベルが5・10・15・20に上がったときに1つずつ
 * ランダムに加わり、メインより低い値になる。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Module {
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sub {
        private ModuleStat stat;
        private double value;

        public Sub(ModuleStat stat, double value) {
            this.stat = stat;
            this.value = value;
        }
    }

    private long id;
    private ModuleKind kind;
    private Rarity rarity;
    private double mainValue; // メイン能力の値(割合)
    private int level = 1; // モジュールのレベル(レベルが上がるとサブ能力が増える)
    private List<Sub> subs = new ArrayList<>(); // 解放済みのサブ能力(Lv5,10,15,20で1つずつ増える)
}
