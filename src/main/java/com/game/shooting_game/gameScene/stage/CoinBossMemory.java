package com.game.shooting_game.gameScene.stage;

import java.util.ArrayList;
import java.util.List;

import com.game.shooting_game.ResourceManager.ResourceKinds.Rarity;

/**
 * 無限モードのコインボスの「覚え」(1回の挑戦の間だけ)
 * コインボスは一定時間で撤収し、あとで再登場する。再登場したとき、削ったHPが満タンに戻らないよう、
 * 撤収のときの残りHPの割合を覚えておき、次の登場に引き継ぐ。倒したら、覚えを消して、特殊ドロップ(モジュール)を1つ溜める。
 * 溜めたモジュールは、挑戦の精算のときに受け取る。
 */
public final class CoinBossMemory {
    /** 特殊ドロップのモジュールが EPIC から LEGENDARY になるウェーブ */
    static final int LEGENDARY_WAVE = 50;

    private static double remaining = 1.0; // 残りHPの割合(0より大きく1まで。1なら満タン)
    private static final List<Rarity> specialDrops = new ArrayList<>();

    private CoinBossMemory() {
    }

    /** 新しい挑戦の始まり(覚えと、溜めた特殊ドロップを消す) */
    public static synchronized void reset() {
        remaining = 1.0;
        specialDrops.clear();
    }

    /** 再登場したときの、残りHPの割合(はじめては1) */
    public static synchronized double remainingRatio() {
        return remaining;
    }

    /** 撤収するときに、残りHPの割合を覚える(0以下は倒れたことになるので覚えず、1を超える分は1に収める) */
    public static synchronized void save(double ratio) {
        if (ratio > 0)
            remaining = Math.min(1.0, ratio);
    }

    /**
     * 倒したとき。覚えを消し、特殊ドロップを1つ溜める。
     *
     * @param wave 倒したウェーブ
     */
    public static synchronized void defeated(int wave) {
        remaining = 1.0;
        specialDrops.add(rarityFor(wave));
    }

    /** 特殊ドロップのモジュールのレア度(ウェーブが進むほど良い。ふだんのウェーブ報酬より少し上) */
    static Rarity rarityFor(int wave) {
        return wave >= LEGENDARY_WAVE ? Rarity.LEGENDARY : Rarity.EPIC;
    }

    /** 溜めた特殊ドロップを受け取る(受け取ると空になる) */
    public static synchronized List<Rarity> takeSpecialDrops() {
        List<Rarity> taken = new ArrayList<>(specialDrops);
        specialDrops.clear();
        return taken;
    }
}
