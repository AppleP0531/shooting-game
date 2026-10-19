package com.game.shooting_game.module;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1個のモジュール(メイン能力1つ + サブ能力4つ)
 * メインは種類とレア度で決まる固定の割合。サブは制作時にランダムに決まり、メインより低い値になる。
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
    private List<Sub> subs = new ArrayList<>();
}
