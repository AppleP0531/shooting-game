package com.game.shooting_game.gameScene.status;

import java.util.EnumMap;
import java.util.Map;

/**
 * 船にかかっている特殊効果の一覧(効果ごとの残り時間)
 * 画面や船に依存しない計算だけを持つ(テストできる)。同じ効果をまた受けると、残り時間は長い方になる(重ねがけで時間を更新)。
 */
public final class StatusSet {
    private final Map<StatusEffect, Double> timers = new EnumMap<>(StatusEffect.class);
    private final Map<StatusEffect, Double> powers = new EnumMap<>(StatusEffect.class); // 効果ごとの強さの倍率(継続ダメージ・脆弱の増え幅)

    /**
     * 効果を受ける
     *
     * @param effect  効果
     * @param seconds 時間(秒)。0以下なら何もしない
     */
    public void apply(StatusEffect effect, double seconds) {
        apply(effect, seconds, 1.0);
    }

    /**
     * 効果を受ける(強さつき)
     * 
     * @param power 強さの倍率(継続ダメージ・脆弱の増え幅にかかる。重ねがけは強い方)
     */
    public void apply(StatusEffect effect, double seconds, double power) {
        if (effect == null || seconds <= 0)
            return;
        timers.merge(effect, seconds, Math::max);
        powers.merge(effect, Math.max(0, power), Math::max);
    }

    /**
     * 時間を進める
     *
     * @param dt 経過時間
     * @return この間に受ける継続ダメージ(最大HPに対する割合。効果が残っている時間ぶんだけ)
     */
    public double update(double dt) {
        double fraction = 0;
        for (var it = timers.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            StatusEffect key = e.getKey();
            double active = Math.min(dt, e.getValue());
            fraction += key.getTickRate() * powers.getOrDefault(key, 1.0) * active;
            double left = e.getValue() - dt;
            if (left <= 0) {
                it.remove();
                powers.remove(key);
            } else
                e.setValue(left);
        }
        return fraction;
    }

    public boolean isActive(StatusEffect effect) {
        return timers.containsKey(effect);
    }

    /** 残り時間(かかっていなければ0) */
    public double remaining(StatusEffect effect) {
        return timers.getOrDefault(effect, 0.0);
    }

    public boolean isEmpty() {
        return timers.isEmpty();
    }

    /** かかっている効果(重なった順ではなく、定義の順) */
    public java.util.Set<StatusEffect> active() {
        return java.util.Collections.unmodifiableSet(timers.keySet());
    }

    /** 動く速さの倍率(かかっている効果のうち、いちばん遅いもの。止まる効果があれば0) */
    public double speedRate() {
        double rate = 1.0;
        for (StatusEffect e : timers.keySet())
            rate = Math.min(rate, e.getSpeedRate());
        return rate;
    }

    /** 受けるダメージの倍率(脆弱の間は増える。かかっていなければ1) */
    public double damageTakenRate() {
        if (!timers.containsKey(StatusEffect.VULNERABLE))
            return 1.0;
        return 1.0 + (StatusEffect.VULNERABLE_RATE - 1.0) * powers.getOrDefault(StatusEffect.VULNERABLE, 1.0);
    }

    /** 回復できない状態か(毒など) */
    public boolean blocksHeal() {
        for (StatusEffect e : timers.keySet())
            if (e.isBlocksHeal())
                return true;
        return false;
    }

    public void clear() {
        timers.clear();
        powers.clear();
    }
}
